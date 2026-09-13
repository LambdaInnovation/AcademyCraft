(ns cn.li.ac.test.support.network
  "Shared network mock helpers for ac tests.")

(defn capture-send-to-server!
  "Return a send-to-server mock fn that records calls in `sent-atom`.
  Supports 2/3/4-arg mcmod.network.client/send-to-server arities."
  [sent-atom]
  (fn
    ([msg-id payload]
     (swap! sent-atom conj {:msg-id msg-id :payload payload})
     nil)
    ([msg-id payload callback]
     (swap! sent-atom conj {:msg-id msg-id :payload payload :callback callback})
     nil)
    ([_owner msg-id payload callback]
     (swap! sent-atom conj {:msg-id msg-id :payload payload :callback callback})
     nil)))

(defn open-tile-mock
  "Return a machine-handlers/open-container-tile mock that always resolves `tile`."
  [tile]
  (fn [_payload _player] tile))

(defn require-open-container-mock
  "Return a sync-routing/require-open-container! mock resolving `tile`."
  ([tile]
   (require-open-container-mock tile nil))
  ([tile extra]
   (fn [_payload _player]
     (cond-> {:tile-entity tile}
       extra (merge extra)))))

(defn sent-without-callbacks
  "Drop optional :callback entries from captured send-to-server calls."
  [sent-coll]
  (mapv #(dissoc % :callback) sent-coll))
