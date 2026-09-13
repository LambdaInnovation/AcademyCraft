(ns cn.li.ac.ability.spi-lifecycle-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.spi-lifecycle :as spi]))

(defn- clean-fixture [f]
  (spi/install-lifecycle-registry-runtime!
    (spi/create-lifecycle-registry-runtime))
  (spi/reset-lifecycle-registry-for-test!)
  (try
    (f)
    (finally
      (spi/reset-lifecycle-registry-for-test!))))

(use-fixtures :each clean-fixture)


