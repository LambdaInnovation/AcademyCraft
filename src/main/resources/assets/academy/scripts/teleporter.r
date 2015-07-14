ac {
    teleporter {
        location_teleport {
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
        }
        
        penetrate_teleport {
            cooldown(exp) { lerp(120, 60, exp) }
            max_distance(exp) { lerp(10, 35, exp) }
            consumption(exp) { lerp(20, 14, exp) }
            overload(exp) { lerp(80, 50, exp) }
        }
    }
}