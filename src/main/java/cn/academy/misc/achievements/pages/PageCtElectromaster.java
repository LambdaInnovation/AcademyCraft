package cn.academy.misc.achievements.pages;

import cn.academy.ability.api.CategoryManager;
import cn.academy.misc.achievements.ItemAchievement;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchAbility;
import cn.academy.misc.achievements.aches.AchEvLevelChange;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.CatElectromaster;

/**
 * @author EAirPeter
 */
public final class PageCtElectromaster extends PageCategory<CatElectromaster> {

    private final ACAchievement aLv1;
    private final ACAchievement aLv2;
    private final ACAchievement aLv3;
    private final ACAchievement aLv4;
    private final ACAchievement aLv5;
    
    private final ACAchievement aArcGen;                    //Manual
    private final ACAchievement aAtCreeper;                 //Manual
    private final ACAchievement aMagnetic;                  //Manual
    private final ACAchievement aBodyIntensify;             //Manual
    //private final ACAchievement aIronSand;                //Manual
    private final ACAchievement aMineDetect;                //Manual
    private final ACAchievement aThunderBolt;          	    //Manual
    private final ACAchievement aRailgun;                   //Manual
    private final ACAchievement aThunderClap;               //Manual

    public PageCtElectromaster() {
        super(ModuleVanilla.electromaster);
        add(new ACAchievement[] {
            aLv1 = new AchEvLevelChange(1, category.currentCharging, "lv1", 0, 0, null),
            aLv2 = new AchEvLevelChange(2, category.magManip, "lv2", 2, 0, aLv1),
            aLv3 = new AchEvLevelChange(3, category.bodyIntensify, "lv3", 2, 2, aLv2),
            aLv4 = new AchEvLevelChange(4, category.railgun, "lv4", -2, 2, aLv3),
            aLv5 = new AchEvLevelChange(5, category.thunderClap, "lv5", -2, 0, aLv4),
        });
        add(new ACAchievement[] {
            aArcGen = new AchAbility(category.arcGen, -3, -1, null),
            //Icon pending
            aAtCreeper = new AchAbility(category, "attack_creeper", -3, 1,
                ItemAchievement.getStack("achievements/em_attack_creeper"), aArcGen),
            aMagnetic = new AchAbility(category.magMovement, -3, 3, aAtCreeper),
            aBodyIntensify = new AchAbility(category.bodyIntensify, -1, 3, aMagnetic),
            //aIronSand = new AchAbility(category.ironSand, 1, 3, null),
            aMineDetect = new AchAbility(category.mineDetect, 3, 3, aBodyIntensify),
            aThunderBolt = new AchAbility(category.thunderBolt, 3, 1, aMineDetect),
            aRailgun = new AchAbility(category.railgun, 3, -1, aThunderBolt),
            aThunderClap = new AchAbility(category.thunderClap, 1, -1, aRailgun),
        });
    }
    
}