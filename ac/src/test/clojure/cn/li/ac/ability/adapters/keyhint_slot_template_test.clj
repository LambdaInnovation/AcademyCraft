(ns cn.li.ac.ability.adapters.keyhint-slot-template-test
  "Upstream parity for the battle-HUD skill slots (KeyHintUI.drawSingle).

   The slot list draws at KeyHintUI.SCALE = 0.23, and render-text! multiplies
   :font-size by the node's cum-scale exactly like x/y/w/h. Font sizes here are
   therefore raw design units; pre-multiplying them by the scale applied 0.23
   twice and drew the key character at ~1.6px (a white smear, not a letter)
   with an even smaller cooldown readout."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.adapters.reactive-overlay :as overlay]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.dsl :as dsl]
            [cn.li.mcmod.ui.layout :as layout]
            [cn.li.mcmod.ui.runtime :as rt]))

(def ^:private skill-slot-template @#'overlay/skill-slot-template)
(def ^:private slot-scale @#'overlay/skill-slot-scale)
(def ^:private key-label-color @#'overlay/key-label-color)
(def ^:private key-label-color-dim @#'overlay/key-label-color-dim)

(defn- node [id]
  (->> (:children (skill-slot-template))
       (filter #(= id (:id (:props %))))
       first
       :props))

(defn- screen-font-size [id]
  (* (double (:font-size (node id))) (double slot-scale)))

(deftest key-label-matches-upstream-font-option-test
  (let [{:keys [font-size color x y align]} (node :key-label)]
    (testing "upstream FontOption(32, CENTER, 0xff194246) drawn at (180, 27)"
      (is (= 32.0 (double font-size)))
      (is (= :center align))
      (is (= 180 x))
      (is (= 27 y)))
    (testing "the key character is upstream's dark teal, never white"
      (is (= 0xFF194246 color))
      (is (= key-label-color color)))))

(deftest dim-key-label-is-the-mono-average-of-the-base-colour-test
  ;; Upstream leaves ShaderMono bound (c = (r+g+b)/3) while the skill is on
  ;; cooldown or the player can't use abilities.
  (let [r (bit-and (bit-shift-right key-label-color 16) 0xFF)
        g (bit-and (bit-shift-right key-label-color 8) 0xFF)
        b (bit-and key-label-color 0xFF)
        ;; mono.frag averages in float and the result is rounded back to a
        ;; byte on write, so 161/3 = 53.67 lands on 54, not 53.
        mono (Math/round (/ (+ r g b) 3.0))]
    (is (= [mono mono mono]
           [(bit-and (bit-shift-right key-label-color-dim 16) 0xFF)
            (bit-and (bit-shift-right key-label-color-dim 8) 0xFF)
            (bit-and key-label-color-dim 0xFF)]))))

(deftest slot-text-is-legible-on-screen-test
  (testing "the key character renders at upstream's 32 units × SCALE"
    (is (< 7.0 (screen-font-size :key-label) 7.5)))
  (testing "the cooldown readout stays in the same legible range"
    ;; Guards the double-scale regression: 7 design units × 0.23 was 1.6px.
    (is (< 6.0 (screen-font-size :cd-text) 7.5))))

(deftest slot-text-inherits-the-list-scale-test
  ;; The premise of the sizes above: a template text node ends up at the list's
  ;; cum-scale, which render-text! multiplies :font-size by. If that ever stops
  ;; being true the sizes here are wrong by 4.3x in one direction or the other.
  (let [r (rt/create-runtime)]
    (try
      (rt/build! r (dsl/group {:id :root :x 0.0 :y 0.0 :w 400.0 :h 400.0}
                     (dsl/list-node {:id :slots :spacing 0 :w 320 :h 400
                                     :scale slot-scale
                                     :template (skill-slot-template)})))
      (ui/list-set! r :slots [{:idx 0}] (fn [_ _ _] nil))
      (layout/ensure-layout! r)
      (let [item (.getChild (ui/node r :slots) 0)
            ^cn.li.mcmod.ui.node.INode label (ui/item-node item :key-label)]
        (is (= (double slot-scale) (.getCumScale label))))
      (finally
        (rt/dispose! r)))))

(deftest cooldown-readout-sits-on-the-wipe-test
  ;; Upstream draws no number; the port centres it on the icon box that the
  ;; cooldown wipe darkens — colorRect(221, .., 62, ..) — rather than sharing
  ;; the key cap with the key character.
  (let [{:keys [x y font-size align]} (node :cd-text)
        icon (node :icon)]
    (is (= :center align))
    (is (= (+ (double (:x icon)) (/ (double (:w icon)) 2.0)) (double x)))
    (testing "vertically centred in the icon box"
      (is (= (+ (double (:y icon)) (/ (- (double (:h icon)) (double font-size)) 2.0))
             (double y))))
    (testing "and fits inside it"
      (is (<= (double font-size) (double (:h icon)))))))
