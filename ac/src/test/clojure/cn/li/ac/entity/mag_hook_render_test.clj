(ns cn.li.ac.entity.mag-hook-render-test
  "Upstream RendererMagHook picks maghook_open.obj once EntityMagHook.isHit is
  set and maghook.obj before that. The port drew a flat thrown-item sprite that
  never changed, so the hook never appeared to open after anchoring."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.entity.mag-hook-render :as hook-render]
            [cn.li.mcmod.client.obj :as obj]
            [cn.li.mcmod.client.render.buffer :as rb]
            [cn.li.mcmod.client.render.pose :as pose]
            [cn.li.mcmod.client.resources :as res]))

(defn- render-with [hit?]
  (let [drawn (atom nil)
        rotations (atom [])]
    (with-redefs [res/load-obj-model (fn [name] {::model name})
                  res/texture-location (fn [loc] {::texture loc})
                  obj/bake-obj-model (fn [raw _] {::baked (::model raw)})
                  rb/get-solid-buffer (fn [_ _] ::vertex-consumer)
                  pose/push-pose (fn [_] nil)
                  pose/pop-pose (fn [_] nil)
                  pose/scale (fn [& _] nil)
                  pose/apply-y-rotation (fn [_ deg] (swap! rotations conj [:y deg]) nil)
                  pose/apply-z-rotation (fn [_ deg] (swap! rotations conj [:z deg]) nil)
                  obj/render-baked-all! (fn [model & _] (reset! drawn (::baked model)))]
      (hook-render/render! 1 hit? 5 30.0 20.0 0.5 ::pose ::buffer 15728880 0)
      {:model @drawn :rotations @rotations})))

(deftest anchored-hook-draws-the-opened-model-test
  (is (= "maghook" (:model (render-with false))) "in flight")
  (is (= "maghook_open" (:model (render-with true))) "after it bites"))

(deftest orientation-matches-upstream-test
  ;; glRotated(-yaw + 90, 0,1,0) then glRotated(pitch - 90, 0,0,1)
  (is (= [[:y 60.0] [:z -70.0]] (:rotations (render-with false)))))
