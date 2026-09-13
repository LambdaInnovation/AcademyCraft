(ns aot-safety
  "Static linter: top-level Clojure forms must not touch Minecraft registry/bootstrap state.
  See docs/dev/AOT_BOOTSTRAP.md and docs/dev/aot-linter-allowlist.edn."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]))

(def ^:private default-source-dirs
  ["ac/src/main/clojure"
   "mcmod/src/main/clojure"
   "platform-src/common/src/main/clojure"
   "platform-src/minecraft/version/mc-1201/src/main/clojure"
   "platform-src/loader/forge-1.20.1/src/main/clojure"
   "platform-src/loader/fabric-1.20.1/src/main/clojure"])

(def ^:private forbidden-symbols
  ["Blocks/" "Items/" "BuiltInRegistries/" "ForgeRegistries/"
   "DeferredRegister" "create-blocks-register" "create-items-register"
   "create-creative-tabs-register" "create-block-entity-types-register"
   "create-fluid-types-register" "create-fluids-register"
   "create-sounds-register" "create-effects-register"
   "create-particle-types-register"])

(defn- load-allowlist []
  (let [f (io/file "docs/dev/aot-linter-allowlist.edn")]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defn- allowed-line? [allowlist line]
  (some #(str/includes? line (str %)) (:allowed-top-levels allowlist #{})))

(defn- safe-deferred-line? [line]
  (or (str/includes? line "deferred/deferred")
      (str/includes? line "(delay ")
      (str/includes? line "(delay\n")))

(defn- top-level-def-line? [line]
  (boolean (re-find #"^\(def(?:once)?\s" (str/trim line))))

(defn- forbidden-in-line? [line]
  (some #(str/includes? line %) forbidden-symbols))

(defn- lint-file [^File file allowlist]
  (let [rel (.getPath file)
        lines (str/split-lines (slurp file))]
    (mapcat
      (fn [[idx line]]
        (when (and (top-level-def-line? line)
                   (not (safe-deferred-line? line))
                   (not (allowed-line? allowlist line))
                   (forbidden-in-line? line))
          (let [sym (or (re-find #"Blocks/\S+" line)
                        (some #(when (str/includes? line %) %) forbidden-symbols))]
            [{:file rel
              :line (inc idx)
              :symbol sym
              :hint "Wrap registry/bootstrap access in deferred/deferred or delay; see docs/dev/AOT_BOOTSTRAP.md"}])))
      (map vector (range) lines))))

(defn- source-dirs []
  (if-let [raw (System/getProperty "aot.lint.sourceDirs")]
    (map str/trim (str/split raw #","))
    default-source-dirs))

(defn- clj-files [dir]
  (let [root (io/file dir)]
    (when (.exists root)
      (->> (file-seq root)
           (filter #(.isFile ^File %))
           (filter #(.endsWith (.getName ^File %) ".clj"))
           vec))))

(defn -main [& _]
  (let [allowlist (or (load-allowlist) {})
        files (mapcat clj-files (source-dirs))
        violations (vec (mapcat #(lint-file % allowlist) files))]
    (if (seq violations)
      (do
        (println "AOT bootstrap safety violations:")
        (doseq [{:keys [file line symbol hint]} violations]
          (println (str file ":" line " symbol=" symbol " hint=" hint)))
        (System/exit 1))
      (do
        (println "AOT bootstrap safety: OK (" (count files) "files scanned)")
        (System/exit 0)))))

(-main)
