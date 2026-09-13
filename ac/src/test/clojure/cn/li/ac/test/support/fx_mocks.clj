(ns cn.li.ac.test.support.fx-mocks
  "Shared capture helpers for ability FX unit tests.")

(defn capture-fx-send!
  "Returns `{:calls* atom :send! fn}` suitable for `(with-redefs [fx/send! send!] ...)`.

  Each captured entry is `[ctx-id topic mode payload]`."
  []
  (let [calls* (atom [])]
    {:calls* calls*
     :send! (fn
              ([ctx-id entry _evt]
               (swap! calls* conj [ctx-id (:topic entry) (:mode entry) nil])
               nil)
              ([ctx-id entry _evt payload]
               (swap! calls* conj [ctx-id (:topic entry) (:mode entry) payload])
               nil))}))

(defn capture-fx-topics!
  "Returns `{:topics* atom :send! fn}` recording only topics."
  []
  (let [topics* (atom [])]
    {:topics* topics*
     :send! (fn [_ctx-id entry _evt _payload]
              (swap! topics* conj (:topic entry))
              nil)}))

(defn capture-ctx-fx-messages!
  "Legacy-shaped capture for tests that expect `{:ctx-id :channel :payload}` maps.

  `fx/send!` stores `{:ctx-id :topic :payload}` where `:topic` replaces `:channel`."
  []
  (let [messages* (atom [])]
    {:messages* messages*
     :send! (fn [ctx-id entry _evt payload]
              (swap! messages* conj {:ctx-id ctx-id
                                     :topic (:topic entry)
                                     :payload payload})
              nil)}))
