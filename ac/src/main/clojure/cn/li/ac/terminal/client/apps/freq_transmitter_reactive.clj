(ns cn.li.ac.terminal.client.apps.freq-transmitter-reactive
  "Reactive port of AcademyCraft's Frequency Transmitter state machine."
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.client.toast :as toast]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.terminal.freq-network :as freq-net]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.xml :as ui-xml])
  (:import [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.runtime UiRt]))

(def ^:private normal-timeout-ms 20000)
(def ^:private transmitting-timeout-ms 3000)

(defonce ^:private overlay-sessions (atom {}))

(defn- net-owner
  "Owner for every RPC here. Without one send-to-server falls back to
   hooks/client-session-id, a ThreadLocal bound only inside a client-context
   callback -- these run from UI events and overlay ticks, where nothing binds
   it. default-client-owner derives the session from the live connection, so it
   holds on any client thread."
  []
  (runtime-hooks/default-client-owner))

(defn- player-uuid [player]
  (or (uuid/player-uuid player)
      (:player-uuid (runtime-hooks/default-client-owner))))

(defn- local [key fallback]
  (or (i18n/translate (str "app." modid/MOD-ID ".freq_transmitter." key))
      fallback))

(defn- content-node [^UiRt r]
  (rt/node-by-id r :content))

(defn- add-text!
  [^UiRt r y text & {:keys [color size]}]
  (rt/build-child!
    r
    {:kind :text
     :props {:id (keyword (str "freq-text-" (rand-int 1000000)))
             :x 10.0 :y (double y) :w 340.0 :h 28.0
             :text text :font-size (double (or size 10.0))
             :color (long (or color 0xFFFFFFFF))}}
    (content-node r)))

(defn- add-button! [^UiRt r y label click-fn]
  (let [id (keyword (str "freq-button-" (rand-int 1000000)))
        ^INode n
        (rt/build-child!
          r
          {:kind :box
           :props {:id id :x 105.0 :y (double y) :w 150.0 :h 30.0
                   :fill 0x773A4C5A :hover-tint 0x44FFFFFF}
           :children [{:kind :text
                       :props {:x 0.0 :y 7.0 :w 150.0 :h 16.0
                               :text label :font-size 10.0 :align :center
                               :color 0xFFFFFFFF}}]}
          (content-node r))]
    (rt/register-event! r (.getIdx n) :left-click
                        (fn [_ _ _] (click-fn)))))

(defn- add-password-field! [^UiRt r state]
  (add-text! r 35 (if (= :matrix (:source-type @state))
                    (str "SSID: " (:source-name @state))
                    (str "NAME: " (:source-name @state)))
             :color 0xFFBFBFBF)
  (add-text! r 61 "PASS:" :color 0xFFFFFFFF)
  (let [^INode n
        (rt/build-child!
          r
          {:kind :text
           :props {:id :freq-password :x 55.0 :y 61.0 :w 280.0 :h 24.0
                   :text (:password @state) :font-size 10.0
                   :color 0xFFFFFFFF :editable? true :masked? true}}
          (content-node r))]
    (rt/register-event! r (.getIdx n) :change-content
                        (fn [_ _ evt] (swap! state assoc :password (:value evt))))
    (events/gain-focus! r (.getIdx n))))

(declare rebuild!)

(defn- show-messages! [response]
  (doseq [m (:messages response)]
    (toast/show-toast! {:message-key (:key m) :args (:args m)})))

(defn- notice! [state message return-phase]
  (swap! state assoc
         :phase :notice
         :message message
         :return-phase return-phase
         :phase-start-ms (System/currentTimeMillis)
         :timeout-ms (if return-phase 700 1000)))

(defn- finish-overlay! [puuid]
  (swap! overlay-sessions dissoc (str puuid))
  (bridge/set-active-overlay-app! nil puuid))

(defn- start-link-overlay! [puuid state]
  (swap! overlay-sessions assoc (str puuid)
         {:phase :link
          :source-type (:source-type @state)
          :source-pos (:source-pos @state)
          :password (:password @state)
          :last-click? false
          :phase-start-ms (System/currentTimeMillis)
          :timeout-ms normal-timeout-ms})
  (bridge/set-active-overlay-app! :freq-tx puuid)
  (bridge/close-screen!))

