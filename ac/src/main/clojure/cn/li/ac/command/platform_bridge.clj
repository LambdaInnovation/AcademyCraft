(ns cn.li.ac.command.platform-bridge
	"AC command bindings for command runtime hooks."
	(:require [cn.li.mcmod.command.runtime-hooks :as command-hooks]
						[cn.li.mcmod.command.metadata :as command-metadata]
						[cn.li.ac.command.actions :as command-actions]
						[cn.li.ac.command.commands :as commands]
						[cn.li.ac.command.dsl :as command-dsl]
						[cn.li.mcmod.runtime.install :as install]
						[cn.li.mcmod.util.log :as log]))

(defn- init-commands-and-publish-metadata!
	"Populate the DSL registry, then publish it to the platform metadata
	registry. Publishing must happen AFTER commands/init-commands! — an eager
	snapshot at hook-install time captures an empty DSL registry, so Brigadier
	registration iterates zero commands (/aim etc. silently missing)."
	[]
	(commands/init-commands!)
	(command-metadata/register-command-registry!
		(:commands (command-dsl/command-registry-snapshot))))

(defn install-command-hooks!
	[]
	(install/framework-once! ::hooks-installed?
  (fn []
    (command-actions/install-command-actions!)
		(command-hooks/register-command-hooks!
			{:init-commands! init-commands-and-publish-metadata!})
		(log/debug "AC command hooks installed")))
	nil)
