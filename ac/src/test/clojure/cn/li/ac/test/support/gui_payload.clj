(ns cn.li.ac.test.support.gui-payload
  "Shared GUI C2S payload builders for handler tests.")

(defn machine-payload
  ([container-id]
   (machine-payload container-id {}))
  ([container-id {:keys [pos-x pos-y pos-z]
                  :or {pos-x 1 pos-y 2 pos-z 3}}]
   {:container-id (int container-id)
    :pos-x pos-x
    :pos-y pos-y
    :pos-z pos-z}))

(defn with-pos
  [payload pos]
  (merge payload pos))
