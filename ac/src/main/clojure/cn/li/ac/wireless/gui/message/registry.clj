(ns cn.li.ac.wireless.gui.message.registry
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.gui.message.dsl :as msg-dsl]
            [cn.li.mcmod.gui.registry-contract :as registry-contract]))

;; Registry — Framework [:registry :messages :wireless]

(def ^:private wmsg-path [:registry :messages :wireless])

(defn- wireless-message-registry-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom wmsg-path {:domains {} :frozen? false})
    {:domains {} :frozen? false}))

(defn- update-wireless-message-registry-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in wmsg-path
           (fn [current] (apply f (or current {:domains {} :frozen? false}) args))))
  nil)

(def ^:private message-prefix "wireless")

(defn- assert-registry-open!
  []
  (when (:frozen? (wireless-message-registry-state-snapshot))
    (throw (ex-info "Wireless GUI message registry is frozen" {}))))

(defn registry-snapshot []
  (wireless-message-registry-state-snapshot))

(defn reset-registry-for-test!
  ([] (reset-registry-for-test! {}))
  ([{:keys [domains frozen?]
     :or {domains {} frozen? false}}]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in wmsg-path {:domains domains :frozen? frozen?}))
   nil))

(defn freeze-registry!
  []
  (update-wireless-message-registry-state! assoc :frozen? true)
  nil)

(defn register-block-messages!
  ([domain actions]
   (register-block-messages! domain actions (registry-contract/default-server-gui-handler-contract)))
  ([domain actions contract]
   (let [contract* (registry-contract/normalize-message-domain-contract domain contract)
         spec (msg-dsl/build-domain-spec message-prefix domain actions contract*)]
     (if-let [existing (get (:domains (wireless-message-registry-state-snapshot)) domain)]
       (when-not (= existing spec)
         (throw (ex-info "Conflicting wireless GUI message domain"
                         {:domain domain :existing existing :new spec})))
       (do
         (assert-registry-open!)
         (update-wireless-message-registry-state! assoc-in [:domains domain] spec)))
     spec)))

(defn get-domain-spec [domain]
  (get (:domains (wireless-message-registry-state-snapshot)) domain))

(defn build-catalog []
  (msg-dsl/build-catalog (vals (:domains (wireless-message-registry-state-snapshot)))))

(defn msg
  "Resolve a message ID for the given domain+action.
   Uses direct ID computation (matching build-domain-spec formula)
   so callers don't depend on catalog being populated first.
   Format: <prefix>_<domain>_<action> with underscores for hyphens."
  [domain action]
  (msg-dsl/message-id message-prefix domain action))
