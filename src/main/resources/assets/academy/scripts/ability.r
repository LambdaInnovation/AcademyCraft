ac {
    ability {
        learning {
            # 学习一个技能的消耗。 返回值：刺激数（被约到整数），参数：技能等级
            learning_cost(skillLevel) {
                3 + skillLevel * 1.5
            }
        }
        
        cp {
            recover_cooldown { 15 } # 技能使用后，CP恢复开始的冷却时间。
            recover_speed(cp, maxcp) { # CP的恢复速度。参数：当前CP，最大CP
                0.0001 * maxcp * lerp(1, 2, cp / maxcp)
            }
            
            overload_cooldown { 20 } # 技能使用后，过载恢复开始的冷却时间。
            overload_recover_speed(o, maxo) { # 过载的恢复速度。 参数：当前过载，最大过载
                max(0.002 * maxo, 
                    0.01 * maxo * lerp(1, 0.5, o / (maxo * 2)))
            }
            
            # 在过载时CP和过载消耗（增量）所乘的倍数。
            overload_cp_mul { 2.5 }
            overload_o_mul { 1.5 }
            
            # 各个等级初始CP值
            init_cp(level) {
                switch(level) {
					0: 1800;
                    1: 1800;
                    2: 2800;
                    3: 4000;
                    4: 5800;
                    5: 8000
                }
            }
            
            # 各个等级初始过载值
            init_overload(level) {
                switch(level) {
					0: 100;
                    1: 100;
                    2: 150;
                    3: 240;
                    4: 350;
                    5: 500
                }
            }
        }
    }
}