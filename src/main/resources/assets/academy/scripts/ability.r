ac {
    ability {
        learning {
            learning_cost(skillLevel) { # stimulations.
                3 + skillLevel * 1.5
            }
        }
        
        cp {
            recover_cooldown { 15 } # tick
            recover_speed(cp, maxcp) { # CP/tick
                0.02 * maxcp * lerp(0.6, 1, cp / maxcp)
            }
            
            overload_cooldown { 20 } # tick
            overload_recover_speed(o, maxo) {
                0.1 * maxo * lerp(1, 0.5, o / maxo)
            }
            
            # How many times does the value change increase when overloaded
            overload_cp_mul { 2 }
            overload_o_mul { 2 }
            
            init_cp(level) {
                switch(level) {
                    0: 1800;
                    1: 2800;
                    2: 4000;
                    3: 5800;
                    4: 8000
                }
            }
            
            
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