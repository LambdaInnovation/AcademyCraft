(ns cn.li.mcmod.entity.dsl-render-key-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.entity.dsl :as dsl]))

(deftest resolve-render-profile-key-precedence-test
  (testing "explicit :profile-key wins over :renderer-id"
    (let [spec {:properties {:effect {:profile-key "effect/v2"
                                      :renderer-id "effect/v1"}}}]
      (is (= "effect/v2"
             (dsl/resolve-render-profile-key spec :effect "effect-billboard")))))

  (testing ":renderer-id is used when :profile-key is absent"
    (let [spec {:properties {:marker {:renderer-id "marker-custom"}}}]
      (is (= "marker-custom"
             (dsl/resolve-render-profile-key spec :marker "marker-billboard")))))

  (testing "blank values fall back to default"
    (let [spec {:properties {:ray {:profile-key ""
                                   :renderer-id "   "}}}]
      (is (= "ray-composite"
             (dsl/resolve-render-profile-key spec :ray "ray-composite"))))))
