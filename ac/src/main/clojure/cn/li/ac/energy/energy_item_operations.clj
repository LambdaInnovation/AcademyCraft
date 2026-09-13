(ns cn.li.ac.energy.energy-item-operations
	"Generic item-level energy operations across registered energy types."
	(:require [cn.li.ac.energy.energy-type-interface :as energy-type]
						[cn.li.ac.energy.imaginary-energy-impl :as imaginary-energy]))

(defn- ensure-default-type!
	[]
	(imaginary-energy/register-default-energy-type!))

(defn resolve-item-energy-type
	[item-stack]
	(ensure-default-type!)
	(energy-type/resolve-energy-type item-stack))

(defn supports-energy-item?
	[item-stack]
	(boolean (resolve-item-energy-type item-stack)))

(defn get-item-energy
	[item-stack]
	(if-let [etype (resolve-item-energy-type item-stack)]
		(energy-type/get-energy* etype item-stack)
		0.0))

(defn get-item-capacity
	[item-stack]
	(if-let [etype (resolve-item-energy-type item-stack)]
		(energy-type/get-capacity* etype item-stack)
		0.0))

(defn get-item-bandwidth
	[item-stack]
	(if-let [etype (resolve-item-energy-type item-stack)]
		(energy-type/get-bandwidth* etype item-stack)
		0.0))

(defn set-item-energy!
	[item-stack amount]
	(when-let [etype (resolve-item-energy-type item-stack)]
		(energy-type/set-energy*! etype item-stack amount)))

(defn charge-energy-to-item
	[item-stack amount ignore-bandwidth]
	(if-let [etype (resolve-item-energy-type item-stack)]
		(energy-type/charge-item*! etype item-stack amount ignore-bandwidth)
		(double amount)))

(defn pull-energy-from-item
	[item-stack amount ignore-bandwidth]
	(if-let [etype (resolve-item-energy-type item-stack)]
		(energy-type/discharge-item*! etype item-stack amount ignore-bandwidth)
		0.0))