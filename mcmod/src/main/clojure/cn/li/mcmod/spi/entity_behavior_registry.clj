(ns cn.li.mcmod.spi.entity-behavior-registry)

(defonce ^:private behaviors* (atom {}))

(defn register-behavior! [id spec]
  (swap! behaviors* assoc (name id) spec)
  nil)

(defn get-behavior [id]
  (get @behaviors* (name id)))

(defn value [id key default]
  (get (get-behavior id) key default))
