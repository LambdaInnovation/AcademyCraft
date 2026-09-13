(ns cn.li.mcmod.platform.structured-data
  "Structured-data operations via Framework function map — pure relay, no MC deps.

   Platform installers bind concrete storage (CompoundTag on 1.20.1, Data Components
   on newer versions). Content code calls these wrappers; they lookup
   [:platform :structured-data-ops]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Contract keys — domain vocabulary (not CompoundTag / ListTag names)
(def structured-map-keys #{:sd-set-int! :sd-get-int :sd-set-string! :sd-get-string
                           :sd-set-boolean! :sd-get-boolean :sd-set-double! :sd-get-double
                           :sd-set-entry! :sd-get-entry :sd-get-structured :sd-get-list
                           :sd-has-key? :sd-set-float! :sd-get-float
                           :sd-set-long! :sd-get-long :sd-remove-entry!
                           :sd-set-string-list! :sd-get-string-list})
(def structured-list-keys #{:sd-append! :sd-list-size :sd-list-get :sd-list-get-structured})
(def structured-factory-keys #{:create-structured :create-list})

;; Installation
(defn install-structured-data-ops!
  "Install structured-data ops map. Keys match structured-map/list/factory key sets."
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [required (into structured-map-keys (into structured-list-keys structured-factory-keys))
          missing (seq (remove (set (keys ops-map)) required))]
      (swap! fw-atom assoc-in [:platform :structured-data-ops] ops-map)
      (when missing
        (log/error "Structured-data ops MISSING required keys:" (pr-str missing))))
    (log/error "Structured-data ops install FAILED: Framework atom nil")))

(defn install-has-key-fn!
  "Install a single has-key? override at [:platform :structured-data-ops :sd-has-key?]."
  [f _label]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in [:platform :structured-data-ops :sd-has-key?] f))
  nil)

;; Queries
(defn available? [] (boolean (get-in @(fw/fw-atom) [:platform :structured-data-ops])))
(defn current-ops [] (get-in @(fw/fw-atom) [:platform :structured-data-ops]))

;; Helper
(defn- call [k & args]
  (let [ops (current-ops)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required structured-data operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

;; Structured map access API
(defn set-int!      [c k v]     (call :sd-set-int! c k v))
(defn get-int       [c k]       (call :sd-get-int c k))
(defn set-string!   [c k v]     (call :sd-set-string! c k v))
(defn get-string    [c k]       (call :sd-get-string c k))
(defn set-boolean!  [c k v]     (call :sd-set-boolean! c k v))
(defn get-boolean   [c k]       (call :sd-get-boolean c k))
(defn set-double!   [c k v]     (call :sd-set-double! c k v))
(defn get-double    [c k]       (call :sd-get-double c k))
(defn set-entry!    [c k entry] (call :sd-set-entry! c k entry))
(defn remove-entry! [c k]       (call :sd-remove-entry! c k))
(defn get-entry     [c k]       (call :sd-get-entry c k))
(defn get-structured [c k]      (call :sd-get-structured c k))
(defn get-list      [c k]       (call :sd-get-list c k))
(defn has-key?      [c k]       (call :sd-has-key? c k))
(defn set-float!    [c k v]     (call :sd-set-float! c k v))
(defn get-float     [c k]       (call :sd-get-float c k))
(defn set-long!     [c k v]     (call :sd-set-long! c k v))
(defn get-long      [c k]       (call :sd-get-long c k))
(defn set-string-list! [c k vs] (call :sd-set-string-list! c k vs))
(defn get-string-list  [c k]    (call :sd-get-string-list c k))

;; Structured list access API
(defn append!          [lst el] (call :sd-append! lst el))
(defn list-size        [lst]    (call :sd-list-size lst))
(defn list-get         [lst i]  (call :sd-list-get lst i))
(defn list-structured  [lst i]  (call :sd-list-get-structured lst i))

;; Factory access API
(defn create-structured []
  (if-let [f (get (current-ops) :create-structured)]
    (f)
    (throw (ex-info "Structured-data ops not installed" {:key :create-structured}))))
(defn create-list []
  (if-let [f (get (current-ops) :create-list)]
    (f)
    (throw (ex-info "Structured-data ops not installed" {:key :create-list}))))

;; Convenience
(defn has-key-safe? [structured key]
  (try (boolean (has-key? structured key)) (catch Throwable _ false)))
(defn structured->map [structured]
  (let [result (java.util.HashMap.)]
    (if-let [f (get (current-ops) :sd-get-all-keys)]
      (doseq [k (f structured)] (.put result k (get-entry structured k)))
      ;; Without :sd-get-all-keys installed, can't enumerate keys from pure relay layer
      nil)
    (into {} result)))
(defn factory-initialized? []
  (boolean (and (get (current-ops) :create-structured) (get (current-ops) :create-list))))
