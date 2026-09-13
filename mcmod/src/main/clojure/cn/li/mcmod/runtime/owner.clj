(ns cn.li.mcmod.runtime.owner
  "Shared runtime owner model: canonical session keys, store partition, and context route keys.

  Upper layers should use this API instead of reading :server-session-id, :client-session-id,
  or generic :session-id directly."
  (:require [clojure.string :as str]
            [cn.li.mcmod.schema.core :as schema]))

(def ^:private transport-route-key :owner/route-key)
(def ^:private transport-store-session :owner/store-session-id)

(def ^:private session-token-schema
  [:or keyword? vector? symbol?])

(def ^:private logical-side-schema
  [:enum :client :server])

(defn- ^:private valid-client-owner-extra?
  "Named predicate for Malli schema — avoids anon-fn AOT symbol leak."
  [m]
  (and (nil? (:server-session-id m))
       (or (nil? (:logical-side m))
           (= :client (:logical-side m)))))

(def ^:private client-owner-schema
  [:and
   [:map
    [:client-session-id session-token-schema]
    [:player-uuid {:optional true} string?]
    [:logical-side {:optional true} logical-side-schema]
    [:screen-id {:optional true} any?]
    [:channel-id {:optional true} any?]
    [:timeout-ms {:optional true} any?]
    [:client-network-session {:optional true} any?]]
   [:fn valid-client-owner-extra?]])

(defn- ^:private valid-server-owner-extra?
  "Named predicate for Malli schema — avoids anon-fn AOT symbol leak."
  [m]
  (and (nil? (:client-session-id m))
       (or (nil? (:logical-side m))
           (= :server (:logical-side m)))))

(def ^:private server-owner-schema
  [:and
   [:map
    [:server-session-id session-token-schema]
    [:player-uuid string?]
    [:logical-side {:optional true} logical-side-schema]]
   [:fn valid-server-owner-extra?]])

(def ^:private owner-schema
  [:or client-owner-schema server-owner-schema])

(defn- normalize-player-uuid
  [owner]
  (when (map? owner)
    (let [uuid (some-> (:player-uuid owner) str)]
      (when (not (str/blank? uuid))
        (assoc owner :player-uuid uuid)))))

(def ^:private client-owner-validator (schema/lazy-validator client-owner-schema))
(defn valid-client-owner? [x]
  (schema/valid? (client-owner-validator) (normalize-player-uuid x)))
(def ^:private server-owner-validator (schema/lazy-validator server-owner-schema))
(defn valid-server-owner? [x]
  (schema/valid? (server-owner-validator) (normalize-player-uuid x)))
(def ^:private owner-validator (schema/lazy-validator owner-schema))
(defn valid-owner? [x]
  (schema/valid? (owner-validator) (normalize-player-uuid x)))

(defn- contract-ex-info
  [contract value explain]
  (ex-info (str contract " contract violation")
           {:contract contract
            :value value
            :explain explain}))

(defn- require*
  [schema* valid? contract value]
  (let [value* (or (normalize-player-uuid value) value)]
    (if (valid? value*)
      value*
      (throw (contract-ex-info contract value* (schema/explain schema* value*))))))

(defn- require-player-uuid!
  [contract owner]
  (when (str/blank? (:player-uuid owner))
    (throw (ex-info (str contract " requires :player-uuid")
                    {:contract contract
                     :value owner
                     :required :player-uuid})))
  owner)

;; valid-client-owner? valid-server-owner? valid-owner? — lazy-memoized above

(defn explain-owner [owner]
  (schema/explain owner-schema (normalize-player-uuid owner)))

(defn require-client-owner
  [owner]
  (require-player-uuid! :client-owner
                        (require* client-owner-schema valid-client-owner? :client-owner owner)))

(defn require-server-owner
  [owner]
  (require* server-owner-schema valid-server-owner? :server-owner owner))

(defn require-owner
  [owner]
  (require* owner-schema valid-owner? :owner owner))

(defn logical-side
  [owner]
  (when (map? owner)
    (or (:logical-side owner)
        (when (:server-session-id owner) :server)
        (when (:client-session-id owner) :client))))

(defn player-uuid
  [owner]
  (some-> (:player-uuid (normalize-player-uuid owner)) str))

(defn store-session-id
  "Bare runtime-store partition token from a canonical owner."
  [owner]
  (or (:server-session-id owner)
      (:client-session-id owner)))

(defn require-store-session-id
  [owner component]
  (or (store-session-id owner)
      (throw (ex-info (str component " requires canonical owner store session")
                      {:owner owner}))))

(defn route-key
  "Context transport route identity; narrower than store session for multiplayer ctx-id isolation.

  Optional route-scope:
  - :server — server-side route including player when :player-uuid present
  - :client — client-side route including player when :player-uuid present
  - nil — infer from owner logical side"
  ([owner]
   (route-key owner nil))
  ([owner route-scope]
   (let [owner* (when owner (require-owner owner))
         side (or route-scope (logical-side owner*))
         store (store-session-id owner*)
         uuid (player-uuid owner*)]
     (cond
       (and (= :server side) store uuid) [store uuid]
       (and (= :client side) store uuid) [store uuid]
       store store
       :else (throw (ex-info "Owner route-key requires store session"
                             {:owner owner*
                              :route-scope route-scope}))))))

(defn transport-route-key
  [ctx]
  (get ctx transport-route-key))

(defn transport-store-session-id
  [ctx]
  (get ctx transport-store-session-id))

(defn- normalize-logical-side
  [logical-side]
  (case logical-side
    (:client "client" :logical-side/client) :client
    (:server "server" :logical-side/server) :server
    (:any "any") :any
    logical-side))

(defn attach-transport-owner-metadata!
  "Attach private dispatcher metadata to a transport context map."
  [ctx owner route-scope]
  (let [owner* (require-owner owner)
        rk (route-key owner* route-scope)
        store (store-session-id owner*)]
    (assoc ctx
           transport-route-key rk
           transport-store-session-id store
           :logical-side (or (:logical-side ctx)
                             (normalize-logical-side route-scope)
                             (logical-side owner*))
           :player-uuid (or (:player-uuid ctx) (player-uuid owner*)))))

(defn public-context
  "Strip private owner/route metadata before returning a context to content layers."
  [ctx]
  (when ctx
    (dissoc ctx
            transport-route-key
            transport-store-session-id
            :session-id
            :server-session-id
            :client-session-id)))

(defn canonical-owner-from-transport
  "Rebuild a canonical owner map from dispatcher transport metadata."
  [ctx]
  (when ctx
    (let [side (or (:logical-side ctx)
                   (when (:server-id ctx) :server)
                   :client)
          store (transport-store-session-id ctx)
          uuid (some-> (:player-uuid ctx) str)]
      (cond
        (and (= :server side) store uuid)
        {:logical-side :server :server-session-id store :player-uuid uuid}

        (and (= :client side) store)
        (cond-> {:logical-side :client :client-session-id store}
          uuid (assoc :player-uuid uuid))

        :else nil))))

(defmacro with-owner
  [owner-var owner-expr & body]
  `(let [~owner-var (require-owner ~owner-expr)]
     ~@body))
