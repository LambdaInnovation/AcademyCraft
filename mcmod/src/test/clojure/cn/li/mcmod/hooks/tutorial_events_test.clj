(ns cn.li.mcmod.hooks.tutorial-events-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.hooks.tutorial-events :as sut]))

(defn- with-framework [f]
  (let [prev-fw fw/framework]
    (try
      (when-let [fw-inst (fw/create-framework)]
        (alter-var-root #'fw/framework (constantly fw-inst))
        (sut/reset-tutorial-events-for-test!)
        (f))
      (finally
        (sut/reset-tutorial-events-for-test!)
        (alter-var-root #'fw/framework (constantly prev-fw))))))

(use-fixtures :each with-framework)

(deftest content-can-claim-unset-tutorial-handler-test
  (let [calls (atom [])
        on-item! (fn [_ _ _] (swap! calls conj :item))]
    (is (nil? (sut/register-tutorial-handlers! {:on-item-event! on-item!})))
    (sut/on-item-event! nil "demo" :craft)
    (is (= [:item] @calls))))

(deftest duplicate-tutorial-registration-conflicts-test
  (sut/register-tutorial-handlers! {:on-item-event! (fn [_ _ _] :first)})
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Conflicting tutorial handler"
        (sut/register-tutorial-handlers! {:on-item-event! (fn [_ _ _] :second)}))))
