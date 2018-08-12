package cn.academy.achievement.pages;

import cn.academy.achievement.aches.ACAchievement;
import cn.academy.achievement.aches.AchAbility;
import cn.academy.achievement.aches.AchEvLevelChange;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.vecmanip.CatVecManip$;
import cn.academy.ability.vanilla.vecmanip.skill.*;

/**
 * @author KSkun
 */
public class PageCtVecmanip extends PageCategory<CatVecManip$> {

    private final ACAchievement aLv1;
    private final ACAchievement aLv2;
    private final ACAchievement aLv3;
    private final ACAchievement aLv4;
    private final ACAchievement aLv5;

    private final ACAchievement aGroundshock;
    private final ACAchievement aDirBlast;
    private final ACAchievement aStormWing;
    private final ACAchievement aBloodRetro;
    private final ACAchievement aVecReflection;

    public PageCtVecmanip() {
        super(VanillaCategories.vecManip);
        add(new ACAchievement[] {
                aLv1 = new AchEvLevelChange<CatVecManip$>(1, DirectedShock$.MODULE$, "lv1", 0, 0, null),
                aLv2 = new AchEvLevelChange<CatVecManip$>(2, VecAccel$.MODULE$, "lv2", 2, 0, aLv1),
                aLv3 = new AchEvLevelChange<CatVecManip$>(3, DirectedBlastwave$.MODULE$, "lv3", 2, 2, aLv2),
                aLv4 = new AchEvLevelChange<CatVecManip$>(4, BloodRetrograde$.MODULE$, "lv4", -2, 2, aLv3),
                aLv5 = new AchEvLevelChange<CatVecManip$>(5, PlasmaCannon$.MODULE$, "lv5", -2, 0, aLv4)
        });
        add(new ACAchievement[] {
                aGroundshock = new AchAbility<CatVecManip$>(Groundshock$.MODULE$, -3, -1, null),
                aDirBlast = new AchAbility<CatVecManip$>(DirectedBlastwave$.MODULE$, -3, 1, aGroundshock),
                aStormWing = new AchAbility<CatVecManip$>(StormWing$.MODULE$, -3, 3, aDirBlast),
                aBloodRetro = new AchAbility<CatVecManip$>(BloodRetrograde$.MODULE$, -1, 3, aStormWing),
                aVecReflection = new AchAbility<CatVecManip$>(VecReflection$.MODULE$, 1, 3, aBloodRetro)
        });
    }
}