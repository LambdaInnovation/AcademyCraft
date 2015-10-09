# AcademyCraft Ripple Script file
# Electromaster
# 炮姐什么的最喜欢了! >3<

ac {
    electromaster {
		# 电弧激发
        arc_gen {
            x { 112 } y { 230 }
            damage(exp) { lerp(4, 9, exp) } 
            consumption(exp) { lerp(60, 90, exp) } 
            overload(exp) { lerp(15, 10, exp) }
            # 点燃方块的概率 
            p_ignite(exp) { lerp(0, 0.6, exp) } 
            cooldown(exp) { lerp(6, 3, exp) }
            range(exp) { lerp(6, 15, exp) }
			
			# 击中敌人时经验增长
            expincr_effective(exp) { 
                0.00008 * lerp(60, 90, exp)
            }
            
			# 击空时经验增长
            expincr_ineffective(exp) {
              0.00003 * lerp(60, 90, exp)
            }
        }
        
		# 电流回充
        charging {
            x { 217 } y { 89 }
            # 充电速率，IF/tick
            speed(exp) { lerp(10, 30, exp) } 
            consumption(exp) { lerp(6, 14, exp) }
            overload(exp) { lerp(65, 48, exp) }
            
            expincr_effective(exp) { 0.0001 }
            expincr_ineffective(exp) { 0.00003 }
        }
        
		# 生物电强化
        # @param ct: 蓄力时间，单位tick  [10, 40]
        body_intensify { 
            x { 357 } y { 77 }
			# 总buff出现概率值，>1则计算多次
            probability(ct) { (ct - 10.0) / 18.0 } 
			# buff持续时间
            time(exp, ct) { floor(4 * lerp(1.5, 2.5, exp) * range_double(1, 2) * ct) } 
			# buff等级
            level(exp, ct) { floor( lerp(0.5, 1, exp) * (ct / 18.0) ) } 
			# 饥饿buff的时间
            hunger_time(ct) { ct * 5 / 4 } 
            
            consumption(exp) { lerp(20, 15, exp) }
            cooldown(exp) { lerp(45, 30, exp) }
            overload(exp) { lerp(200, 120, exp) }
            expincr { 0.01 }
        }
        
		# 矿物探测
        mine_detect { 
            x { 786 } y { 60 }
            consumption(exp) { lerp(1800, 1400, exp) }
            overload(exp) { lerp(200, 180, exp) }
            cooldown(exp) { lerp(900, 400, exp) }
            range(exp) { lerp(15, 30, exp) } 
            expincr { 0.008 }
        }
        
		# 电磁移动
        mag_movement { 
            x { 490 } y { 177 }
            consumption(exp) { lerp(15, 10, exp) } # per tick
            overload(exp) { lerp(3, 2, exp) } # per tick
            expincr(distance) { distance * 0.0011 }
        }
        
		# 雷击之枪
        thunder_bolt {
            x { 321 } y { 334 }
            damage(exp) { lerp(15, 25, exp) }
			# 扩散的电弧伤害值
            aoe_damage(exp) { 0.4 * lerp(15, 25, exp) }
            consumption(exp) { lerp(100, 200, exp) }
            overload(exp) { lerp(30, 27, exp) }
            cooldown(exp) { floor(20 * lerp(4, 1.5, exp)) }
            expincr_effective { 
                0.003
            }
            expincr_ineffective {
               0.005
            }
        }
        
		# 超电磁炮
        railgun {
            x { 581 } y { 295 }
            consumption(exp) { lerp(1200, 800, exp) }
            overload(exp) { 3 * lerp(120, 80, exp) }
            cooldown(exp) { lerp(300, 160, exp) }
            damage(exp) { lerp(25, 45, exp) }
			# 方块破坏能量（最大可破坏的方块的硬度值的和）
            energy(exp) { lerp(900, 2000, exp) }
            expincr { 0.005 }
        }
        
		# 最后的落雷
        thunder_clap {
            x { 714 } y { 400 }
            damage(exp, ct) { lerp(40, 70, exp) * lerp(1, 1.2, (ct - 40.0) / 60.0) }
            range(exp) { 3 * lerp(5, 10, exp) }
            consumption(exp) { lerp(100, 120, exp) }
            overload(exp) { lerp(400, 350, exp) }
            cooldown(exp, ct) { ct * lerp(9, 5, exp) }
            expincr { 0.003 }
        }
        
        iron_sand {
            x { 844 } y { 271 }
        }
        
		# 磁力操纵
        mag_manip {
            x { 713 } y { 165 }
            damage(exp) { lerp(8, 15, exp) }
            consumption(exp) { lerp(140, 270, exp) }
            overload(exp) { lerp(35, 20, exp) }
            cooldown(exp) { 20 * lerp(2, 1, exp) }
            expincr { 0.0025 }
        }
    }
}