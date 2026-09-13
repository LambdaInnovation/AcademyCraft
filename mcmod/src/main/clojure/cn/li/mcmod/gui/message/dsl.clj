(ns cn.li.mcmod.gui.message.dsl
  "Message DSL helpers for content-owned GUI message catalogs."
  (:require [clojure.string :as str]))

(def ^:private default-prefix "content")

(def ^:private message-id-pattern
  #"[a-z0-9_]+_[a-z0-9_]+_[a-z0-9_]+")

(defn- segment->token
  [segment]
  (-> (name segment)
      (str/replace "-" "_")))

(defn message-id
  "Build a message id with a stable caller-owned prefix.

  Format: <prefix>_<domain>_<action>.
  Prefix, domain, and action are normalized to underscore tokens."
  ([domain action]
   (message-id default-prefix domain action))
  ([prefix domain action]
   (str (segment->token prefix) "_" (segment->token domain) "_" (segment->token action))))

(defn build-domain-spec
  "Build message spec for a domain and validate duplicate actions early.

  Arities:
  - [domain actions] — default prefix
  - [domain actions contract] — third arg is a contract map
  - [prefix domain actions] — third arg is the action vector
  - [prefix domain actions contract]

  Contract keys:
  - :owner-spec (:server|:client)
  - :payload-routing (:none|:sync-routing)"
  ([domain actions]
   (build-domain-spec default-prefix domain actions {}))
  ([a b c]
   (if (map? c)
     (build-domain-spec default-prefix a b c)
     (build-domain-spec a b c {})))
  ([prefix domain actions contract]
   (let [dupes (->> actions frequencies (keep (fn [[k n]] (when (> n 1) k))))]
     (when (seq dupes)
       (throw (ex-info "Duplicate actions in domain"
                       {:domain domain :duplicate-actions (vec dupes)})))
     {:prefix prefix
      :domain domain
      :contract contract
      :messages (into {}
                      (map (fn [action]
                             [action (message-id prefix domain action)])
                           actions))
      :specs (mapv (fn [action]
                     {:prefix prefix
                      :domain domain
                      :action action
                      :msg-id (message-id prefix domain action)
                      :contract contract})
                   actions)})))

(defn build-catalog
  "Merge domain specs and validate global message-id uniqueness and format."
  [domain-specs]
  (let [all-specs (vec (mapcat #(get % :specs) domain-specs))
        by-id (group-by #(get % :msg-id) all-specs)
        dup-ids (->> by-id
                     (keep (fn [[msg-id entries]]
                             (when (> (count entries) 1)
                               msg-id))))
        invalid-ids (->> all-specs
                         (remove (fn [{:keys [msg-id]}]
                                   (boolean (re-matches message-id-pattern msg-id))))
                         (mapv #(get % :msg-id)))]
    (when (seq dup-ids)
      (throw (ex-info "Duplicate message ids"
                      {:duplicate-msg-ids (vec dup-ids)})))
    (when (seq invalid-ids)
      (throw (ex-info "Invalid message ids"
                      {:invalid-msg-ids invalid-ids})))
    {:domains (into {}
                    (map (fn [domain-spec]
                           [(:domain domain-spec) (:messages domain-spec)])
                         domain-specs))
     :specs all-specs
     :by-msg-id (into {}
                      (map (fn [spec]
                             [(:msg-id spec) (select-keys spec [:domain :action])])
                           all-specs))}))

(defn msg-id
  [catalog domain action]
  (or (get-in catalog [:domains domain action])
      (throw (ex-info "Unknown GUI message"
                      {:domain domain :action action}))))

(defn find-by-msg-id
  [catalog msg-id]
  (get-in catalog [:by-msg-id msg-id]))
