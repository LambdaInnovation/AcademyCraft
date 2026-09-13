(ns cn.li.ac.registry.spi.content-phase
	"SPI facade for declaring and listing AC content phases."
	(:require [cn.li.ac.registry.discovery :as discovery]))

(defn declare-content-phase!
	"Declare (or replace by :phase key) a content phase plugin spec."
	[phase-spec]
	(discovery/register-phase-provider! phase-spec))

(defn list-content-phases
	"List currently declared content phases in load order."
	[]
	(discovery/discovered-content-phases))