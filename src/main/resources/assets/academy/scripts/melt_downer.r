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
	}
}