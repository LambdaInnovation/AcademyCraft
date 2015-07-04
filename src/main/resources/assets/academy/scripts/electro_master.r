ac {
	electro_master {
		arc_gen { # 电弧激发
		  damage(exp) { lerp(3, 7, exp) } # 伤害
		  consumption(exp) { lerp(60, 90, exp) } # CP消耗
		  overload(exp) { lerp(15, 10, exp) } # 过载
		  p_ignite(exp) { lerp(0, 0.6, exp) } # 点燃概率
		  
		  # 有效攻击时增加的熟练度
		  exp_incr_effective(exp) { 
		      0.00008 * consumption(exp)
		  }
		  # 无效攻击时增加的熟练度
		  exp_incr_ineffective(exp) {
		      0.00003 * consumption(exp)
		  }
		},
		
		charging { # 电流回充
		  speed(exp) { lerp(5, 15, exp) }, # IF/tick
		  consumption(exp) { lerp(3, 7, exp) },
		  overload(exp) { lerp(65, 48, exp) },
		  
		  exp_incr_effective(exp) { consumption(exp) * 0.0008 },
		  exp_incr_ineffective(exp) { consumption(exp) * 0.0003 }
		}
	}
}