# AcademyCraft Ripple Script file
# Electro Master
# 炮姐什么的最喜欢啦啦啦啦啦啦! >3<

ac {
	electro_master {
	   # 电弧激发
		arc_gen { 
		  # 伤害
		  damage(exp) { lerp(3, 7, exp) } 
		  # CP消耗
		  consumption(exp) { lerp(60, 90, exp) } 
		  # 过载
		  overload(exp) { lerp(15, 10, exp) } 
		  # 点燃概率
		  p_ignite(exp) { lerp(0, 0.6, exp) } 
		  
		  # 有效攻击时增加的熟练度
		  exp_incr_effective(exp) { 
		      0.00008 * lerp(60, 90, exp)
		  }
		  # 无效攻击时增加的熟练度
		  exp_incr_ineffective(exp) {
		      0.00003 * lerp(60, 90, exp)
		  }
		}
		
		# 电流回充
		charging { 
		  # 充能速度，IF/tick
		  speed(exp) { lerp(5, 15, exp) } 
		  consumption(exp) { lerp(3, 7, exp) }
		  overload(exp) { lerp(65, 48, exp) }
		  
		  exp_incr_effective(exp) { lerp(3, 7, exp) * 0.0008 }
		  exp_incr_ineffective(exp) { lerp(3, 7, exp) * 0.0003 }
		}
		
		# 生物电强化, ct=蓄力时间 (range: [10, 40])
		body_intensify { 
		  # 总概率
		  probability(ct) { (ct - 10.0) / 18.0 } 
		  # 每个buff持续时间
		  time(exp, ct) { floor(4 * lerp(1.5, 2.5, exp) * range_double(1, 2) * ct) } 
		  # buff等级
		  level(exp, ct) { floor( lerp(0.5, 1, exp) * (ct / 18.0) ) } 
		  # 饥饿buff时间
		  hunger_time(ct) { ct * 5 / 4 } 
		  
		  consumption(exp) { lerp(20, 15, exp) }
		  cooldown(exp) { lerp(45, 30, exp) }
		  overload(exp) { lerp(200, 120, exp) }
		}
		
		# 矿物探测
		mine_detect { 
		  consumption(exp) { lerp(1800, 1400, exp) }
		  overload(exp) { lerp(200, 180, exp) }
		  cooldown(exp) { lerp(900, 400, exp) }
		  # 可视距离
		  range(exp) { lerp(15, 30, exp) } 
		}
		
		# 电磁牵引
		mag_movement { 
		  consumption(exp) { lerp(15, 10, exp) } # per tick
		  overload(exp) { lerp(3, 2, exp) } # per tick
		  exp_incr(distance) { distance * 0.00015 }
		}
		
		# 超电磁炮
		railgun { 
		  consumption(exp) { lerp(200, 500, exp) }
		  overload(exp) { lerp(120, 80, exp) }
		  cooldown(exp) { lerp(300, 160, exp) }
		}
		
	}
}