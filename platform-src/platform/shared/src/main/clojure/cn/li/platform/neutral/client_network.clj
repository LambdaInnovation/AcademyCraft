(ns cn.li.platform.neutral.client-network
  "Installed client-network callbacks for AOT/remapped packet code.")

(defn- unavailable [operation]
  (throw (IllegalStateException. (str "Client network provider is unavailable: " operation))))

(defn register-request-transport! [& _] (unavailable :register-request-transport!))
(defn send-to-server [& _] (unavailable :send-to-server))
(defn clear-client-session-state! [& _] (unavailable :clear-client-session-state!))
(defn handle-push [& _] (unavailable :handle-push))
(defn handle-response [& _] (unavailable :handle-response))

(def ^:private operation-vars
  {:register-request-transport! #'register-request-transport!
   :send-to-server #'send-to-server
   :clear-client-session-state! #'clear-client-session-state!
   :handle-push #'handle-push
   :handle-response #'handle-response})

(defn install! [operations]
  (let [expected (set (keys operation-vars))]
    (when (or (not= expected (set (keys operations)))
              (some (complement ifn?) (vals operations)))
      (throw (ex-info "Client network provider contract mismatch"
                      {:expected (sort expected) :actual (sort (keys operations))})))
    (doseq [[operation target-var] operation-vars]
      (alter-var-root target-var (constantly (get operations operation)))))
  nil)
