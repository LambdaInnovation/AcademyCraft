(ns cn.li.mcbase.gui.init.checks
  "Shared helpers for GUI initialization verification.")

(defn build-gui-checks
  [gui-ids key-prefix check-fn]
  (into {}
        (for [gui-id gui-ids]
          (let [check-key (keyword (str key-prefix gui-id))]
            [check-key (boolean (check-fn gui-id))]))))
