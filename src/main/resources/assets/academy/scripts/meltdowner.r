ac {
    meltdowner {
        electron_bomb {
            x { 106 } y { 202 }
            damage(exp) { lerp(5, 10, exp) }
            consumption(exp) { lerp(35, 60, exp) }
            overload(exp) { lerp(16, 11, exp) }
            cooldown(exp) { lerp(20, 10, exp) }
            expincr { 0.005 }
        }
        
        rad_intensify {
            x { 212 } y { 315 }
            rate(exp) { lerp(1.4, 2, exp) }
        }
    
        scatter_bomb {
            x { 308 } y { 70 }
            damage(exp) { lerp(5, 9, exp) }
            consumption(exp) { lerp(3, 6, exp) }
            overload(exp) { lerp(100, 80, exp) }
            expincr { 0.001 }
        }
        
        light_shield {
            x { 391 } y { 266 }
            consumption(exp) { lerp(9, 4, exp) }
            overload(exp) { lerp(90, 60, exp) }
            cooldown(exp) { lerp(80, 60, exp) }
            
            absorb_consumption(exp) { lerp(50, 30, exp) }
            absorb_overload(exp) { lerp(20, 15, exp) }
            
            touch_damage(exp) { lerp(2, 6, exp) }
            absorb_damage(exp) { lerp(8, 25, exp) }
            
            expincr { 0.001 }
        }
        
        meltdowner {
            x { 488 } y { 166 }
            range(exp) { lerp(2, 4.5, exp) }
            energy(exp) { lerp(600, 1300, exp) }
            damage(exp) { lerp(18, 45, exp) }
            overload(exp) { lerp(200, 170, exp) }
            consumption(exp) { 40 * lerp(10, 13, exp) }
            cooldown(exp) { 20 * lerp(15, 6, exp) }
            expincr { 0.002 }
        }
        
        mine_ray_basic {
            x { 524 } y { 390 }
            range { 10 }
            harvest_level { 2 }
            speed(exp) { 0.2 * lerp(1, 2, exp) }
            consumption(exp) { lerp(15, 20, exp) }
            overload(exp) { lerp(150, 70, exp) }
            cooldown(exp) { lerp(60, 30, exp) }
            expincr { 0.0005 }
        }
        
        ray_barrage {
            x { 664 } y { 66 }
            plain_damage(exp) { lerp(25, 40, exp) }
            scatter_damage(exp) { lerp(16, 24, exp) }
            consumption(exp) { lerp(300, 200, exp) }
            overload(exp) { lerp(350, 300, exp) }
            cooldown(exp) { 20 * lerp(5, 8, exp) }
            expincr { 0.005 }
        }
        
        jet_engine {
            x { 654 } y { 263 }
            damage(exp) { lerp(20, 30, exp) }
            cooldown(exp) { 18 * floor(lerp(6, 3, exp)) }
            consumption(exp) { 1.5 * lerp(170, 140, exp) }
            overload(exp) { 1.3 * lerp(60, 50, exp) }
            expincr { 0.004 }
        }
        
        mine_ray_expert {
            x { 754 } y { 362 }
            range { 20 }
            harvest_level { 5 }
            speed(exp) { 0.5 * lerp(1, 2, exp) }
            consumption(exp) { lerp(18, 12, exp) }
            overload(exp) { lerp(200, 110, exp) }
            cooldown(exp) { lerp(60, 30, exp) }
            expincr { 0.0003 }
        }
        
        mine_ray_luck {
            x { 946 } y { 401 }
            range { 20 }
            harvest_level { 5 }
            speed(exp) { 0.5 * lerp(1, 2, exp) }
            consumption(exp) { lerp(20, 15, exp) }
            overload(exp) { lerp(250, 160, exp) }
            cooldown(exp) { lerp(60, 30, exp) }
            expincr { 0.0003 }
        }
        
        electron_missile {
            x { 891 } y { 120 }
            range(exp) { lerp(7, 12, exp) }
            consumption(exp) { lerp(20, 15, exp) }
            overload(exp) { lerp(2, 1.5, exp) }
            overload_attacked(exp) { lerp(50, 35, exp)}
            consumption_attacked(exp) { lerp(300, 500, exp) }
            cooldown(ticks) { min(300, max(100, ticks)) }
            time_limit(exp) { 20 * lerp(7, 20, exp) }
            damage(exp) { lerp(14, 22, exp) }
            expincr { 0.001 } 
        }
    }
}
