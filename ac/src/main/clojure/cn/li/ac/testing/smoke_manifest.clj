(ns cn.li.ac.testing.smoke-manifest
  "AC-owned smoke manifest data registered through mcmod's neutral registry."
  (:require [cn.li.mcmod.content.registry :as content-registry]))

(def ^:private manifest-id :cn.li.ac/content-smoke)

(defn register!
  []
  (content-registry/register-smoke-manifest!
   {:id manifest-id
    :content-id "ac"
    :checks [{:id :registry-present
              :kind :registry}
             {:id :client-surface-present
              :kind :client}
             {:id :persistence-present
              :kind :persistence}]
    :fixtures {:content-owned? true
               :electron-bomb-defaults
               {:settle-delay-ticks 15
                :combat-damage [6.0 12.0]}
               :scatter-bomb-defaults
               {:max-hold-ticks 80
                :anti-afk-tick 200
                :anti-afk-damage 6.0
                :spawn-start-tick 20
                :spawn-interval-ticks 10
                :max-balls 6
                :settle-delay-ticks 15}}})
  nil)