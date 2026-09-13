(ns cn.li.ac.test.support.player-state
  (:require [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(def test-player-state-owner {:server-session-id :test-session
                              :player-uuid "test-player"})

(def test-session-id
  (:server-session-id test-player-state-owner))

(defn with-framework [f]
  (let [prev-fw fw/framework]
    (try
      (when-let [fw-inst (or (fw/create-framework)
                             (when-not prev-fw
                               (atom {:registry {:blocks {} :items {} :entities {} :fluids {}
                                                 :effects {} :sounds {} :particles {} :loot {}
                                                 :configs {} :guis {} :slots {} :tiles {}
                                                 :tile-kinds {} :hooks {} :handlers {}
                                                 :commands {} :energy {} :providers {}
                                                 :keybinds {} :messages {} :integrations {}}
                                      :service {:lifecycle {:content-init-fns []
                                                            :runtime-content-activation-fns []
                                                            :world-tick-fns []
                                                            :datagen-metadata-init-fns []
                                                            :client-init-fns []
                                                            :post-spi-client-init-fns []}}
                                      :platform {}})))]
        (alter-var-root #'fw/framework (constantly fw-inst))
        (f))
      (finally
        (alter-var-root #'fw/framework (constantly prev-fw))))))

(defn with-test-player-state-owner
  [f]
  (with-framework
    (fn []
      (runtime-hooks/with-player-state-owner-fn test-player-state-owner f))))

(defn clean-player-states-fixture
  [f]
  (with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (try
        (f)
        (finally
          (store/reset-store!))))))

(defn seed-player-state!
  [uuid state]
  (store/set-player-state! test-session-id uuid state))
