(ns cn.li.ac.wireless.gui.tab.view-reactive
  "Reactive view-layer for wireless tab — list-set! + editable password rows."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.i18n :as i18n]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.events :as events]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.uipojo.signal ISigL]
           [cn.li.mcmod.ui.node INode]))

(def ^:private row-h 16.0)   ;; template item height; spacing (DSlot 0) is set separately before list-set!
(def ^:private visible-h 115.0)

(def ^:private tex-connected
  (modid/asset-path "textures" "guis/icons/icon_connected.png"))

(def ^:private tex-unconnected
  (modid/asset-path "textures" "guis/icons/icon_unconnected.png"))

(defn- set-image-alpha! [r ^INode n alpha]
  (when n (ui/set-node-prop! r n :alpha alpha)))

(defn- wire-avail-row!
  [^UiRt rt item target name connect-fn encrypted?]
  ;; XML-parsed ids are keywords; string lookups fail
  (let [^INode text-name (ui/item-node item :el_text_name)
        ^INode icon-key (ui/item-node item :el_icon_key)
        ^INode input-bg (ui/item-node item :el_input_bg)
        ^INode input-pass (ui/item-node item :el_input_pass)
        ^INode icon-connect (ui/item-node item :el_icon_connect)
        encrypted? (boolean encrypted?)]
    (ui/set-node-prop! rt text-name :text name)
    ;; Upstream hides the password field (and, in our port, its backing box) for
    ;; unencrypted targets; only el_icon_key/el_input_pass were toggled before, so
    ;; the el_input_bg box stayed drawn as a stray dark rectangle on open networks.
    (when input-bg (.setVisible input-bg encrypted?))
    (if encrypted?
      (do (.setVisible icon-key true)
          (.setVisible input-pass true)
          (set-image-alpha! rt icon-key 0.6)
          (ui/set-node-prop! rt input-pass :text "")
          (rt/register-event! rt (.getIdx input-pass) :confirm-input
            (fn [_ _ evt]
              (connect-fn target (:value evt))
              (ui/set-node-prop! rt input-pass :text "")))
          (rt/register-event! rt (.getIdx input-pass) :gain-focus
            (fn [_ _ _] (set-image-alpha! rt icon-key 1.0)))
          (rt/register-event! rt (.getIdx input-pass) :lost-focus
            (fn [_ _ _] (set-image-alpha! rt icon-key 0.6))))
      (do (.setVisible icon-key false)
          (.setVisible input-pass false)))
    (rt/register-event! rt (.getIdx icon-connect) :left-click
      (fn [_ _ _]
        (log/debug "[wireless-reactive] connect icon clicked for" name)
        (let [pwd (if encrypted?
                    (str (or (.getOSlot input-pass 0) ""))
                    "")]
          (connect-fn target pwd)
          (when encrypted?
            (ui/set-node-prop! rt input-pass :text "")))))))

(defn rebuild-page!
  [^UiRt rt {:keys [linked avail connect-fn disconnect-fn name-fn encrypted?-fn]}]
  (let [linked? (some? linked)
        alpha (if linked? 1.0 0.6)
        name (if linked? (name-fn linked)
              (i18n/translate "gui.academy.common.pg_wireless.not_connected"))
        ;; XML-parsed ids are keywords; string lookups fail silently
        ^INode ec-connect (ui/node rt :ec_icon_connect)
        ^INode ec-logo (ui/node rt :ec_icon_logo)
        ^INode ec-name (ui/node rt :ec_text_name)]
    (when ec-name (ui/set-node-prop! rt ec-name :text name))
    ;; Static section headers (upstream XML hardcodes English; localize)
    (when-let [^INode connected (ui/node rt :text_connected)]
      (ui/set-node-prop! rt connected :text
        (i18n/translate "gui.academy.common.pg_wireless.connected")))
    (when-let [^INode available (ui/node rt :text_available)]
      (ui/set-node-prop! rt available :text
        (i18n/translate "gui.academy.common.pg_wireless.available")))
    (when ec-connect
      (ui/set-node-prop! rt ec-connect :src (if linked? tex-connected tex-unconnected))
      (set-image-alpha! rt ec-connect alpha)
      (rt/register-event! rt (.getIdx ec-connect) :left-click
        (fn [_ _ _]
          (when linked? (disconnect-fn linked)))))
    (when ec-logo (set-image-alpha! rt ec-logo alpha))
    ;; Compact spacing between available network rows (default 4.0; must be >0 for pos?)
    (when-let [^INode list-node (rt/node-by-id rt :zone_elementlist)]
      (.setDSlot list-node 0 0.01))
    (ui/list-set! rt :zone_elementlist avail
      (fn [r item target]
        (wire-avail-row! r item target (name-fn target) connect-fn (encrypted?-fn target))))
    (when-let [^ISigL cnt (rt/user-signal rt :wireless-avail-count)]
      (sig/sset-l! cnt (long (count avail))))
    nil))

(defn setup-panel-logo!
  [^UiRt rt {:keys [logo-path logo-breathe?]} override-path]
  (when-let [^INode logo (ui/node rt :icon_logo)]
    (let [path (or override-path (when logo-path (modid/namespaced-path logo-path)))]
      (when path (ui/set-node-prop! rt logo :src path)))
    (when logo-breathe?
      (log/debug "[wireless-reactive] breathe effect not yet ported; static logo only"))))

(defn set-connected-row-logo!
  [^UiRt rt connected-row-logo-path]
  (when connected-row-logo-path
    (when-let [^INode row-logo (ui/node rt :ec_icon_logo)]
      (ui/set-node-prop! rt row-logo :src connected-row-logo-path))))

(defn attach-scroll-buttons!
  [^UiRt rt]
  (let [scroll (or (rt/user-signal rt :wireless-scroll) (sig/signal-d 0.0))
        avail-count (or (rt/user-signal rt :wireless-avail-count) (sig/signal-l 0))
        scroll-px (sig/computed-d [scroll avail-count]
                    (fn [_ _]
                      (let [n (sig/sget-l avail-count)
                            max-scroll (max 0.0 (- (* n row-h) visible-h))]
                        (* (sig/sget-d scroll) max-scroll))))]
    (rt/put-user-signal! rt :wireless-scroll scroll)
    (rt/put-user-signal! rt :wireless-avail-count avail-count)
    (ui/bind! rt :zone_elementlist :scroll-offset scroll-px)
    (events/on! rt :btn_arrowup :left-click
      (fn [_ _ _]
        (sig/sset-d! scroll (max 0.0 (- (sig/sget-d scroll) 0.125)))))
    (events/on! rt :btn_arrowdown :left-click
      (fn [_ _ _]
        (sig/sset-d! scroll (min 1.0 (+ (sig/sget-d scroll) 0.125)))))))
