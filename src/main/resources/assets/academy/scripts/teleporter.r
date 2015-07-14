ac {
    teleporter {
        location_teleport {
            # 单次传送消耗。 参数：(距离, 跨维度惩罚(1 or 2), 技能经验)
            consumption(dist, dimfac, exp) { 
                lerp(200, 150, exp) * dimfac * max(8, sqrt( min(800, dist) ))
            }
        }
        
        penetrate_teleport {
            cooldown(exp) { lerp(120, 60, exp) }
            max_distance(exp) { lerp(10, 35, exp) }
            consumption(exp) { lerp(20, 14, exp) }
            overload(exp) { lerp(80, 50, exp) }
        }
    }
}