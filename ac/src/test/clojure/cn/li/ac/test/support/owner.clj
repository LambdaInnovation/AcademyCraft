(ns cn.li.ac.test.support.owner
  "Canonical runtime owners for ability tests.")

(defn server-owner
  ([server-session-id]
   (server-owner server-session-id "test-player"))
  ([server-session-id player-uuid]
   {:logical-side :server
    :server-session-id server-session-id
    :player-uuid (str player-uuid)}))

(defn client-owner
  ([client-session-id]
   (client-owner client-session-id "test-player"))
  ([client-session-id player-uuid]
   {:logical-side :client
    :client-session-id client-session-id
    :player-uuid (str player-uuid)}))

(def default-server-session-id :test-server-session)
(def default-client-session-id :test-client-session)

(defn default-server-owner
  ([] (server-owner default-server-session-id))
  ([player-uuid] (server-owner default-server-session-id player-uuid)))

(defn default-client-owner
  ([] (client-owner default-client-session-id))
  ([player-uuid] (client-owner default-client-session-id player-uuid)))
