(ns cn.li.mcmod.ui.xml
  "XML layout loader — parses new-schema XML into node-spec data.
   Element name = node kind; attributes = static props; <template> = reusable.
   Single parse path, schema validation at load time."
  (:refer-clojure :exclude [parse-double])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [cn.li.mcmod.config :as modid]
            [cn.li.mcmod.ui.spec :as ui-spec])
  (:import [javax.xml.parsers SAXParserFactory SAXParser]
           [org.xml.sax ContentHandler Attributes InputSource]
           [org.xml.sax.helpers DefaultHandler]
           [java.io InputStream]
           [java.util ArrayDeque ArrayList]))

;; ============================================================================
;; Attribute parsers
;; ============================================================================

(defn- parse-int [s default]
  (if s
    (try (Long/parseLong (str/trim s)) (catch Exception _ default))
    default))

(defn- parse-double [s default]
  (if s
    (try (Double/parseDouble (str/trim s)) (catch Exception _ default))
    default))

(defn- parse-color [s]
  "Parse #RRGGBB or #RRGGBBAA hex color string into ARGB long.
   Returns nil if not a valid color."
  (when (and s (str/starts-with? (str/trim s) "#"))
    (try
      (let [hex (str/replace (str/trim s) #"^#" "")
            len (count hex)]
        (Long/parseLong (if (= len 6) (str "FF" hex) hex) 16))
      (catch Exception _ nil))))

(defn- parse-pos [s]
  "Parse 'x,y' into {:x :y}."
  (if s
    (let [parts (str/split (str/trim s) #"\s*,\s*")]
      {:x (parse-double (first parts) 0.0)
       :y (parse-double (second parts) 0.0)})
    {:x 0.0 :y 0.0}))

(defn- parse-size [s]
  "Parse 'w,h' into {:w :h}."
  (if s
    (let [parts (str/split (str/trim s) #"\s*,\s*")]
      {:w (parse-double (first parts) 0.0)
       :h (parse-double (second parts) 0.0)})
    {:w 0.0 :h 0.0}))

(defn- parse-align [s]
  "Parse 'center,center' into {:align-w :center :align-h :middle}.
   XML uses 'center' for both axes; framework uses :middle for vertical center."
  (if s
    (let [parts (str/split (str/trim s) #"\s*,\s*")
          w (keyword (or (first parts) "left"))
          h-raw (or (second parts) "top")
          h (keyword (if (= "center" h-raw) "middle" h-raw))]
      {:align-w w :align-h h})
    {:align-w :left :align-h :top}))

;; ============================================================================
;; Kind → spec conversion
;; ============================================================================

(def ^:private kind-attrs
  "Attribute mapping for each kind. Maps XML attr name → spec key + parse fn.
   Boolean predicate props use a legal (?-free) XML attr name and a
   map-returning parse fn to emit the ?-suffixed spec key the nodes read
   (XML attribute names cannot contain '?')."
  {:group {:pos parse-pos :size parse-size :scale (fn [s] (or (parse-double s nil) 1.0))
           :align parse-align :clip (fn [s] {:clip? (= "true" s)}) :z (fn [s] (parse-double s 0.0))}
   :box   {:pos parse-pos :size parse-size :fill parse-color :outline parse-color
           :outline-width (fn [s] (parse-double s 0.0)) :tint parse-color
           :hover-tint (fn [s] (parse-double s 0.0)) :z (fn [s] (parse-double s 0.0))}
   :text  {:pos parse-pos :size parse-size :text str :font-size (fn [s] (parse-double s 14.0))
           :color parse-color :font str :z (fn [s] (parse-double s 0.0))
           :text-align (fn [s] {:align (keyword s)})
           :editable (fn [s] {:editable? (= "true" s)})
           :masked (fn [s] {:masked? (= "true" s)})}
   :image {:pos parse-pos :size parse-size :src str :alpha (fn [s] (parse-double s 1.0))
           :scale (fn [s] (or (parse-double s nil) 1.0))
           :tint parse-color :z (fn [s] (parse-double s 0.0))}
   :progress {:pos parse-pos :size parse-size :z (fn [s] (parse-double s 0.0))
              :fg-src str :bg-src str :icon-src str}
   :list   {:pos parse-pos :size parse-size :spacing (fn [s] (parse-double s 4.0))
            :template str :z (fn [s] (parse-double s 0.0))}})

(defn- parse-elem-attrs [kind attrs]
  "Convert XML attributes to spec props based on kind's attr mapping."
  (let [mapping (get kind-attrs kind)]
    (reduce-kv (fn [m k v]
                 (if-let [parse-fn (get mapping (keyword k))]
                   (let [parsed (parse-fn v)]
                     (if (map? parsed)
                       (merge m parsed)
                       (assoc m (keyword k) parsed)))
                   m))
               {} attrs)))

;; ============================================================================
;; Templates (forward declare needed for circular parse-ui-element ref)
;; ============================================================================

(declare parse-ui-element)

(defn- collect-templates [children]
  "Extract <template> elements from children, returning {name spec} map."
  (reduce (fn [acc child]
            (if (= :template (:tag child))
              (assoc acc (get-in child [:attrs :name])
                     (parse-ui-element child {}))
              acc))
          {} children))

(defn- non-template-children [children]
  (remove #(= :template (:tag %)) children))

;; ============================================================================
;; Recursive parser
;; ============================================================================

(defn- parse-ui-children [children templates]
  (when (seq children)
    (mapv (fn [child]
            (if (= :template (:tag child))
              nil  ;; templates handled separately
              (let [spec (parse-ui-element child templates)]
                spec)))
          (non-template-children children))))

(defn- parse-ui-element [elem templates]
  "Parse a single XML element into node-spec {:kind :id :props :children}.
   :list elements with a template=\"name\" attr get the resolved template SPEC
   stored in props :template (consumed by ui.core/list-set! for instantiation)."
  (let [tag (:tag elem)
        attrs (:attrs elem)
        content (:content elem)
        kind (if (or (= :Ui tag) (= :template tag)) :group (keyword (name tag)))]
    (if (and (:template attrs) (not= kind :list))
      ;; Non-list element referencing a template: clone the template spec
      (if-let [tmpl (get templates (:template attrs))]
        (merge tmpl (when-let [id-val (:id attrs)]
                      {:id (keyword id-val)}))
        (throw (ex-info (str "Template not found: " (:template attrs)) {})))
      ;; Regular element (or :list — template resolved into props below)
      (let [props (parse-elem-attrs kind attrs)
            props (if-let [id-attr (:id attrs)]
                    (assoc props :id (keyword id-attr))
                    props)
            ;; :visible — common to every kind (like :id), so handled here
            ;; rather than per-kind. Legal XML name → ?-suffixed spec key.
            props (if-let [vis (:visible attrs)]
                    (assoc props :visible? (= "true" vis))
                    props)
            ;; :align — common to every kind; parse-elem-attrs maps it only for
            ;; :group, so image/box/progress/list would otherwise drop it (the
            ;; XML author wrote align="center,center" on <image> and it silently
            ;; had no effect). parse-align returns {:align-w :align-h}.
            props (if-let [align (:align attrs)]
                    (merge props (parse-align align))
                    props)
            ;; :list — replace template NAME string with the resolved template SPEC
            props (if (and (= kind :list) (:template attrs))
                    (if-let [tmpl (get templates (:template attrs))]
                      (assoc props :template tmpl)
                      (throw (ex-info (str "List template not found: " (:template attrs)) {})))
                    props)]
        {:kind kind
         :id (keyword (:id attrs (str (gensym "node-"))))
         :props props
         :children (vec (keep #(parse-ui-element % templates) (non-template-children content)))}))))

;; ============================================================================
;; Public API
;; ============================================================================

;; ============================================================================
;; SAX-based XML parser — replaces clojure.xml/parse which uses reflection
;; that breaks on Java 17+ with SAXParserImpl.
;; ============================================================================

(defn- parse-xml
  "Parse XML from InputStream using aot-compatible reify implementing ContentHandler.
   Returns the same {:tag :attrs {:key \"val\"} :content [...]} tree as clojure.xml/parse.
   Zero reflection, zero runtime code generation — safe under full AOT + Minecraft ClassLoaders."
  [^InputStream in]
  (let [^ArrayDeque stack (ArrayDeque.)
        root (atom nil)
        ^StringBuilder current-text (StringBuilder.)
        flush-text! (fn [parent]
                      (let [s (.toString current-text)]
                        (.setLength current-text 0)
                        (when-not (str/blank? s)
                          (.add ^ArrayList (:content parent) (.trim s)))))
        handler (reify ContentHandler
                  (startElement [_this _uri local-name q-name attrs]
                    (when-let [parent (.peek stack)]
                      (flush-text! parent))
                    (let [^Attributes a attrs
                          tag (keyword (or (not-empty q-name) local-name))
                          len (.getLength a)
                          attr-map (reduce (fn [m i]
                                             (assoc m (keyword (.getQName a (int i)))
                                                      (.getValue a (int i))))
                                           {} (range len))
                          elem {:tag tag :attrs attr-map :content (ArrayList.)}]
                      (when-let [parent (.peek stack)]
                        (.add ^ArrayList (:content parent) elem))
                      (.push stack elem)
                      (when (nil? @root) (reset! root elem))))
                  (endElement [_this _uri _local-name _q-name]
                    (when-let [current (.peek stack)]
                      (flush-text! current))
                    (.pop stack))
                  (characters [_this ch start length]
                    (.append ^StringBuilder current-text ^chars ch (int start) (int length)))
                  ;; Remaining ContentHandler methods — must be implemented, all no-ops
                  (setDocumentLocator [_this _locator])
                  (startDocument [_this])
                  (endDocument [_this])
                  (startPrefixMapping [_this _prefix _uri])
                  (endPrefixMapping [_this _prefix])
                  (ignorableWhitespace [_this _ch _start _length])
                  (processingInstruction [_this _target _data])
                  (skippedEntity [_this _name]))]
    (let [^SAXParserFactory factory (SAXParserFactory/newInstance)
          ^SAXParser parser (.newSAXParser factory)
          reader (.getXMLReader parser)]
      (.setContentHandler reader handler)
      (.parse reader (InputSource. in)))
    ;; Deep-convert mutable ArrayLists to immutable Clojure vectors
    (letfn [(to-clj [node]
              (if (string? node)
                node
                (update node :content (fn [^ArrayList l]
                                        (vec (map to-clj l))))))]
      (to-clj @root))))

(defn parse-spec [^String xml-str]
  "Parse XML string into node-spec map. Top-level <Ui> → spec tree."
  (let [root (parse-xml (java.io.ByteArrayInputStream. (.getBytes xml-str "UTF-8")))
        templates (collect-templates (:content root))]
    (parse-ui-element root templates)))

(defn load-spec
  "Load XML from classpath resource and parse into node-spec.
   Schema-validated at load time (never on the render path).
   resource-path: e.g. \"academy:guis/rework/page_inv.xml\"
   Falls back to assets/<modid>/ prefix when the bare path is not on classpath."
  [resource-path]
  (let [path (str/replace resource-path #"^[^:]+:" "")
        res (or (io/resource path)
                (io/resource (str "assets/" modid/mod-id "/" path)))]
    (when-not res
      (throw (ex-info (str "XML resource not found: " resource-path) {:path path})))
    (let [root (parse-xml (io/input-stream res))
          templates (collect-templates (:content root))
          spec (parse-ui-element root templates)]
      (ui-spec/validate! spec)
      spec)))
