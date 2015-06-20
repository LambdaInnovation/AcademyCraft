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
                0.001 * maxcp * lerp(0.6, 1, cp / maxcp)
            }
            
            overload_cooldown { 20 } # 技能使用后，过载恢复开始的冷却时间。
            overload_recover_speed(o, maxo) { # 过载的恢复速度。 参数：当前过载，最大过载
                0.02 * maxo * lerp(1, 0.75, o / maxo)
            }
            
            # 在过载时CP和过载消耗（增量）所乘的倍数。
            overload_cp_mul { 2 }
            overload_o_mul { 2 }
            
            # 各个等级初始CP值
            init_cp(level) {
                switch(level) {
                    0: 1800;
                    1: 2800;
                    2: 4000;
                    3: 5800;
                    4: 8000
                }
            }
            
            # 各个等级初始过载值
            init_overload(level) {
                switch(level) {
                    0: 100;
                    1: 150;
                    2: 240;
                    3: 350;
                    4: 500
                }
            }
        }
    }
}