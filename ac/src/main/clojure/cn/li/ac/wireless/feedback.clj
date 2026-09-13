(ns cn.li.ac.wireless.feedback
  "Maps wireless operation results to translatable feedback messages.

  Pure functions — callers are responsible for attaching :messages to
  handler response maps and for client-side toast rendering."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.i18n :as i18n]))


;; ============================================================================
;; Reason → i18n key mapping
;; ============================================================================

(def ^:private reason->key
  "Maps [domain reason] to translation keys from ac_content_translations.clj.
  Keys follow the original AcademyCraft pattern: app.academy.freq_transmitter.eN"
  {:node    {:not-found (str "app." modid/MOD-ID ".freq_transmitter.e0")
             :password  (str "app." modid/MOD-ID ".freq_transmitter.e1")
             :capacity  (str "app." modid/MOD-ID ".freq_transmitter.e2")
             :range     (str "app." modid/MOD-ID ".freq_transmitter.e2")
             :aborted   (str "app." modid/MOD-ID ".freq_transmitter.e4")
             :success   (str "app." modid/MOD-ID ".freq_transmitter.e6")
             :pending   (str "app." modid/MOD-ID ".freq_transmitter.e5")}
   :matrix  {:not-initialized (str "app." modid/MOD-ID ".freq_transmitter.e0")
             :password        (str "app." modid/MOD-ID ".freq_transmitter.e1")
             :ssid-exists     (str "app." modid/MOD-ID ".freq_transmitter.e2")
             :ssid-taken      (str "app." modid/MOD-ID ".freq_transmitter.e2")
             :capacity        (str "app." modid/MOD-ID ".freq_transmitter.e2")
             :not-found       (str "app." modid/MOD-ID ".freq_transmitter.e0")
             :aborted         (str "app." modid/MOD-ID ".freq_transmitter.e4")
             :success         (str "app." modid/MOD-ID ".freq_transmitter.e6")}
   :generator {:password      (str "app." modid/MOD-ID ".freq_transmitter.e1")
               :capacity      (str "app." modid/MOD-ID ".freq_transmitter.e2")
               :range         (str "app." modid/MOD-ID ".freq_transmitter.e2")
               :not-a-generator (str "app." modid/MOD-ID ".freq_transmitter.e0")
               :aborted       (str "app." modid/MOD-ID ".freq_transmitter.e4")
               :success       (str "app." modid/MOD-ID ".freq_transmitter.e6")
               :pending       (str "app." modid/MOD-ID ".freq_transmitter.e5")}
   :developer {:password     (str "app." modid/MOD-ID ".freq_transmitter.e1")
               :capacity     (str "app." modid/MOD-ID ".freq_transmitter.e2")
               :range        (str "app." modid/MOD-ID ".freq_transmitter.e2")
               :not-found    (str "app." modid/MOD-ID ".freq_transmitter.e0")
               :not-a-node   (str "app." modid/MOD-ID ".freq_transmitter.e0")
               :not-a-receiver (str "app." modid/MOD-ID ".freq_transmitter.e0")
               :not-a-generator (str "app." modid/MOD-ID ".freq_transmitter.e0")
               :aborted      (str "app." modid/MOD-ID ".freq_transmitter.e4")
               :success      (str "app." modid/MOD-ID ".freq_transmitter.e6")
               :pending      (str "app." modid/MOD-ID ".freq_transmitter.e5")}
   :ability-interferer {:password      (str "app." modid/MOD-ID ".freq_transmitter.e1")
                        :capacity      (str "app." modid/MOD-ID ".freq_transmitter.e2")
                        :range         (str "app." modid/MOD-ID ".freq_transmitter.e2")
                        :not-a-node    (str "app." modid/MOD-ID ".freq_transmitter.e0")
                        :not-a-receiver (str "app." modid/MOD-ID ".freq_transmitter.e0")
                        :aborted       (str "app." modid/MOD-ID ".freq_transmitter.e4")
                        :success       (str "app." modid/MOD-ID ".freq_transmitter.e6")
                        :pending       (str "app." modid/MOD-ID ".freq_transmitter.e5")}})


;; ============================================================================
;; Public API
;; ============================================================================

(defn result->messages
  "Convert a wireless operation result to a :messages vector suitable for
  handler response maps and toast rendering.

  result: {:success bool :reason kw}
  domain: :node | :matrix | :generator | :developer

  Returns a vector of message maps (or nil if no message).
  Each message: {:type :translatable :key \"...\" :args [...]}"
  [domain result]
  (let [reason (if (:success result)
                 :success
                 (or (:reason result) :aborted))
        domain-map (get reason->key domain)
        key (get domain-map reason)]
    (when key
      [{:type :translatable :key key :args []}])))


(defn reason->i18n-key
  "Return the i18n translation key for a given domain and reason keyword.
  Returns nil if no mapping exists."
  [domain reason]
  (get-in reason->key [domain reason]))


(defn translate-message
  "Translate a single message map to a localized string.
  Uses the platform i18n translate function."
  [{:keys [key args]}]
  (let [template (i18n/translate key)]
    (if (seq args)
      (apply format template args)
      template)))
