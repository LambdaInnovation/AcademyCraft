(ns cn.li.ac.wireless.gui.tab.role-config
	"Role-specific wireless tab configuration."
	(:require [cn.li.ac.wireless.gui.message.registry :as msg-registry]))

(defn- node-msg [action] (msg-registry/msg :node action))
(defn- gen-msg [action] (msg-registry/msg :generator action))
(defn- dev-msg [action] (msg-registry/msg :developer action))
(defn- interferer-msg [action] (msg-registry/msg :ability-interferer action))

(def role-config
	{:node      {:list-msg       #(node-msg :list-networks)
							 :disconnect-msg #(node-msg :disconnect)
							 :connect-msg    #(node-msg :connect)
							 :name-fn        (fn [t] (:ssid t))
							 :connect-payload-fn (fn [payload target pass]
																		 (assoc payload :ssid (:ssid target) :password pass))
							 :logo-path      "textures/guis/icons/icon_tomatrix.png"
							 :logo-breathe?  true}
	 :generator {:list-msg       #(gen-msg :list-nodes)
							 :disconnect-msg #(gen-msg :disconnect)
							 :connect-msg    #(gen-msg :connect)
							 :name-fn        (fn [t] (:node-name t))
							 :connect-payload-fn (fn [payload target pass]
																		 (assoc payload
																						:node-x (:pos-x target)
																						:node-y (:pos-y target)
																						:node-z (:pos-z target)
																						:password pass
																						:need-auth? true))
							 :logo-path      nil
							 :logo-breathe?  true}
	 :receiver  {:list-msg       #(dev-msg :list-nodes)
							 :disconnect-msg #(dev-msg :disconnect)
							 :connect-msg    #(dev-msg :connect)
							 :name-fn        (fn [t] (:node-name t))
							 :connect-payload-fn (fn [payload target pass]
																		 (assoc payload
																						:node-x (:pos-x target)
																						:node-y (:pos-y target)
																						:node-z (:pos-z target)
																						:password pass
																						:need-auth? true))
							 :logo-path      nil
							 :logo-breathe?  true}
	 :ability-interferer
	             {:list-msg       #(interferer-msg :list-nodes)
							 :disconnect-msg #(interferer-msg :disconnect)
							 :connect-msg    #(interferer-msg :connect)
							 :name-fn        (fn [t] (:node-name t))
							 :connect-payload-fn (fn [payload target pass]
																		 (assoc payload
																						:node-x (:pos-x target)
																						:node-y (:pos-y target)
																						:node-z (:pos-z target)
																						:password pass
																						:need-auth? true))
							 :logo-path      nil
							 :logo-breathe?  true}
	 :machine   {:list-msg       #(dev-msg :list-nodes)
							 :disconnect-msg #(dev-msg :disconnect)
							 :connect-msg    #(dev-msg :connect)
							 :name-fn        (fn [t] (:node-name t))
							 :connect-payload-fn (fn [payload target pass]
																		 (assoc payload
																						:node-x (:pos-x target)
																						:node-y (:pos-y target)
																						:node-z (:pos-z target)
																						:password pass
																						:need-auth? true))
							 :logo-path      nil
							 :logo-breathe?  true}})
