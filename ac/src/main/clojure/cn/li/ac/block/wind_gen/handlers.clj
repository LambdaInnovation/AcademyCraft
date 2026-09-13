(ns cn.li.ac.block.wind-gen.handlers
  "Wind Generator network handlers.

  Wind Generator reuses the :generator message domain handlers registered by
  Solar Generator. The link!/unlink!/get-linked-node logic is tile-generic:
  `get-node-conn-by-generator` works for any IWirelessGenerator tile.")

(defn register-network-handlers! []
  ;; :generator domain handlers already registered by solar-gen.
  ;; Wind-gen GUI uses (wireless-tab/create-wireless-panel {:role :generator})
  ;; which sends :generator messages → handled by solar-gen's registered handlers.
  )
