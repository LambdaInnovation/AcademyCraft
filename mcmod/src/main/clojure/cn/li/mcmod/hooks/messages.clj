(ns cn.li.mcmod.hooks.messages
  "Platform-neutral runtime message-id registry.

  Business/content modules own concrete wire message ids and register them by
  generic keys. Shared Minecraft/platform code resolves ids through this seam
  without carrying business protocol strings.

  State stored in Framework [:registry :messages]."
  (:require [cn.li.mcmod.framework :as fw]))

(def ^:private messages-path [:registry :messages])

(defn- message-ids-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom messages-path {})
    {}))

(defn register-message!
  "Register a concrete wire message id for `message-key`."
  [message-key message-id]
  (when-not (keyword? message-key)
    (throw (ex-info "Message key must be a keyword"
                    {:message-key message-key})))
  (when-not (and (string? message-id) (seq message-id))
    (throw (ex-info "Message id must be a non-empty string"
                    {:message-key message-key :message-id message-id})))
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in (conj messages-path message-key) message-id))
  nil)

(defn register-messages!
  "Register a map of message-key → concrete wire message id."
  [messages]
  (doseq [[message-key message-id] messages]
    (register-message! message-key message-id))
  nil)

(defn maybe-msg-id [message-key]
  (get (message-ids-snapshot) message-key))

(defn msg-id [message-key]
  (or (maybe-msg-id message-key)
      (throw (ex-info "Runtime message id is not registered"
                      {:message-key message-key}))))

(defn valid-msg-id? [message-id]
  (contains? (set (vals (message-ids-snapshot))) message-id))

(defn clear-messages!
  "Clear registered message ids. Intended for tests."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in messages-path {}))
  nil)
