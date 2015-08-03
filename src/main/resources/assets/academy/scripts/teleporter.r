ac {
    teleporter {
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
        }
        
        penetrate_teleport {
            cooldown(exp) { lerp(120, 60, exp) }
            max_distance(exp) { lerp(10, 35, exp) }
            consumption(exp) { lerp(20, 14, exp) }
            overload(exp) { lerp(80, 50, exp) }
        }
        
        threatening_teleport {
            range(exp) { lerp(8, 15, exp) }
            damage(exp) { lerp(3, 6, exp) }
            consumption(exp) { lerp(35, 150, exp) }
            overload(exp) { lerp(18, 13, exp) }
        }
        
        shift_tp {
            damage(exp) { lerp(15, 30, exp) }
            consumption(exp) { lerp(160, 320, exp) }
            overload(exp) { lerp(80, 50, exp) }
            range(exp) { lerp(25, 35, exp) }
        }
		
		flesh_ripping {
			range(exp) { lerp(6, 14, exp) }
			damage(exp) { lerp(5, 15, exp) }
			consumption(exp) { lerp(130, 150, exp) }
			overload(exp) { lerp(60, 50, exp) }
			cooldown(exp) { floor(20 * lerp(5, 3, exp)) }
			disgust_prob { 0.05 }
		}
		
		flashing {
			range(exp) { lerp(12, 18, exp) }
			consumption(exp) { lerp(100, 70, exp) }
			overload(exp) { lerp(250, 180, exp) }
		}
    }
}