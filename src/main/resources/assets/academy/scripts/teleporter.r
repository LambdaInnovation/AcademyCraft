ac {
    teleporter {
		_crithit {
			incr_0 { 2.5 }
			incr_1 { 1.75 }
			incr_2 { 1.4 }
		}
	
        location_teleport {
            # 单次传送消耗。 参数：(距离, 跨维度惩罚(1 or 2), 技能经验)
            consumption(dist, dimfac, exp) { 
                lerp(200, 150, exp) * dimfac * max(8, sqrt( min(800, dist) ))
            }
            # 最多附加传送几个实体
            entities(exp) {
                round(lerp(3, 7, exp))
            }
            # 传送距离
            range { 4 }
            overload(exp) { 240 }
			expincr(distance) {
				switch(distance) {
					when >= 200: 0.08;
					default: 0.05
				}
			}
        }
		
		mark_teleport {
			range(exp) { lerp(25, 60, exp) }
			consumption(exp) { lerp(15, 9, exp) }
			overload(exp) { lerp(40, 20, exp) }
			expincr(dist) { 0.0001 * dist }
		}
        
        penetrate_teleport {
            cooldown(exp) { lerp(120, 60, exp) }
            max_distance(exp) { lerp(10, 35, exp) }
            consumption(exp) { lerp(20, 14, exp) }
            overload(exp) { lerp(80, 50, exp) }
			expincr(distance) {
				0.00008 * distance
			}
        }
        
        threatening_teleport {
            range(exp) { lerp(8, 15, exp) }
            damage(exp) { lerp(3, 6, exp) }
            consumption(exp) { lerp(35, 150, exp) }
            overload(exp) { lerp(18, 13, exp) }
			expincr { 0.005 }
        }
        
        shift_tp {
            damage(exp) { lerp(15, 30, exp) }
            consumption(exp) { lerp(160, 320, exp) }
            overload(exp) { lerp(80, 50, exp) }
            range(exp) { lerp(25, 35, exp) }
			expincr(entities) { (1 + entities) * 0.003 }
        }
		
		flesh_ripping {
			range(exp) { lerp(6, 14, exp) }
			damage(exp) { lerp(5, 15, exp) }
			consumption(exp) { lerp(130, 150, exp) }
			overload(exp) { lerp(60, 50, exp) }
			cooldown(exp) { floor(20 * lerp(5, 3, exp)) }
			disgust_prob { 0.05 }
			expincr { 0.005 }
		}
		
		flashing {
			range(exp) { lerp(12, 18, exp) }
			consumption(exp) { lerp(100, 70, exp) }
			overload(exp) { lerp(250, 180, exp) }
			expincr { 0.002 }
		}
    }
}