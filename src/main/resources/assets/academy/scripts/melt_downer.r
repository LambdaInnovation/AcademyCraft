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
			consumption(exp) { 233 }
			overload(exp) { 233 }
		}
	}
}