(ns cn.li.ac.item.media
  "Media items - information storage devices for Wireless Matrix

  Media items store information/music that can be transmitted through wireless network.
  Right-clicking one unlocks the matching internal track in the terminal's
  Media Player app (see cn.li.ac.media.acquire) — matching upstream's
  MediaAcquireData.install() flow, minus a physical MediaItem class since
  these items already exist 1:1 with the 3 catalog ids."
  (:require [cn.li.ac.media.acquire :as acquire]
            [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn- media-unlock-handler
  [media-id]
  (fn [{:keys [player side]}]
    (when (= side :server)
      (acquire/acquire! player media-id))
    {:consume? true}))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init-media! []
  (install/framework-once! ::media-installed?
  (fn []
    (idsl/register-item!
      (idsl/create-item-spec
        "media_0"
        {:max-stack-size 1
         :creative-tab :misc
         :properties {:display-name "Sisters' Noise"
                      :tooltip ["音乐: 妹妹的噪音 (Sisters\\'s Noise)"
                                "来自某某动画的BGM"]
                      :model-texture "media_sisters_noise"}
         :on-right-click (media-unlock-handler :media_0)}))
    (idsl/register-item!
      (idsl/create-item-spec
        "media_1"
        {:max-stack-size 1
         :creative-tab :misc
         :properties {:display-name "Only My Railgun"
                      :tooltip ["音乐: 仅有我的超电磁炮 (Only My Railgun)"
                                "来自某某动画的OP曲"]
                      :model-texture "media_only_my_railgun"}
         :on-right-click (media-unlock-handler :media_1)}))
    (idsl/register-item!
      (idsl/create-item-spec
        "media_2"
        {:max-stack-size 1
         :creative-tab :misc
         :properties {:display-name "Level5 Judgelight"
                      :tooltip ["音乐: 5级审判之光 (Level5 Judgelight)"
                                "来自某某动画的ED曲"]
                      :model-texture "media_level5_judgelight"}
         :on-right-click (media-unlock-handler :media_2)}))
    (log/debug "Media items initialized: media-0, media-1, media-2"))))
