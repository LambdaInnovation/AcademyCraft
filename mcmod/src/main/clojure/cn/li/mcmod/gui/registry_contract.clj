(ns cn.li.mcmod.gui.registry-contract
  "Registry-phase GUI/network contracts validated at handler and catalog registration time."
  (:require [cn.li.mcmod.schema.core :as schema]))

(def known-owner-specs
  #{:server :client})

(def known-payload-routings
  #{:none :sync-routing})

(def ^:private owner-spec-schema
  [:enum :server :client])

(def ^:private payload-routing-schema
  [:enum :none :sync-routing])

(def ^:private sync-routing-schema
  [:map
   [:container-id int?]])

(def ^:private handler-contract-schema
  [:map
   [:owner-spec owner-spec-schema]
   [:payload-routing {:optional true} payload-routing-schema]])

(def ^:private screen-contract-schema
  [:map
   [:owner-spec owner-spec-schema]])

(def ^:private handler-contract-validator (schema/lazy-validator handler-contract-schema))
(defn- valid-handler-contract? [x]
  (schema/valid? (handler-contract-validator) x))
(def ^:private screen-contract-validator (schema/lazy-validator screen-contract-schema))
(defn- valid-screen-contract? [x]
  (schema/valid? (screen-contract-validator) x))
(def ^:private sync-routing-validator (schema/lazy-validator sync-routing-schema))
(defn- valid-sync-routing? [x]
  (schema/valid? (sync-routing-validator) x))

(defn require-sync-routing
  [routing]
  (if (valid-sync-routing? routing)
    routing
    (throw (ex-info "sync-routing contract violation"
                    {:contract :sync-routing
                     :value routing
                     :explain (schema/explain sync-routing-schema routing)}))))

(defn default-server-gui-handler-contract
  []
  {:owner-spec :server
   :payload-routing :sync-routing})

(defn default-client-screen-contract
  []
  {:owner-spec :client})

(defn- contract-ex-info
  [contract-type value explain]
  (ex-info (str contract-type " contract violation")
           {:contract contract-type
            :value value
            :explain explain}))

(defn- require-contract*
  [schema* valid? contract-type value]
  (if (valid? value)
    value
    (throw (contract-ex-info contract-type value (schema/explain schema* value)))))

(defn normalize-handler-contract
  [contract]
  (merge (default-server-gui-handler-contract) contract))

(defn normalize-screen-contract
  [contract]
  (merge (default-client-screen-contract) contract))

(defn validate-handler-contract!
  ([]
   (validate-handler-contract! (default-server-gui-handler-contract)))
  ([contract]
   (let [normalized (normalize-handler-contract contract)]
     (require-contract* handler-contract-schema valid-handler-contract? :handler-contract normalized)
     (when-not (= :server (:owner-spec normalized))
       (throw (ex-info "Server GUI handler contract requires :owner-spec :server"
                       {:contract normalized})))
     normalized)))

(defn validate-screen-contract!
  ([contract]
   (let [normalized (normalize-screen-contract contract)]
     (when-not (= :client (:owner-spec normalized))
       (throw (ex-info "Block CGUI screen contract requires :owner-spec :client"
                       {:contract normalized})))
     (require-contract* screen-contract-schema valid-screen-contract? :screen-contract normalized))))

(defn validate-handler-fn!
  "Fail fast when handler is not a function."
  [msg-id handler-fn]
  (when-not (fn? handler-fn)
    (throw (ex-info "Network handler must be a function"
                    {:msg-id msg-id
                     :handler handler-fn}))))

(defn handler-entry
  [handler-fn contract]
  {:fn handler-fn
   :contract (validate-handler-contract! contract)})

(defn registered-handler-fn
  [handler-entry]
  (when (map? handler-entry)
    (:fn handler-entry)))

(defn registered-handler-contract
  [handler-entry]
  (when (map? handler-entry)
    (:contract handler-entry)))

(defn contracts-compatible?
  "Return true when a registered handler contract matches the catalog domain contract."
  [registered-contract catalog-contract]
  (let [registered (validate-handler-contract! registered-contract)
        catalog (validate-handler-contract! catalog-contract)]
    (and (= (:owner-spec registered) (:owner-spec catalog))
         (= (:payload-routing registered) (:payload-routing catalog)))))

(defn verify-handler-registered!
  "Ensure msg-id is registered and its contract matches the catalog declaration."
  [handlers-by-msg-id msg-id catalog-contract]
  (let [catalog-contract* (validate-handler-contract! catalog-contract)]
    (if-let [entry (get handlers-by-msg-id msg-id)]
      (when-not (contracts-compatible? (registered-handler-contract entry)
                                        catalog-contract*)
        (throw (ex-info "Handler contract does not match message catalog contract"
                        {:msg-id msg-id
                         :registered (registered-handler-contract entry)
                         :catalog catalog-contract*})))
      (throw (ex-info "No network handler registered for catalog message"
                      {:msg-id msg-id
                       :catalog-contract catalog-contract*})))
    nil))

(defn verify-catalog-handlers!
  "Verify every catalog message spec has a registered handler with a matching contract."
  [handlers-by-msg-id catalog domain->contract]
  (doseq [{:keys [domain action msg-id]} (:specs catalog)]
    (let [domain-contract (or (get domain->contract domain)
                              (default-server-gui-handler-contract))]
      (verify-handler-registered! handlers-by-msg-id msg-id domain-contract)))
  nil)

(defn normalize-message-domain-contract
  [domain contract]
  (assoc (validate-handler-contract! contract)
         :domain domain))
