(ns cn.li.ac.content.sound-resources-test
	(:require [clojure.java.io :as io]
					[clojure.set :as set]
					[clojure.string :as str]
					[clojure.test :refer [deftest is testing]]
					[cn.li.ac.content.sounds :as sounds]))

(def ^:private sounds-json-path "assets/academy/sounds.json")

(defn- sounds-json-text []
	(slurp (io/resource sounds-json-path)))

(defn- parse-sound-entries []
	(into {}
			(map (fn [[_ sound-id resource-name]]
					[sound-id resource-name]))
			(re-seq #"\"([^\"]+)\"\s*:\s*\{\s*\"category\"\s*:\s*\"[^\"]+\"\s*,\s*\"sounds\"\s*:\s*\[\s*\{\s*\"name\"\s*:\s*\"([^\"]+)\"" (sounds-json-text))))

(defn- sound-resource-root []
	(io/file "src/main/resources/assets/academy/sounds"))

(defn- normalize-path [s]
	(str/replace s #"\\" "/"))

(defn- file->sound-resource-name [root file]
	(let [root-path (normalize-path (.getPath root))
			file-path (normalize-path (.getPath file))
			rel (subs file-path (inc (count root-path)))
			without-ext (str/replace rel #"\.ogg$" "")]
		(str "academy:" without-ext)))

(defn- actual-sound-resource-names []
	(let [root (sound-resource-root)]
		(->> (file-seq root)
			 (filter #(.isFile %))
			 (filter #(str/ends-with? (.getName %) ".ogg"))
			 (map #(file->sound-resource-name root %))
			 set)))

(defn- clj-source-files []
	(->> (file-seq (io/file "src/main/clojure"))
		 (filter #(.isFile %))
		 (filter #(str/ends-with? (.getName %) ".clj"))))

(defn- code-sound-ids []
	(->> (clj-source-files)
		 (mapcat (fn [file]
					(re-seq #"academy:([A-Za-z0-9_]+\.[A-Za-z0-9_.-]+)" (slurp file))))
		 (map second)
		 set))

(deftest sound-json-uses-current-namespace-and-real-ogg-files
	(let [entries (parse-sound-entries)
			actual-resource-names (actual-sound-resource-names)
			resource-names (set (vals entries))]
		(testing "all sounds.json sound resources use academy namespace"
			(is (empty? (remove #(str/starts-with? % "academy:") resource-names))))
		(testing "all sounds.json sound resources point to existing ogg files"
			(is (empty? (sort (set/difference resource-names actual-resource-names)))))
		(testing "all shipped ogg files are referenced by sounds.json"
			(is (empty? (sort (set/difference actual-resource-names resource-names)))))))

(deftest registered-sound-ids-match-sounds-json
	(let [json-ids (set (keys (parse-sound-entries)))
			declared-ids (set sounds/all-sound-ids)]
		(is (= json-ids declared-ids))))

(deftest code-sound-literals-are-declared
	(let [json-ids (set (keys (parse-sound-entries)))
			literal-ids (code-sound-ids)]
		(is (empty? (sort (set/difference literal-ids json-ids))))))