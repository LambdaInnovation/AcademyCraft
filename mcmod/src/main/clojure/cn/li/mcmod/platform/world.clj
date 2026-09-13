(ns cn.li.mcmod.platform.world
  "World operations via Framework function map — pure relay layer, no MC dependencies."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def world-ops-keys
  #{:world-get-tile-entity :world-get-block-state :world-set-block :world-remove-block
    :world-break-block :world-place-block-by-id :world-is-chunk-loaded?
    :world-get-day-time :world-get-game-time :world-get-dimension-id :world-server-session-id
    :world-get-players :world-is-raining :world-is-client-side :world-can-see-sky})

(def block-state-keys
  #{:block-state-is-air :block-state-is-replaceable? :block-state-get-block
    :block-state-get-state-definition :block-state-get-property :block-state-set-property})

(defn install-world-ops!
  "Install world ops to [:platform :world-ops]."
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [missing (seq (remove (set (keys ops-map)) world-ops-keys))]
      (swap! fw-atom assoc-in [:platform :world-ops] ops-map)
      (when missing
        (log/error "World ops MISSING required keys:" (pr-str missing))))
    (log/error "World ops install FAILED: Framework atom nil")))

(defn install-block-state-ops!
  "Install block-state ops to [:platform :block-state-ops]."
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [missing (seq (remove (set (keys ops-map)) block-state-keys))]
      (swap! fw-atom assoc-in [:platform :block-state-ops] ops-map)
      (when missing
        (log/error "Block-state ops MISSING required keys:" (pr-str missing))))
    (log/error "Block-state ops install FAILED: Framework atom nil")))

(defn available? [] (boolean (get-in @(fw/fw-atom) [:platform :world-ops])))
(defn current-ops          [] (get-in @(fw/fw-atom) [:platform :world-ops]))
(defn current-block-state-ops [] (get-in @(fw/fw-atom) [:platform :block-state-ops]))

(defn- world-call [k & args]
  (let [ops (current-ops)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required world operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))
(defn- bs-call [k & args]
  (let [ops (current-block-state-ops)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required block-state operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

;; World access API.
(defn get-tile-entity    [w pos]       (world-call :world-get-tile-entity w pos))
(defn get-block-state    [w pos]       (world-call :world-get-block-state w pos))
(defn set-block!         [w pos s fl]  (world-call :world-set-block w pos s fl))
(defn remove-block!      [w pos]       (world-call :world-remove-block w pos))
(defn break-block!       [w pos drop?] (world-call :world-break-block w pos drop?))
(defn place-block-by-id! [w id pos fl] (world-call :world-place-block-by-id w id pos fl))
(defn chunk-loaded?      [w cx cz]     (world-call :world-is-chunk-loaded? w cx cz))
(defn day-time           [w]           (world-call :world-get-day-time w))
(defn game-time          [w]           (world-call :world-get-game-time w))
(defn dimension-id       [w]           (world-call :world-get-dimension-id w))
(defn server-session-id  [w]           (world-call :world-server-session-id w))
(defn players            [w]           (world-call :world-get-players w))
(defn raining?           [w]           (world-call :world-is-raining w))
(defn client-side?       [w]           (world-call :world-is-client-side w))
(defn can-see-sky?       [w pos]       (world-call :world-can-see-sky w pos))

;; BlockState wrappers — use separate [:platform :block-state-ops]
(defn block-state-is-air               [bs]       (bs-call :block-state-is-air bs))
(defn block-state-is-replaceable?      [bs]       (bs-call :block-state-is-replaceable? bs))
(defn block-state-get-block            [bs]       (bs-call :block-state-get-block bs))
(defn block-state-get-state-definition [bs]       (bs-call :block-state-get-state-definition bs))
(defn block-state-get-property         [bs sd pn] (bs-call :block-state-get-property bs sd pn))
(defn block-state-set-property         [bs p v]   (bs-call :block-state-set-property bs p v))

(defn block-to-chunk-coord [block-coord] (bit-shift-right block-coord 4))
(defn is-chunk-loaded-at-block? [world x z]
  (let [cx (block-to-chunk-coord x) cz (block-to-chunk-coord z)]
    (chunk-loaded? world cx cz)))
