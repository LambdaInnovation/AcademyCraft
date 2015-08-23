ac {
	melt_downer {
		scatter_bomb {
			damage(exp) { lerp(5, 9, exp) }
			consumption(exp) { lerp(3, 6, exp) }
			overload(exp) { lerp(100, 80, exp) }
		}
		
		ray_barrage {
			consumption(exp) { 233 }
			overload(exp) { 233 }
		}
		
		light_shield {
			consumption(exp) { lerp(9, 4, exp) }
			overload(exp) { lerp(90, 60, exp) }
			cooldown(exp) { lerp(80, 60, exp) }
			
			absorb_consumption(exp) { lerp(50, 30, exp) }
			absorb_overload(exp) { lerp(50, 30, exp) }
			
			touch_damage(exp) { lerp(2, 6, exp) }
		  absorb_damage(exp) { lerp(8, 25, exp) }
		}
		
		jet_engine {
			damage(exp) { lerp(7, 10, exp) }
			cooldown(exp) { 20 * floor(lerp(6, 3, exp)) }
			consumption(exp) { lerp(170, 140, exp) }
			overload(exp) { lerp(60, 50, exp) }
			expincr { 0.004 }
		}
		
		mine_ray_basic {
			range { 10 }
			harvest_level { 2 }
			speed(exp) { 0.1 * lerp(1, 2, exp) }
			consumption(exp) { lerp(15, 20, exp) }
			overload(exp) { lerp(4, 2.5, exp) }
			cooldown(exp) { lerp(60, 30, exp) }
			expincr { 0.0005 }
		}
		
		mine_ray_expert {
			range { 20 }
			harvest_level { 5 }
			speed(exp) { 0.5 * lerp(1, 2, exp) }
			consumption(exp) { lerp(25, 20, exp) }
			overload(exp) { lerp(6, 5, exp) }
			cooldown(exp) { lerp(60, 30, exp) }
			expincr { 0.0003 }
		}
		
		mine_ray_luck {
			range { 20 }
			harvest_level { 5 }
			speed(exp) { 0.5 * lerp(1, 2, exp) }
			consumption(exp) { lerp(25, 20, exp) }
			overload(exp) { lerp(6, 5, exp) }
			cooldown(exp) { lerp(60, 30, exp) }
			expincr { 0.0003 }
		}
		
		mine_ray_acc {
			range { 20 }
			harvest_level { 5 }
			speed(exp) { 0.5 * lerp(1, 2, exp) }
			consumption(exp) { lerp(25, 20, exp) }
			overload(exp) { lerp(6, 5, exp) }
			cooldown(exp) { lerp(60, 30, exp) }
			expincr { 0.0003 }
		}
	}
}