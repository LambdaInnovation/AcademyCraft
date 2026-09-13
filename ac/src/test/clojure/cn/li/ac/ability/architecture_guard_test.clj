(ns cn.li.ac.ability.architecture-guard-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(def ^:private guarded-runtime-install-files
  ["src/main/clojure/cn/li/ac/ability/registry/category.clj"
   "src/main/clojure/cn/li/ac/ability/registry/skill.clj"
   "src/main/clojure/cn/li/ac/ability/registry/event.clj"
   "src/main/clojure/cn/li/ac/ability/spi_lifecycle.clj"
   "src/main/clojure/cn/li/ac/ability/service/context_dispatcher.clj"])

(def ^:private reducer-only-forbidden-patterns
  ["command-runtime-ready?"
   "safe-context-data"
   "ctx/update-context!"
   "sync-register-context-command!"
   "sync-context-status-command!"
   ":command :sync-ability-data"
   ":command :sync-resource-data"
   ":command :sync-cooldown-data"
   ":command :sync-preset-data"
   ":command :sync-develop-data"
   ":command :sync-terminal-data"
   ":command :sync-context-registry"
   ":command :sync-runtime-data"
   ":command :apply-server-tick-postprocess"])

(def ^:private reducer-only-scan-dirs
  ["src/main/clojure/cn/li/ac/ability"
   "src/main/clojure/cn/li/ac/content/ability"])

(def ^:private context-registry-facade-forbidden-patterns
  ["service.context-registry"
   "ctx-reg/"])

(def ^:private skill-runtime-forbidden-patterns
  ["create-damage-helper-runtime"
   "create-vec-reflection-runtime"
   "create-projectile-arbitration-runtime"
   "create-delayed-projectile-runtime"
   "install-damage-helper-runtime"
   "install-vec-reflection-runtime"
   "install-projectile-arbitration-runtime"
   "server-runtime-lifecycle"
   "*damage-helper-runtime*"
   "*vec-reflection-runtime*"
   "*projectile-arbitration-runtime*"
   "*delayed-projectile-runtime*"])

(def ^:private skill-runtime-scan-dirs
  ["src/main/clojure/cn/li/ac/content/ability"
   "src/main/clojure/cn/li/ac/ability/service/delayed_projectiles.clj"])

(def ^:private dead-skill-op-keys
  [":on-down" ":on-tick" ":on-up"])

(defn- project-file
  [rel-path]
  (io/file (System/getProperty "user.dir") rel-path))

(defn- fx-file?
  [file]
  (str/ends-with? (.getName file) "_fx.clj"))

(defn- scan-entry-sources
  [rel-entries file-pred]
  (let [bases [(project-file ".")
               (project-file "..")]]
    (mapcat (fn [rel-entry]
              (mapcat (fn [base]
                        (let [root (io/file base rel-entry)]
                          (cond
                            (and (.exists root) (.isFile root))
                            (when (and (str/ends-with? (.getName root) ".clj")
                                       (file-pred root))
                              [root])

                            (and (.exists root) (.isDirectory root))
                            (->> (file-seq root)
                                 (filter #(.isFile %))
                                 (filter #(str/ends-with? (.getName %) ".clj"))
                                 (filter #(not (str/includes? (.getPath %) "test")))
                                 (filter file-pred)))))

                      bases))
            rel-entries)))

(defn- scan-sources
  [rel-dirs]
  (scan-entry-sources rel-dirs (constantly true)))

(deftest runtime-installation-no-alter-var-root-guard-test
  (doseq [rel-path guarded-runtime-install-files]
    (let [file (project-file rel-path)]
      (is (.exists file) (str "Missing guarded file: " rel-path))
      (let [source (slurp file)]
        (is (not (str/includes? source "alter-var-root"))
            (str "Forbidden alter-var-root found in guarded runtime file: " rel-path))))))

(deftest no-deleted-context-registry-facade-guard-test
  (let [violations
        (for [file (scan-sources reducer-only-scan-dirs)
              :let [rel (.getPath file)
                    source (slurp file)]
              line (str/split-lines source)
              [idx text] (map vector (range) line)
              pattern context-registry-facade-forbidden-patterns
              :when (str/includes? text pattern)]
          (str rel ":" (inc idx) ": " (str/trim text)))]
    (is (empty? violations)
        (str "Deleted context-registry facade references:\n"
             (str/join "\n" violations)))))

(deftest reducer-only-no-legacy-context-mutation-guard-test
  (let [violations
        (for [file (scan-sources reducer-only-scan-dirs)
              :let [rel (.getPath file)
                    source (slurp file)]
              line (str/split-lines source)
              [idx text] (map vector (range) line)
              pattern reducer-only-forbidden-patterns
              :when (str/includes? text pattern)]
          (str rel ":" (inc idx) ": " (str/trim text)))]
    (is (empty? violations)
        (str "Reducer-only guard violations:\n"
             (str/join "\n" violations)))))

(deftest no-content-skill-runtime-bundle-residue-guard-test
  (let [violations
        (for [file (scan-entry-sources skill-runtime-scan-dirs #(not (fx-file? %)))
              :let [rel (.getPath file)
                    source (slurp file)]
              line (str/split-lines source)
              [idx text] (map vector (range) line)
              pattern skill-runtime-forbidden-patterns
              :when (str/includes? text pattern)]
          (str rel ":" (inc idx) ": " (str/trim text)))]
    (is (empty? violations)
        (str "Content skill runtime bundle residues:\n"
             (str/join "\n" violations)))))

(def ^:private platform-impl-var-pattern
  #"cn\.li\.mcmod\.platform\.[a-z0-9-]+/\*[a-z0-9-]+\*")

(deftest no-direct-platform-impl-var-access-guard-test
  (let [violations
        (for [file (scan-entry-sources ["src/main/clojure/cn/li/ac"]
                                        #(not (fx-file? %)))
              :let [rel (.getPath file)
                    source (slurp file)]
              line (str/split-lines source)
              [idx text] (map vector (range) line)
              :when (and (string? text)
                         (re-find platform-impl-var-pattern text))]
          (str rel ":" (inc idx) ": " (str/trim text)))]
    (is (empty? violations)
        (str "ac must use platform *-wrappers, not impl vars:\n"
             (str/join "\n" violations)))))

(deftest no-dead-skill-op-vector-keys-guard-test
  (let [violations
        (for [file (scan-entry-sources ["src/main/clojure/cn/li/ac/content/ability"]
                                        #(not (fx-file? %)))
              :let [rel (.getPath file)
                    source (slurp file)]
              line (str/split-lines source)
              [idx text] (map vector (range) line)
              key dead-skill-op-keys
              :when (and (string? text) (str/includes? text key))]
          (str rel ":" (inc idx) ": " (str/trim text)))]
    (is (empty? violations)
        (str "Dead :on-down/:on-tick/:on-up skill op vectors:\n"
             (str/join "\n" violations)))))

(def ^:private legacy-session-id-owner-allowlist
  #{"src/main/clojure/cn/li/ac/ability/service/command_runtime.clj"
    "src/main/clojure/cn/li/ac/ability/effects/interpreter.clj"})

(def ^:private legacy-session-id-owner-patterns
  [":session-id [" "{:session-id" "(assoc :session-id" "(:session-id "
   "clear-session-contexts!"])

(deftest no-legacy-session-id-owner-guard-test
  (let [violations
        (for [file (scan-sources ["src/main/clojure/cn/li/ac/ability"])
              :let [rel (.getPath file)
                    norm (str/replace rel "\\" "/")]
              :when (not (contains? legacy-session-id-owner-allowlist norm))
              :let [source (slurp file)]
              line (str/split-lines source)
              [idx text] (map vector (range) line)
              pattern legacy-session-id-owner-patterns
              :when (and (string? text)
                         (str/includes? text pattern)
                         (not (str/includes? text "transport-session-id"))
                         (not (str/includes? text "store-session-id")))]
          (str norm ":" (inc idx) ": " (str/trim text)))]
    (is (empty? violations)
        (str "Legacy :session-id owner usage (use mcmod.runtime.owner):\n"
             (str/join "\n" violations)))))
