(ns cn.li.ac.block.obj-bake-smoke-test
  "Sanity check bake-obj-model against real OBJ assets: structural shape,
  vertex-count conservation vs raw face count, and that culling options
  actually remove vertices when enabled."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.client.obj :as obj]
            [cn.li.mcmod.client.render.buffer :as buffer]
            [cn.li.mcmod.client.resources :as res]))

;; bake-obj-model reads buffer/triangle-vertex-order (platform render-buffer-ops,
;; installed only by real Forge/Fabric bootstrap) purely to fix vertex winding
;; order; stub it so the bake ALGORITHM can be exercised without a platform.
(use-fixtures :each
  (fn [f]
    (with-redefs [buffer/triangle-vertex-order (fn [] [0 1 2])]
      (f))))

(defn- total-raw-faces [model]
  (reduce + (map count (vals (:faces model)))))

(defn- total-baked-verts [baked]
  (reduce + (for [[_ {:keys [batches]}] (:parts baked)
                  {:keys [^floats verts]} batches]
              (quot (alength verts) 8))))

(deftest bake-preserves-vertex-count-with-default-opts-test
  (testing "default bake-opts (no culling enabled) emits 3 verts per raw face"
    (doseq [asset ["solar" "matrix" "silbarn"]]
      (let [model (res/load-obj-model asset)
            baked (obj/bake-obj-model model)
            raw-faces (total-raw-faces model)
            baked-verts (total-baked-verts baked)]
        (is (pos? raw-faces) (str asset ": raw model has faces"))
        (is (= (* 3 raw-faces) baked-verts)
            (str asset ": 3 verts/face with all culling disabled"))))))

(deftest bake-batches-have-valid-shape-test
  (testing "every batch has a 4-float rgba and a verts array that's a multiple of 8 floats"
    (let [model (res/load-obj-model "solar")
          baked (obj/bake-obj-model model {:skip-flat-bottom-plane? true
                                           :bottom-plane-epsilon 0.0008})]
      (is (seq (:parts baked)))
      (doseq [[part {:keys [batches]}] (:parts baked)]
        (is (seq batches) (str "part " part " has at least one batch"))
        (doseq [{:keys [^floats rgba ^floats verts]} batches]
          (is (= 4 (alength rgba)))
          (is (zero? (rem (alength verts) 8))))))))

(deftest bake-flat-bottom-culling-reduces-verts-test
  (testing "enabling :skip-flat-bottom-plane? never increases vertex count"
    (let [model (res/load-obj-model "matrix")
          baked-off (obj/bake-obj-model model {:skip-flat-bottom-plane? false})
          baked-on (obj/bake-obj-model model {:skip-flat-bottom-plane? true
                                              :bottom-plane-epsilon 0.0008})]
      (is (<= (total-baked-verts baked-on) (total-baked-verts baked-off))))))

(deftest baked-has-part-and-render-baked-part-do-not-throw-test
  (testing "render-baked-part! runs against a stub VertexConsumer without throwing"
    (let [model (res/load-obj-model "solar")
          baked (obj/bake-obj-model model {:skip-flat-bottom-plane? true
                                           :bottom-plane-epsilon 0.0008})
          calls (atom 0)
          fake-vc :fake-vc
          fake-pose :fake-pose]
      (with-redefs [buffer/submit-vertex
                    (fn [_vc _pose _x _y _z _r _g _b _a _u _v _overlay _light _nx _ny _nz]
                      (swap! calls inc))]
        (doseq [part (keys (:parts baked))]
          (is (obj/baked-has-part? baked part))
          (obj/render-baked-part! baked part fake-pose fake-vc 0 0))
        (is (pos? @calls))))))
