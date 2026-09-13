(ns cn.li.ac.tutorial.markdown-renderer
  "Convert parsed tutorial markdown text into CGUI text-box component specs.

  Aligns with original AcademyCraft ACMarkdownRenderer behavior:
    - Headings (\"# ...\") → bold, larger font
    - Bold (\"__...__\") → bold font variant
    - Custom tags:
        ![key id=\"...\"] → keybinding display name (from lookup map)
        ![misakaname]    → \"Misaka No.<id>\"
    - Plain text → standard text-box specs
    - Word-wrapping: long lines split at word boundaries to fit max-content-width

  Returns a vector of pure data specs.  Caller creates CGUI widgets from them."
  (:require [clojure.string :as str]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as platform-bridge]
            [cn.li.mcmod.i18n :as i18n]))

;; --- Constants ---

(def default-font-size 8.0)
(def heading-font-size 10.0)
(def default-color 0xFFFFFFFF)
(def line-height 13.0)
(def indent-x 2.0)

;; Match original AcademyCraft GLMarkdownRenderer widthLimit of 150px
(def max-content-width 150)

;; --- Tag resolution ---

(def ^:private key-tag-re
  #"!\[key\s+id=\"([^\"]+)\"\]")

(def ^:private misaka-tag-re
  #"!\[misakaname\]")

(def ^:private image-re
  #"!\[([^\]]*)\]\(([^\)]+)\)")

(defn key-display-name
  "Localized label for a keybinding prop id, or nil when the id is unknown.
  Mirrors the settings app's `settings.<modid>.prop.<id>` lookup; the platform
  translator echoes the key back when the language file has no entry."
  [key-id]
  (let [k (str "settings." modid/MOD-ID ".prop." key-id)
        v (i18n/translate k)]
    (when (and (string? v) (not= v k) (not (str/blank? v)))
      v)))

(defn- misaka-name
  "Localized 'Misaka No.<id>' via the tutorial.<MOD-ID>.misaka key, formatted
   with the misaka id like upstream ac.tutorial.misaka. Falls back to the
   English literal when no lang entry is available (tests run without a
   translate fn)."
  [misaka-id]
  (let [k (str "tutorial." modid/MOD-ID ".misaka")
        v (i18n/translate k)]
    (if (and (string? v) (not= v k) (not (str/blank? v)))
      (i18n/translate k misaka-id)
      (str "Misaka No." misaka-id))))

(defn- resolve-inline-tags
  [line misaka-id]
  (-> line
      (str/replace key-tag-re
                   (fn [[_ key-id]]
                     (or (key-display-name key-id)
                         (str "[" key-id "]"))))
      (str/replace misaka-tag-re
                   (if misaka-id
                     (str "__" (misaka-name misaka-id) "__")
                     "__Misaka No.????__"))))

;; --- Segment helpers ---

(defn- is-heading-line? [line]
  (str/starts-with? (str/trim line) "#"))

(defn- strip-heading-marker [line]
  (str/trim (str/replace (str/trim line) #"^#+\s*" "")))

(defn- strip-bold-markers [line]
  (str/replace line #"__([^_]+)__" "$1"))

(defn- is-bold-line? [line]
  (boolean (re-find #"__[^_]+__" line)))

(defn- is-empty-line? [line]
  (str/blank? (str/trim line)))

(def default-image-height
  "Default pixel height for markdown images in tutorial content."
  100.0)

;; --- Word wrapping ---

(defn- find-break-index
  "Return the index in `s` where the text width first exceeds `max-px`.
  Returns the full length if it never exceeds."
  [s font-desc font-size max-px text-width-fn]
  (loop [i 1]
    (if (> i (count s))
      (count s)
      (if (> (text-width-fn font-desc (subs s 0 i) font-size) max-px)
        (dec i)   ; i-1 is the last index that still fits
        (recur (inc i))))))

(defn- wrap-line-proportional
  "Split a single line into sub-lines that fit within max-px pixels, using
  `text-width-fn` for per-character width measurement.  Works for both
  proportional and monospace fonts.

  text-width-fn: (fn [font-desc text font-size]) → pixel width
  font-desc:     map with :bold? key (or nil for default)"
  [line font-desc font-size max-px text-width-fn]
  (if (<= (text-width-fn font-desc line font-size) max-px)
    [line]
    (loop [remaining line
           acc []]
      (if (empty? remaining)
        acc
        (let [break-at (max 1 (find-break-index remaining font-desc font-size max-px text-width-fn))
              chunk   (subs remaining 0 break-at)]
          (if-let [last-space (str/last-index-of chunk " ")]
            ;; Break at the last word boundary within the fitting chunk
            (recur (subs remaining (inc last-space))
                   (conj acc (subs remaining 0 last-space)))
            ;; No space — hard-break at the fitting boundary
            (recur (subs remaining break-at)
                   (conj acc chunk))))))))

(defn- wrap-segments
  "Apply word-wrapping to text segments using proportional width measurement.
  text-width-fn must be a fn of (font-desc, text, font-size) → pixel width."
  [segments max-width-px text-width-fn]
  (mapcat (fn [seg]
            (if (= (:type seg) :image)
              [seg]
              (let [font-desc (when (:bold? seg) {:bold? true})
                    lines (wrap-line-proportional
                            (:text seg) font-desc (:font-size seg)
                            max-width-px text-width-fn)]
                (map (fn [line] (assoc seg :text line)) lines))))
          segments))

;; --- Main API ---

(defn render-segments
  "Parse raw content text into a vector of segment spec maps.

  Each segment:
    {:text      string
     :font-size float
     :color     int (ARGB)
     :bold?     boolean}

  text-width-fn (optional): (fn [font-desc text font-size]) → pixel width.
  When nil, resolves font-api/text-width at runtime for proportional
  wrapping; falls back to a conservative char-count estimate.

  Args:
    raw-content    — parsed markdown content string
    misaka-id      — int or nil, for ![misakaname] resolution
    max-width-px   — pixel width limit for word-wrapping
    text-width-fn  — nil or proportional width measurer"
  ([raw-content] (render-segments raw-content nil max-content-width nil))
  ([raw-content misaka-id] (render-segments raw-content misaka-id max-content-width nil))
  ([raw-content misaka-id max-width-px] (render-segments raw-content misaka-id max-width-px nil))
  ([raw-content misaka-id max-width-px text-width-fn]
   (let [text-width-fn (or text-width-fn
                           ;; Measure with the client font when the bridge is up; off-client
                           ;; (tests, dedicated server) fall back to a char-count estimate.
                           (fn [font-desc text font-size]
                             (or (platform-bridge/font-text-width-optional font-desc text font-size)
                                 (* (count text) font-size 0.6))))
         lines (str/split-lines (or raw-content ""))
         segments
         (loop [remaining lines
                segs []]
           (if (empty? remaining)
             segs
             (let [line (first remaining)
                   more (rest remaining)
                   trimmed (str/trim line)]
               (cond
                 ;; Standalone image: ![alt](path) matched against full trimmed line
                 ;; re-find returns [full-match & groups] vector → must compare (first m)
                 (let [m (re-find image-re trimmed)]
                   (and m (= trimmed (first m))))
                 (let [[_ alt-img texture-path] (re-find image-re trimmed)]
                   (recur more
                          (conj segs
                                {:type :image
                                 :texture-path texture-path
                                 :alt alt-img
                                 :img-h default-image-height})))

                 (is-empty-line? line)
                 (recur more
                        (conj segs
                              {:text "" :font-size default-font-size
                               :color default-color :bold? false}))

                 (is-heading-line? line)
                 (let [text (-> line strip-heading-marker
                                (resolve-inline-tags misaka-id))]
                   (recur more
                          (conj segs
                                {:text text :font-size heading-font-size
                                 :color default-color :bold? true})))

                 :else
                 (let [text (resolve-inline-tags line misaka-id)
                       bold? (is-bold-line? text)
                       clean-text (if bold? (strip-bold-markers text) text)]
                   (recur more
                          (conj segs
                                {:text clean-text :font-size default-font-size
                                 :color default-color :bold? bold?})))))))]
     ;; Word-wrap text segments; skip image segments
     (vec (wrap-segments segments max-width-px text-width-fn)))))
