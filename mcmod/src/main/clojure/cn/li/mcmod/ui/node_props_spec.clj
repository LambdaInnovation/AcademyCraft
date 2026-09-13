(ns cn.li.mcmod.ui.node-props-spec
  "Build-time node props validation — layout fields vs kind slot props."
  (:require [cn.li.mcmod.schema.core :as schema]))

(def ^:private layout-props-schema
  [:map
   [:x {:optional true} number?]
   [:y {:optional true} number?]
   [:w {:optional true} number?]
   [:h {:optional true} number?]
   [:scale {:optional true} number?]
   [:z {:optional true} number?]
   [:pivot-x {:optional true} number?]
   [:pivot-y {:optional true} number?]
   [:align-w {:optional true} [:enum :left :center :right]]
   [:align-h {:optional true} [:enum :top :middle :bottom]]
   [:visible? {:optional true} boolean?]
   [:clip? {:optional true} boolean?]
   [:transform? {:optional true} boolean?]
   [:id {:optional true} [:or keyword? string?]]])

(def ^:private node-template-schema
  [:map
   [:kind keyword?]
   [:id {:optional true} [:or keyword? string? nil?]]
   [:props {:optional true} map?]
   [:children {:optional true} [:or nil? vector?]]])

(def ^:private kind-props-schemas
  {:image [:map
            [:src {:optional true} [:or string? nil?]]
            [:tint {:optional true} [:or number? vector? keyword?]]
            [:alpha {:optional true} number?]
            [:u {:optional true} number?]
            [:v {:optional true} number?]
            [:tex-w {:optional true} number?]
            [:tex-h {:optional true} number?]]
   :text [:map
          [:text {:optional true} [:or string? nil?]]
          [:color {:optional true} number?]
          [:font-size {:optional true} number?]
          [:font {:optional true} [:or string? keyword? map? nil?]]
          [:align {:optional true} [:or string? keyword? nil?]]]
   :list [:map
          [:template {:optional true} node-template-schema]
          [:spacing {:optional true} number?]
          [:scroll-offset {:optional true} number?]]
   :box [:map
         [:fill {:optional true} number?]
         [:outline {:optional true} number?]
         [:outline-width {:optional true} number?]
         [:tint {:optional true} number?]
         [:hover-tint {:optional true} number?]]
   :line [:map
          [:x1 {:optional true} number?]
          [:y1 {:optional true} number?]
          [:x2 {:optional true} number?]
          [:y2 {:optional true} number?]
          [:thickness {:optional true} number?]
          [:alpha {:optional true} number?]
          [:color {:optional true} number?]]})

(def ^:private layout-validator (schema/lazy-validator layout-props-schema))

(def ^:private kind-validators
  (into {}
        (map (fn [[k sch]] [k (schema/lazy-validator sch)])
             kind-props-schemas)))

(defn- throw-invalid!
  [label kind prop schema value]
  (throw (ex-info (str "Invalid UI " label " prop"
                       (when kind (str " for kind " kind))
                       (when prop (str "/" prop))
                       ": " (pr-str (schema/explain schema value)))
                  {:kind kind :prop prop :value value
                   :explain (schema/explain schema value)})))

(defn validate-layout-props!
  [props]
  (when (and props (not (schema/valid? (layout-validator) props)))
    (throw-invalid! "layout" nil nil layout-props-schema props))
  props)

(defn validate-kind-props!
  "Validate kind slot props at build/init time (pilot kinds only)."
  [kind props]
  (when-let [vfn (get kind-validators kind)]
    (when (and props (not (schema/valid? (vfn) props)))
      (throw-invalid! "kind" kind nil (get kind-props-schemas kind) props)))
  props)

(defn validate-build-props!
  [kind props]
  (validate-layout-props! props)
  (validate-kind-props! kind props))

(defn validate-kind-prop!
  "Validate a single prop write (set-prop!/set-node-prop!)."
  [kind prop-key value]
  (when-let [vfn (get kind-validators kind)]
    (let [props {prop-key value}
          schema (get kind-props-schemas kind)]
      (when-not (schema/valid? (vfn) props)
        (throw-invalid! "kind" kind prop-key schema props))))
  value)

(defn pilot-kind?
  [kind]
  (contains? kind-props-schemas kind))
