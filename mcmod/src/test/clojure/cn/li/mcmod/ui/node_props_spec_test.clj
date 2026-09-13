(ns cn.li.mcmod.ui.node-props-spec-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.ui.node-props-spec :as props-spec]))

(deftest layout-props-accepts-id-in-props
  (is (= {:id :root :x 0.0 :y 0.0}
         (props-spec/validate-layout-props! {:id :root :x 0.0 :y 0.0}))))

(deftest layout-props-valid
  (is (= {:x 1.0 :y 2.0}
         (props-spec/validate-layout-props! {:x 1.0 :y 2.0}))))

(deftest layout-props-rejects-bad-align
  (is (thrown? clojure.lang.ExceptionInfo
               (props-spec/validate-layout-props! {:align-w :bogus}))))

(deftest kind-props-cases
  (doseq [[kind good bad]
          [[:image {:src "academy:textures/a.png" :alpha 0.5} {:src 123}]
           [:text {:text "hi" :color 0xFFFFFFFF :font-size 12.0} {:color "red"}]
           [:list {:spacing 4.0 :template {:kind :box :props {:w 10 :h 10}}} {:spacing "x"}]
           [:box {:fill 0.5 :outline 1.0} {:fill "x"}]
           [:line {:x1 0 :y1 0 :x2 10 :y2 10 :color 0xFF0000FF} {:alpha "x"}]]]
    (testing (str kind " valid")
      (is (= good (props-spec/validate-kind-props! kind good))))
    (testing (str kind " invalid")
      (is (thrown? clojure.lang.ExceptionInfo
                   (props-spec/validate-kind-props! kind bad))))))

(deftest single-prop-validation
  (is (= "hello" (props-spec/validate-kind-prop! :text :text "hello")))
  (is (thrown? clojure.lang.ExceptionInfo
               (props-spec/validate-kind-prop! :text :color "not-a-number"))))
