(ns cn.li.ac.content.namespace-migration-test
	(:require [clojure.java.io :as io]
					[clojure.string :as str]
					[clojure.test :refer [deftest is]]))

(def ^:private legacy-namespace-token
	(str "my_mod" ":"))

(def ^:private text-resource-extensions
	#{".json" ".mcmeta" ".md" ".properties" ".txt" ".xml"})

(defn- clj-files-under [path]
	(->> (file-seq (io/file path))
		 (filter #(.isFile %))
		 (filter #(str/ends-with? (.getName %) ".clj"))))

(defn- text-resource-files-under [path]
	(->> (file-seq (io/file path))
		 (filter #(.isFile %))
		 (filter (fn [file]
						 (let [name (str/lower-case (.getName file))]
							(some #(str/ends-with? name %) text-resource-extensions))))))

(defn- files-containing-legacy-namespace []
	(->> (concat (clj-files-under "src/main/clojure")
				   (clj-files-under "src/test/clojure"))
		 (filter #(str/includes? (slurp %) legacy-namespace-token))
		 (map #(.getPath %))
		 sort
		 vec))

(defn- resource-files-containing-legacy-namespace []
	(->> (text-resource-files-under "src/main/resources/assets/academy")
		 (filter #(str/includes? (slurp %) legacy-namespace-token))
		 (map #(.getPath %))
		 sort
		 vec))

(deftest ac-clojure-source-does-not-use-legacy-my-mod-namespace
	(is (empty? (files-containing-legacy-namespace))))

(deftest ac-static-text-resources-do-not-use-legacy-my-mod-namespace
	(is (empty? (resource-files-containing-legacy-namespace))))
