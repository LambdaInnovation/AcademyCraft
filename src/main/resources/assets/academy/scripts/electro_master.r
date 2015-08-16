# AcademyCraft Ripple Script file
# Electro Master
# 炮姐什么的最喜欢啦啦啦啦啦啦! >3<

ac {
	electro_master {
		arc_gen {
		  x { 112 } y { 230 }
		  damage(exp) { lerp(3, 7, exp) } 
		  consumption(exp) { lerp(60, 90, exp) } 
		  overload(exp) { lerp(15, 10, exp) } 
		  p_ignite(exp) { lerp(0, 0.6, exp) } 
		  
		  exp_incr_effective(exp) { 
		      0.00008 * lerp(60, 90, exp)
		  }
		  exp_incr_ineffective(exp) {
		      0.00003 * lerp(60, 90, exp)
		  }
		}
		
		charging {
		  x { 317 } y { 159 }
		  # IF/tick
		  speed(exp) { lerp(5, 15, exp) } 
		  consumption(exp) { lerp(3, 7, exp) }
		  overload(exp) { lerp(65, 48, exp) }
		  
		  exp_incr_effective(exp) { lerp(3, 7, exp) * 0.0008 }
		  exp_incr_ineffective(exp) { lerp(3, 7, exp) * 0.0003 }
		}
		
		# ct: charge time (range: [10, 40])
		body_intensify { 
		  x { 427 } y { 53 }
		  probability(ct) { (ct - 10.0) / 18.0 } 
		  time(exp, ct) { floor(4 * lerp(1.5, 2.5, exp) * range_double(1, 2) * ct) } 
		  level(exp, ct) { floor( lerp(0.5, 1, exp) * (ct / 18.0) ) } 
		  hunger_time(ct) { ct * 5 / 4 } 
		  
		  consumption(exp) { lerp(20, 15, exp) }
		  cooldown(exp) { lerp(45, 30, exp) }
		  overload(exp) { lerp(200, 120, exp) }
		}
		
		mine_detect { 
		  x { 786 } y { 60 }
		  consumption(exp) { lerp(1800, 1400, exp) }
		  overload(exp) { lerp(200, 180, exp) }
		  cooldown(exp) { lerp(900, 400, exp) }
		  range(exp) { lerp(15, 30, exp) } 
		}
		
		mag_movement { 
		  x { 501 } y { 177 }
		  consumption(exp) { lerp(15, 10, exp) } # per tick
		  overload(exp) { lerp(3, 2, exp) } # per tick
		  exp_incr(distance) { distance * 0.00015 }
		}
		
		thunder_bolt {
		    x { 321 } y { 334 }
			damage(exp) { lerp(10, 18, exp) }
			aoe_damage(exp) { 0.2 * lerp(10, 18, exp) }
			consumption(exp) { lerp(100, 200, exp) }
			overload(exp) { lerp(30, 27, exp) }
			cooldown(exp) { floor(20 * lerp(4, 1.5, exp)) }
		}
		
		railgun {
		  x { 581 } y { 295 }
		  consumption(exp) { lerp(200, 500, exp) }
		  overload(exp) { lerp(120, 80, exp) }
		  cooldown(exp) { lerp(300, 160, exp) }
		}
		
		thunder_clap {
		  x { 714 } y { 400 }
		  damage(exp, ct) { lerp(40, 70, exp) * lerp(1, 1.2, (ct - 40.0) / 60.0) }
		  range(exp) { 2 * lerp(5, 10, exp) }
		  consumption(exp) { lerp(100, 120, exp) }
		  overload(exp) { lerp(400, 350, exp) }
		  cooldown(exp, ct) { ct * lerp(18, 10, exp) }
		}
		
		iron_sand {
		  x { 844 } y { 271 }
		}
		
		mag_manip {
		  x { 713 } y { 165 }
		}
	}
}