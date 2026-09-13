(ns cn.li.ac.block.wind-gen.gui-reactive
  "Reactive GUI registration for the Wind Generator main and base screens."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg] [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.mcmod.platform.item :as pitem] [cn.li.ac.energy.operations :as energy]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.util.log :as log] [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.gui.block-gui-reactive :as bgui]
            [cn.li.mcmod.ui.runtime :as rt] [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.ac.block.gui.sync :as gui-sync]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.block.wind-gen.schema :as wind-schema]))

;; ============================================================================
;; Wind Generator GUIs (main + base)
;; ============================================================================

(defn- block-altitude
  "Info-area \"altitude\" value (upstream: tile.getPos.getY). The block never
   moves, so resolve it once at screen creation rather than per frame."
  [container]
  (or (try (some-> (:tile-entity container) pos/block-pos pos/pos-y str)
           (catch Exception _ nil))
      "..."))

;; ============================================================================
;; Main GUI (fan slot)
;; ============================================================================
(def ^:private main-schema-id :wind-gen-main) (def ^:private main-sync (gui-sync/schema-sync-fns wind-schema/wind-gen-main-schema))
(defn- fan-item-stack? [s] (when (and s (not (try (pitem/empty? s) (catch Exception _ true)))) (let [^String rn (try (some-> s pitem/object pitem/registry-name str) (catch Exception _ nil))] (or (= rn "windgen_fan") (= rn (modid/namespaced-path "windgen_fan")) (and rn (.endsWith rn ":windgen_fan"))))))
(defn- create-main-container [tile player] (gui-sync/create-schema-container wind-schema/wind-gen-main-schema tile player :wind-gen-main {:gui-id (gui-manifest/gui-id :wind-gen-main)}))
(defn- main-slot-count [_] (slot-schema/tile-slot-count main-schema-id))
(defn- main-get-slot [c i] (common/get-slot-item-be c i))
(defn- main-set-slot! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity {:sync-client? true}))
(defn- main-can-place? [_ _ s] (fan-item-stack? s))
(defn- main-still-valid? [_ _] true)
(def ^:private main-server-sync! (:server-menu-sync! main-sync))
(defn- main-container? [c] (= (:container-type c) :wind-gen-main))
(defn- create-main-screen [container menu player]
  (let [altitude (block-altitude container)]
    (bgui/create-screen {:page-xml "guis/rework/new/page_windmain.xml" :texture-name "windmain"
                          :container container :menu menu :histograms []
                          :properties {:altitude (fn [] altitude)
                                       :fan (fn [] (if @(:fan-installed container) "YES" "NO"))
                                       :obstacle (fn [] (if @(:no-obstacle container) "CLEAR" "BLOCKED"))}
                          :wireless? false})))

;; ============================================================================
;; Base GUI (energy + wireless)
;; ============================================================================
(def ^:private base-schema-id :wind-gen-base) (def ^:private base-sync (gui-sync/schema-sync-fns wind-schema/wind-gen-base-schema))
(defn- create-base-container [tile player] (gui-sync/create-schema-container wind-schema/wind-gen-base-schema tile player :wind-gen-base {:gui-id (gui-manifest/gui-id :wind-gen-base)}))
(defn- base-slot-count [_] (slot-schema/tile-slot-count base-schema-id))
(defn- base-get-slot [c i] (common/get-slot-item-be c i))
(defn- base-set-slot! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity))
(defn- base-can-place? [_ _ s] (energy/is-energy-item-supported? s))
(defn- base-still-valid? [_ _] true)
(def ^:private base-server-sync! (:server-menu-sync! base-sync))
(defn- base-container? [c] (= (:container-type c) :wind-gen-base))

(defn- completeness-alpha [completeness status]
  ;; Tile :completeness values are (name kw) strings: "complete" / "no-top" /
  ;; "base-only" — "no_top" (underscore) never matched, so a tower missing only
  ;; its top kept the main+middle icons dark.
  (case completeness "complete" (if (= status "COMPLETE") [1.0 1.0 1.0] [0.6 1.0 1.0]) "no-top" [0.2 1.0 1.0] [0.2 0.2 1.0]))

(defn- attach-structure-bind! [r container _menu _player _signals]
  (let [clock (rt/clock-ms-sig r)
        main-alpha (sig/computed-d [clock] (fn [_] (first (completeness-alpha (or @(:completeness container) "") (or @(:status container) "")))))
        middle-alpha (sig/computed-d [clock] (fn [_] (second (completeness-alpha (or @(:completeness container) "") (or @(:status container) "")))))]
    (ui/bind! r :icon_main :alpha main-alpha)
    (ui/bind! r :icon_middle :alpha middle-alpha)))

(defn- create-base-screen [container menu player]
  (let [altitude (block-altitude container)]
    (bgui/create-screen
      {:page-xml "guis/rework/new/page_windbase.xml" :texture-name "windbase"
       :container container :menu menu
       ;; Upstream wind GUI info page: histEnergy (blue, "%.0f IF").
       :histograms [(bgui/hist-buffer (fn [] (double (or @(:energy container) 0.0)))
                                      (fn [] (max 1.0 (double (or @(:max-energy container) 1.0))))
                                      {:label "Energy" :color 0xFF25C4FF
                                       :desc-fn (fn [] (format "%.0f IF" (double (or @(:energy container) 0.0))))})]
       :properties {:gen_speed (fn [] (format "%.2fIF/T" (double (or @(:gen-speed container) 0.0))))
                    :status (fn [] (or @(:status container) "IDLE"))
                    :altitude (fn [] altitude)}
       :wireless? true :wireless-role :generator :custom-bind! attach-structure-bind!})))

(def update! bgui/update-signals!) (def open! bgui/open!)

;; ============================================================================
;; Registration
;; ============================================================================
(defn init-wind-gen-reactive! []
  (install/framework-once! ::wind-gen-reactive-installed?
  (fn []
    (slot-schema/register-slot-schema! {:schema-id main-schema-id :slots [{:id :fan :type :standard :x 78 :y 9}]})
    (slot-schema/register-slot-schema! {:schema-id base-schema-id :slots [{:id :energy :type :energy :x 42 :y 80}]})
    (gui-reg/register-block-gui! (gui-manifest/gui-name :wind-gen-main) (merge (gui-manifest/gui-registration :wind-gen-main) {:container-predicate main-container? :container-fn create-main-container :screen-fn create-main-screen :server-menu-sync-fn main-server-sync! :validate-fn main-still-valid? :close-fn (:on-close main-sync) :slot-count-fn main-slot-count :slot-get-fn main-get-slot :slot-set-fn main-set-slot! :slot-can-place-fn main-can-place? :slot-changed-fn (fn [_ _] nil)}))
    (gui-reg/register-block-gui! (gui-manifest/gui-name :wind-gen-base) (merge (gui-manifest/gui-registration :wind-gen-base) {:container-predicate base-container? :container-fn create-base-container :screen-fn create-base-screen :server-menu-sync-fn base-server-sync! :validate-fn base-still-valid? :close-fn (:on-close base-sync) :slot-count-fn base-slot-count :slot-get-fn base-get-slot :slot-set-fn base-set-slot! :slot-can-place-fn base-can-place? :slot-changed-fn (fn [_ _] nil)}))
    (log/debug "Wind Generator GUIs initialized (reactive: main + base)"))))
