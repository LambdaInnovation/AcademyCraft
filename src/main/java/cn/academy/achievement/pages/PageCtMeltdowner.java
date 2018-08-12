package cn.academy.achievement.pages;

import cn.academy.item.ItemAchievement;
import cn.academy.achievement.aches.ACAchievement;
import cn.academy.achievement.aches.AchAbility;
import cn.academy.achievement.aches.AchEvLevelChange;
import cn.academy.achievement.aches.AchEvSkillLearn;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.meltdowner.CatMeltdowner;

/**
 * @author EAirPeter
 */
public final class PageCtMeltdowner extends PageCategory<CatMeltdowner> {

    private final ACAchievement aLv1;
    private final ACAchievement aLv2;
    private final ACAchievement aLv3;
    private final ACAchievement aLv4;
    private final ACAchievement aLv5;

    private final ACAchievement aRadIntensify;
    private final ACAchievement aLightShield;
    private final ACAchievement aMeltdowner;
    private final ACAchievement aMineRay;
    private final ACAchievement aJetEngine;                //Manual
    private final ACAchievement aElectronMissile;
    
    public PageCtMeltdowner() {
        super(VanillaCategories.meltdowner);
        add(new ACAchievement[] {
            aLv1 = new AchEvLevelChange(1, CatMeltdowner.electronBomb, "lv1", 0, 0, null),
            aLv2 = new AchEvLevelChange(2, CatMeltdowner.lightShield, "lv2", 2, 0, aLv1),
            aLv3 = new AchEvLevelChange(3, CatMeltdowner.meltdowner, "lv3", 2, 2, aLv2),
            aLv4 = new AchEvLevelChange(4, CatMeltdowner.jetEngine, "lv4", -2, 2, aLv3),
            aLv5 = new AchEvLevelChange(5, CatMeltdowner.electronMissile, "lv5", -2, 0, aLv4),
        });
        add(new ACAchievement[] {
            aRadIntensify = new AchEvSkillLearn(CatMeltdowner.radIntensify, -3, -1, null),
            aLightShield = new AchEvSkillLearn(CatMeltdowner.lightShield, -3, 1, aRadIntensify),
            aMeltdowner = new AchEvSkillLearn(CatMeltdowner.meltdowner, -3, 3, aLightShield),
            aMineRay = new AchEvSkillLearn(CatMeltdowner.mineRayBasic, "mine_ray", -1, 3,
                ItemAchievement.getStack(CatMeltdowner.mineRayLuck.getHintIcon()), aMeltdowner),
            aJetEngine = new AchAbility(CatMeltdowner.jetEngine, 1, 3, aMineRay),
            aElectronMissile = new AchEvSkillLearn(CatMeltdowner.electronMissile, 3, 3, aJetEngine),
        });
    }

}