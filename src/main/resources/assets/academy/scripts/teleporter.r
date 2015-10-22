ac {
    teleporter {
		# 暴击数据，分别是三级暴击的伤害倍率
		_crithit {
			incr_0 { 2.5 }
			incr_1 { 1.75 }
			incr_2 { 1.4 }
		}
		
		# 危险传送
		threatening_teleport {
			x { 85 } y { 239 }
			range(exp) { lerp(8, 15, exp) }
            damage(exp) { lerp(6, 9, exp) }
            consumption(exp) { lerp(129, 149, exp) }
            overload(exp) { lerp(26, 11, exp) }
			cooldown(exp) { lerp(18, 10, exp) }
			expincr { 0.003 }
        }
		
		# 空间折叠概论
		dim_folding_theoreom {
			x { 157 } y { 375 }
			# 伴随其他非被动技能技能经验增长的经验增长率
			incr_rate { 0.6 }
		}
		
		# 穿透传送
		penetrate_teleport {
			x { 315 } y { 332 }
            cooldown(exp) { lerp(70, 40, exp) }
			# 最大距离
            max_distance(exp) { lerp(10, 35, exp) }
            consumption(exp) { lerp(15, 10, exp) }
            overload(exp) { lerp(80, 50, exp) }
			expincr(distance) {
				0.00014 * distance
			}
        }
		
		# 标记传送
		mark_teleport {
			x { 304 } y { 143 }
			range(exp) { lerp(25, 60, exp) }
			consumption(exp) { lerp(13, 5, exp) }
			overload(exp) { lerp(40, 20, exp) }
			expincr(dist) { 0.00018 * dist }
			cooldown(exp) { lerp(50, 20, exp) }
		}
		
		# 肌体撕裂
		flesh_ripping {
			x { 554 } y { 119 }
			range(exp) { lerp(6, 14, exp) }
			damage(exp) { lerp(9, 15, exp) }
			consumption(exp) { lerp(247, 302, exp) }
			overload(exp) { lerp(60, 50, exp) }
			cooldown(exp) { floor(20 * lerp(5, 3, exp)) }
			# 恶心buff出现概率
			disgust_prob { 0.05 }
			expincr { 0.005 }
		}
		
		# 定位传送
        location_teleport {
			x { 533 } y { 317 }
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
			# @param distance: 传送距离
			expincr(distance) {
				switch(distance) {
					when >= 200: 0.08;
					default: 0.05
				}
			}
        }
        
		# 转移传送
        shift_tp {
			x { 740 } y { 330 }
            damage(exp) { lerp(16, 27, exp) }
            consumption(exp) { lerp(374, 500, exp) }
            overload(exp) { lerp(38, 27, exp) }
            range(exp) { lerp(25, 35, exp) }
			
			# @param entities: 攻击路径上攻击到的实体数量
			expincr(entities) { (1 + entities) * 0.002 }
			cooldown(exp) { lerp(20, 5, exp) }
        }
		
		space_fluct {
			x { 886 } y { 412 }
			# 伴随其他非被动技能技能经验增长的经验增长率
			incr_rate { 0.4 }
		}
		
		flashing {
			x { 839 } y { 226 }
			range(exp) { lerp(12, 18, exp) }
			consumption(exp) { lerp(100, 70, exp) }
			overload(exp) { lerp(100, 80, exp) }
			expincr { 0.002 }
		}
    }
}
