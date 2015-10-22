ac {
    meltdowner {
		# 电子光束
        electron_bomb {
            x { 106 } y { 202 }
            damage(exp) { lerp(12, 20, exp) }
            consumption(exp) { lerp(117, 135, exp) }
            overload(exp) { lerp(39, 17, exp) }
            cooldown(exp) { lerp(20, 10, exp) }
            expincr { 0.005 }
        }
        
		# β射线强化
        rad_intensify {
            x { 212 } y { 315 }
			
			# 伤害的强化倍率
            rate(exp) { lerp(1.4, 1.8, exp) }
        }
		
		# 电子弹散射
        scatter_bomb {
            x { 308 } y { 70 }
            damage(exp) { lerp(4, 6, exp) }
            consumption(exp) { lerp(7, 9, exp) }
            overload(exp) { lerp(165, 68, exp) }
            expincr { 0.001 }
        }
        
		# 光盾
        light_shield {
            x { 391 } y { 266 }
            consumption(exp) { lerp(12, 7, exp) }
            overload(exp) { lerp(198, 132, exp) }
            cooldown(exp) { lerp(80, 60, exp) }
            
			# 伤害吸收一次的消耗或过载
            absorb_consumption(exp) { lerp(50, 30, exp) }
            absorb_overload(exp) { lerp(15, 10, exp) }
            
			# 攻击到实体给予的伤害
            touch_damage(exp) { lerp(3, 8, exp) }
			# 最大吸收的伤害值
            absorb_damage(exp) { lerp(15, 50, exp) }
            
			# 在使用一次光盾、被实体攻击一次、给予实体攻击一次时都增加经验
            expincr { 0.001 }
        }
        
		# 原子崩坏
        meltdowner {
            x { 488 } y { 166 }
			# 内部使用
            rate(time) { lerp(0.8, 1.2, (time - 20.0) / 20.0) }
			
			# 光束半径
            range(exp) { lerp(2, 3, exp) }
			# 方块破坏能量
            energy(exp, time) { rate(time) * lerp(300, 700, exp) }
            damage(exp, time) { rate(time) * lerp(20, 50, exp) }
            overload(exp) { lerp(300, 200, exp) }
            consumption(exp) { lerp(15, 27, exp) }
            cooldown(exp, time) { rate(time) * 20 * lerp(15, 7, exp) }
            expincr(time) { rate(time) * 0.002 }
        }
        
		# 采集光束系列技能通用方法
		# harvest_level：最高采集等级 设置为0可禁用
		# speed：采集速度（单位：硬度/tick）
		# range：光束长度
		
		# 基础采集光束
        mine_ray_basic {
            x { 524 } y { 390 }
            range { 10 }
            harvest_level { 2 }
            speed(exp) { 0.2 * lerp(1, 2, exp) }
            consumption(exp) { lerp(15, 8, exp) }
            overload(exp) { lerp(230, 140, exp) }
            cooldown(exp) { lerp(60, 30, exp) }
            expincr { 0.0005 }
        }
        
		# 散射光束雨
        ray_barrage {
            x { 664 } y { 66 }
			# 没有射到Silbarn时的光束伤害
            plain_damage(exp) { lerp(25, 40, exp) }
			# 散射时每个光束的伤害
            scatter_damage(exp) { lerp(12, 20, exp) }
            consumption(exp) { lerp(450, 340, exp) }
            overload(exp) { lerp(375, 160, exp) }
            cooldown(exp) { 20 * lerp(5, 8, exp) }
            expincr { 0.005 }
        }
        
		# 喷气引擎
        jet_engine {
            x { 654 } y { 263 }
            damage(exp) { lerp(15, 35, exp) }
            cooldown(exp) { 18 * floor(lerp(6, 3, exp)) }
            consumption(exp) { lerp(170, 140, exp) }
            overload(exp) { lerp(66, 42, exp) }
            expincr { 0.004 }
        }
        
		# 专家采矿光束
        mine_ray_expert {
            x { 754 } y { 362 }
            range { 20 }
            harvest_level { 5 }
            speed(exp) { 0.5 * lerp(1, 2, exp) }
            consumption(exp) { lerp(25, 14, exp) }
            overload(exp) { lerp(300, 180, exp) }
            cooldown(exp) { lerp(60, 30, exp) }
            expincr { 0.0003 }
        }
        
		# 时运采矿光束
        mine_ray_luck {
            x { 946 } y { 401 }
            range { 20 }
            harvest_level { 5 }
            speed(exp) { 0.5 * lerp(1, 2, exp) }
            consumption(exp) { lerp(50, 30, exp) }
            overload(exp) { lerp(400, 300, exp) }
            cooldown(exp) { lerp(60, 30, exp) }
            expincr { 0.0003 }
        }
        
		# 巡航光束炮
        electron_missile {
            x { 891 } y { 120 }
            range(exp) { lerp(7, 12, exp) }
            consumption(exp) { lerp(20, 15, exp) }
			# 固有过载
            overload(exp) { lerp(2, 1.5, exp) }
			# 发射一发光束时过载
            overload_attacked(exp) { lerp(61, 32, exp)}
			# 发射一发光束时消耗
            consumption_attacked(exp) { lerp(270, 405, exp) }
            cooldown(ticks) { min(300, max(100, ticks)) }
			# 最大持续时间
            time_limit(exp) { 20 * lerp(7, 20, exp) }
            damage(exp) { lerp(14, 27, exp) }
            expincr { 0.001 } 
        }
    }
}
