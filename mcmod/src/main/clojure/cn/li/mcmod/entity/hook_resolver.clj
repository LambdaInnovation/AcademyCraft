(ns cn.li.mcmod.entity.hook-resolver
  "Platform-neutral scripted entity hook resolver registry.

  Business/content modules register hook-id → impl-key resolver functions at
  runtime. Shared Minecraft code consumes this namespace without depending on
  the business module that owns the hook ids.

  State stored in Framework [:registry :hooks :hook-resolver]."
  (:require [cn.li.mcmod.framework :as fw]))

(def ^:private resolvers-path [:registry :hooks :hook-resolver])

(defn- resolvers-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom resolvers-path {})
    {}))

(defn register-resolver!
  "Register a resolver for `resolver-key`."
  [resolver-key resolver-fn]
  (when-not (keyword? resolver-key)
    (throw (ex-info "Hook resolver key must be a keyword" {:resolver-key resolver-key})))
  (when-not (fn? resolver-fn)
    (throw (ex-info "Hook resolver must be a function" {:resolver-key resolver-key})))
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in (conj resolvers-path resolver-key) resolver-fn))
  nil)

(defn get-resolver [resolver-key]
  (get (resolvers-snapshot) resolver-key))

(defn resolve-impl-key
  "Resolve a business hook-id through the registered resolver."
  [resolver-key hook-id]
  (when-let [resolver (get-resolver resolver-key)]
    (resolver hook-id)))

(defn clear-resolvers!
  "Clear registered resolvers. Intended for tests."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in resolvers-path {}))
  nil)
