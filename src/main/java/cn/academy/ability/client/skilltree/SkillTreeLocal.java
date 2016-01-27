/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.skilltree;

import cn.academy.ability.api.data.CPData;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

public class SkillTreeLocal {
    
    public static String levelDesc(int level) {
        return StatCollector.translateToLocal("ac.ability.level" + level);
    }
    
    public static String acquiredProg(float skillexp) {
        // FUCK YOU MOJANG WHY IN THE HELL DO YOU REPLACE %.0f to %s ARE YOU ASSHOLE OR SOMETHING???
        return StatCollector.translateToLocalFormatted("ac.skill_tree.acquireprog", String.format("%.1f", skillexp * 100));
    }
    
    public static String upgradeTo(int level) {
        return local("uplevel", levelDesc(level));
    }
    
    public static String notAcquired() {
        return local("not_acquired");
    }
    
    public static String acquire() {
        return local("acquire");
    }
    
    public static String required() {
        return local("required");
    }
    
    public static String unknownSkill() {
        return local("unknown_skill");
    }
    
    public static String unknown() {
        return local("unknown");
    }
    
    public static String learnSkill() {
        return local("learn_skill");
    }
    
    public static String machineType(DeveloperType type) {
        return local("type_" + type.toString().toLowerCase());
    }
    
    public static String estmCons(double amt) {
        return local("consumption", amt);
    }
    
    public static String progress(double amt) {
        return local("progress", String.format("%.1f", amt * 100));
    }
    
    public static String progressAborted() {
        return local("aborted");
    }
    
    public static String successful() {
        return local("successful");
    }
    
    public static String ok() {
        return local("ok");
    }
    
    public static String abort() {
        return local("abort");
    }
    
    public static String aborted() {
        return local("aborted");
    }
    
    public static String energyDesc(double energy, double maxEnergy) {
        return String.format("%.0f/%.0fIF", energy, maxEnergy);
    }
    
    public static String levelPrg(EntityPlayer player) {
        return local("level_prg", String.format("%.2f%%", CPData.get(player).getLevelProgress() * 100));
    }
    
    public static String local(String key) {
        return StatCollector.translateToLocal("ac.skill_tree." + key);
    }
    
    public static String local(String key, Object ...args) {
        return StatCollector.translateToLocalFormatted("ac.skill_tree." + key, args);
    }
    
}
