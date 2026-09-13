(ns cn.li.ac.content.block-loader-core
	"Shared orchestration for AC block content category loaders."
	(:require [cn.li.mcmod.util.log :as log]))

(defn require-namespaces!
	[namespaces]
	(doseq [ns-sym namespaces]
		(require ns-sym)))

(defn- resolve-entry-fn
	[entry]
	(cond
		(symbol? entry) (do (require (symbol (namespace entry)))
		                    (find-var entry))
		(ifn? entry) entry
		:else nil))

(defn invoke-entries!
	[entries]
	(doseq [entry entries]
		(when-let [entry-fn (resolve-entry-fn entry)]
			(entry-fn))))

(defn load-block-category!
	[{:keys [label namespaces init-entries post-init-entries]}]
	(when label
		(log/debug "Loading block category" {:label label}))
	(require-namespaces! namespaces)
	(invoke-entries! init-entries)
	(invoke-entries! post-init-entries)
	(when label
		(log/debug "Loaded block category" {:label label})))
