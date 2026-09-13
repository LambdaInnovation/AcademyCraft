(ns cn.li.ac.block.developer.panel-cover-layer-test
  "Layering regression: the developer panel's modal dim (upstream blackCover)
   must darken the skill-tree embed too.

   The host screen paints its own tape first and the embedded runtimes after
   it, in insertion order. While the dim was a filled box on the host tape it
   therefore drew *under* the skill-tree embed: every part of the developer UI
   went dark except the tree, and the detail popup drawn on top of the bright
   tree was unreadable. The dim now lives in its own embedded runtime inserted
   between the page embeds and the overlay embed."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.developer.panel-reactive :as panel]
            [cn.li.mcmod.ui.runtime :as rt])
  (:import [cn.li.mcmod.uipojo.signal Binding]
           [cn.li.mcmod.uipojo.runtime UiRt]))

(def ^:private begin-cover! #'panel/begin-cover!)
(def ^:private add-embedded-runtime! #'panel/add-embedded-runtime!)
(def ^:private finish-cover-close! #'panel/finish-cover-close!)

(defn- host-runtime
  "Minimal stand-in for the developer panel host: the cover helpers only need
   :root (tick anchor) and :dev-cover (click catcher / dim anchor)."
  ^UiRt []
  (let [r (rt/create-runtime)]
    (rt/build! r {:kind :group
                  :props {:id :root :x 0.0 :y 0.0 :w 400.0 :h 187.0}
                  :children [{:kind :box
                              :props {:id :dev-cover :x 0.0 :y 0.0
                                      :w 400.0 :h 187.0 :fill 0x00000000}}]})
    (rt/resize! r 640.0 480.0)
    r))

(defn- entries [^UiRt rt]
  (some-> (rt/user-signal rt :embedded-runtimes) deref))

(deftest cover-draws-between-page-embeds-and-overlay-test
  (let [rt (host-runtime)
        tree-rt (rt/create-runtime)
        popup-rt (rt/create-runtime)]
    (try
      ;; The skill-tree area embed goes in on mode switch, before any modal.
      (add-embedded-runtime! rt {:child-rt tree-rt :x 0.0 :y 0.0 :w 400.0 :h 187.0
                                 :visible?-fn nil})
      (begin-cover! rt)
      ;; ...then the overlay opener adds the popup, as open-skill-detail-overlay! does.
      (add-embedded-runtime! rt {:child-rt popup-rt :x 0.0 :y 0.0 :w 400.0 :h 187.0
                                 :visible?-fn nil :overlay? true})
      (let [es (entries rt)]
        (testing "one dim, sitting above the tree and below the popup"
          (is (= 3 (count es)))
          (is (= [tree-rt :cover popup-rt]
                 [(:child-rt (nth es 0))
                  (if (:cover? (nth es 1)) :cover (:child-rt (nth es 1)))
                  (:child-rt (nth es 2))])))
        (testing "the dim is tagged :overlay? so close-cover! disposes it with the popup"
          (is (true? (:overlay? (nth es 1))))))
      (finally
        (rt/dispose! tree-rt)
        (rt/dispose! popup-rt)
        (rt/dispose! rt)))))

(deftest cover-fill-is-bound-off-the-host-tape-test
  (let [rt (host-runtime)]
    (try
      (begin-cover! rt)
      (let [^Binding b (rt/user-signal rt :cover-fill-binding)
            cover-entry (first (filter :cover? (entries rt)))
            ^UiRt cover-rt (:child-rt cover-entry)]
        (is (some? b))
        (testing "the animated fill writes into the cover embed, not :dev-cover"
          (is (identical? (rt/node-by-id cover-rt :cover-fill) (.getNode b)))
          (is (not (identical? (rt/node-by-id rt :dev-cover) (.getNode b))))))
      (finally
        (doseq [{:keys [child-rt]} (entries rt)] (rt/dispose! child-rt))
        (rt/dispose! rt)))))

(deftest finish-cover-close-disposes-the-dim-test
  (let [rt (host-runtime)
        tree-rt (rt/create-runtime)]
    (try
      (add-embedded-runtime! rt {:child-rt tree-rt :x 0.0 :y 0.0 :w 400.0 :h 187.0
                                 :visible?-fn nil})
      (begin-cover! rt)
      (finish-cover-close! rt)
      (let [es (entries rt)]
        (testing "the dim goes away with the overlay, the tree embed stays"
          (is (= [tree-rt] (mapv :child-rt es)))))
      (is (nil? (rt/user-signal rt :cover-fill-binding)))
      (finally
        (rt/dispose! tree-rt)
        (rt/dispose! rt)))))

(deftest reopening-does-not-stack-two-dims-test
  (let [rt (host-runtime)]
    (try
      (begin-cover! rt)
      (begin-cover! rt)
      (is (= 1 (count (filter :cover? (entries rt)))))
      (finally
        (doseq [{:keys [child-rt]} (entries rt)] (rt/dispose! child-rt))
        (rt/dispose! rt)))))
