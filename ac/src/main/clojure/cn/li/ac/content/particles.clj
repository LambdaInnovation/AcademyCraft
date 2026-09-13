(ns cn.li.ac.content.particles
	"Custom particle declarations."
	(:require [cn.li.mcmod.particle.dsl :as pdsl]
						[cn.li.mcmod.runtime.install :as install]
						[cn.li.mcmod.util.log :as log]))

(defn init-particles!
	[]
	(install/framework-once! ::particles-initialized?
  (fn []
    (pdsl/defparticle {:id "electric_arc"})
		(pdsl/defparticle {:id "teleport_ripple"})
		(pdsl/defparticle {:id "melt_glow"})
		(pdsl/defparticle {:id "silbarn_frag"})
		(pdsl/defparticle {:id "md_particle"})
		(pdsl/defparticle {:id "md_particle_luck"})
		(log/debug "Particle content initialized"))))