(defn- scan-source! [^UiRt r state puuid]
  (swap! state assoc :phase :transmitting
         :message (local "s1_1" "Transmitting...")
         :phase-start-ms (System/currentTimeMillis)
         :timeout-ms transmitting-timeout-ms)
  (rebuild! r state puuid)
  (net-client/send-to-server
    (net-owner) freq-net/freq-scan-msg {:player-uuid puuid :range 4.0}
    (fn [response]
      (if (:success response)
        (let [{:keys [type pos ssid node-name]} (:device response)]
          (if (contains? #{:matrix :node} type)
            (swap! state assoc
                   :phase :authorize :source-type type :source-pos pos
                   :source-name (or ssid node-name "") :password ""
                   :phase-start-ms (System/currentTimeMillis)
                   :timeout-ms normal-timeout-ms)
            (notice! state (local "e4" "Invalid wireless target") nil)))
        (notice! state (or (:error response)
                           (local "e4" "Invalid wireless target"))
                 nil))
      (rebuild! r state puuid))))

(defn- authorize! [^UiRt r state puuid]
  (events/remove-focus! r)
  (swap! state assoc :phase :transmitting
         :message (local "s1_1" "Transmitting...")
         :phase-start-ms (System/currentTimeMillis)
         :timeout-ms transmitting-timeout-ms)
  (rebuild! r state puuid)
  (net-client/send-to-server
    (net-owner) freq-net/freq-config-msg
    {:operation :authorize
     :source-type (:source-type @state)
     :source-pos (:source-pos @state)
     :password (:password @state)}
    (fn [response]
      (if (:success response)
        (start-link-overlay! puuid state)
        (notice! state (local "e1" "Authorization failed") nil))
      (when-not (:success response)
        (rebuild! r state puuid)))))

(defn- link-target! [^UiRt r state puuid]
  (swap! state assoc :phase :transmitting
         :message (local "e5" "Transmitting...")
         :phase-start-ms (System/currentTimeMillis)
         :timeout-ms transmitting-timeout-ms)
  (rebuild! r state puuid)
  (net-client/send-to-server
    (net-owner) freq-net/freq-config-msg
    {:operation :link-target
     :source-type (:source-type @state)
     :source-pos (:source-pos @state)
     :password (:password @state)}
    (fn [response]
      (show-messages! response)
      (if (:success response)
        (notice! state (local "e6" "Link successful") :link)
        (notice! state
                 (or (:error response)
                     (if (= :matrix (:source-type @state))
                       (local "e2" "Failed to link node")
                       (local "e3" "Failed to link wireless user")))
                 nil))
      (rebuild! r state puuid))))

(defn rebuild! [^UiRt r state puuid]
  (rt/clear-children! r (content-node r))
  (case (:phase @state)
    :start
    (do
      (add-text! r 25 (local "s0_0" "Aim at a wireless matrix or node."))
      (add-button! r 80 "Select Target" #(scan-source! r state puuid)))

    :authorize
    (do
      (add-password-field! r state)
      (add-text! r 90
                 (if (= :matrix (:source-type @state))
                   (local "s1_0" "Enter matrix password")
                   (local "s1_1" "Enter node password"))
                 :color 0xFF30FFFF)
      (add-button! r 120 "Authorize" #(authorize! r state puuid)))

    :link
    (do
      (add-text! r 35
                 (if (= :matrix (:source-type @state))
                   (local "s2_0" "Aim at a wireless node to link.")
                   (local "s3_0" "Aim at a wireless user to link.")))
      (add-button! r 90 "Link Target" #(link-target! r state puuid)))

    :transmitting
    (add-text! r 75 (:message @state) :color 0xFF30FFFF :size 11.0)

    :notice
    (add-text! r 75 (:message @state) :color 0xFF30FFFF :size 11.0)

    nil)
  (rt/mark-tree-dirty! r))

(defn- install-state-tick! [^UiRt r state puuid]
  (rt/put-user-signal!
    r :freq-state-tick
    (sig/computed-o
      [(rt/clock-ms-sig r) (rt/partial-ticks-sig r)]
      (fn [_ _]
        (let [{:keys [phase phase-start-ms timeout-ms return-phase]} @state
              elapsed (- (System/currentTimeMillis) phase-start-ms)]
          (when (> elapsed timeout-ms)
            (cond
              (and (= phase :notice) return-phase)
              (do
                (swap! state assoc :phase return-phase
                       :phase-start-ms (System/currentTimeMillis)
                       :timeout-ms normal-timeout-ms)
                (rebuild! r state puuid))

              (= phase :notice)
              (bridge/close-screen!)

              :else
              (do
                (notice! state (local "st" "Transmission timed out") nil)
                (rebuild! r state puuid)))))
        nil))))

(defn create-runtime [player]
  (let [puuid (player-uuid player)
        r (rt/create-runtime)
        _ (rt/build! r
                     (ui-xml/load-spec
                       (modid/namespaced-path "guis/new/freq_transmitter.xml")))
        state (atom {:phase :start :password ""
                     :phase-start-ms (System/currentTimeMillis)
                     :timeout-ms normal-timeout-ms})]
    (install-state-tick! r state puuid)
    (rebuild! r state puuid)
    r))

(defn open!
  ([player]
   (bridge/open-reactive-screen!
     (create-runtime player)
     "Frequency Transmitter"
     {:render-background? false}))
  ([] (open! nil)))

(defn build-overlay-elements
  [player-uuid screen-width screen-height]
  (when-let [{:keys [phase source-type message]} (get @overlay-sessions (str player-uuid))]
    (let [prompt (case phase
                   :transmitting (local "e5" "Transmitting...")
                   :notice message
                   (if (= source-type :matrix)
                     (local "s2_0" "Aim at a wireless node and right-click.")
                     (local "s3_0" "Aim at a wireless user and right-click.")))
          title (or (i18n/translate
                      (str "app." modid/MOD-ID ".freq_transmitter"))
                    "Frequency Transmitter")
          box-w 180
          x (+ (/ screen-width 2.0) 10)
          y (+ (/ screen-height 2.0) 10)]
      [{:kind :fill :x 15 :y 15 :w 30 :h 18 :color 0x77272727}
       {:kind :texture :src (modid/asset-path "textures/guis/apps"
                                              "freq_transmitter/icon.png")
        :x 17 :y 15 :w 18 :h 18 :color 0xFFFFFFFF}
       {:kind :text :text title :x 39 :y 19 :color 0xFFFFFFFF}
       {:kind :fill :x x :y y :w box-w :h 28 :color 0x77272727}
       {:kind :text :text prompt :x (+ x 5) :y (+ y 8) :color 0xFFFFFFFF}])))

(defn overlay-active? [player-uuid]
  (contains? @overlay-sessions (str player-uuid)))

(defn- overlay-link! [player-uuid session]
  (let [key (str player-uuid)]
    (swap! overlay-sessions assoc key
           (assoc session
                  :phase :transmitting
                  :phase-start-ms (System/currentTimeMillis)
                  :timeout-ms transmitting-timeout-ms))
    (net-client/send-to-server
      (net-owner) freq-net/freq-config-msg
      {:operation :link-target
       :source-type (:source-type session)
       :source-pos (:source-pos session)
       :password (:password session)}
      (fn [response]
        (show-messages! response)
        (if (:success response)
          (swap! overlay-sessions assoc key
                 (assoc session
                        :phase :notice
                        :message (local "e6" "Link successful")
                        :return-phase :link
                        :phase-start-ms (System/currentTimeMillis)
                        :timeout-ms 700
                        :last-click? true))
          (swap! overlay-sessions assoc key
                 (assoc session
                        :phase :notice
                        :message (or (:error response)
                                     (if (= :matrix (:source-type session))
                                       (local "e2" "Failed to link node")
                                       (local "e3" "Failed to link wireless user")))
                        :return-phase nil
                        :phase-start-ms (System/currentTimeMillis)
                        :timeout-ms 1000
                        :last-click? true)))))))

(defn tick-overlay-input!
  "Advance the non-modal link stage and consume a right-click edge."
  [player-uuid right-click-down?]
  (let [key (str player-uuid)]
    (when-let [session (get @overlay-sessions key)]
      (let [now (System/currentTimeMillis)
            elapsed (- now (:phase-start-ms session))
            expired? (> elapsed (:timeout-ms session))]
        (cond
          (and (= :notice (:phase session)) expired? (:return-phase session))
          (swap! overlay-sessions assoc key
                 (assoc session :phase :link :return-phase nil
                        :phase-start-ms now :timeout-ms normal-timeout-ms))

          (and (= :notice (:phase session)) expired?)
          (finish-overlay! player-uuid)

          (and (not= :notice (:phase session)) expired?)
          (finish-overlay! player-uuid)

          (and (= :link (:phase session))
               right-click-down?
               (not (:last-click? session)))
          (overlay-link! player-uuid session)

          :else
          (swap! overlay-sessions assoc-in [key :last-click?]
                 (boolean right-click-down?)))))))
