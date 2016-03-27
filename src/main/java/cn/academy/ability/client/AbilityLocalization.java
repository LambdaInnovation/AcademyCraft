/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client;

import cn.academy.ability.api.data.CPData;
import cn.academy.ability.develop.DeveloperType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

/**
 * TODO Rework this strange class
 */
public enum AbilityLocalization {
    instance;
    
    public String levelDesc(int level) {
        return StatCollector.translateToLocal("ac.ability.level" + level);
    }
    
    public String acquiredProg(float skillexp) {
        return StatCollector.translateToLocalFormatted("ac.skill_tree.acquireprog", String.format("%.1f", skillexp * 100));
    }
    
    public String upgradeTo(int level) {
        return local("uplevel", levelDesc(level));
    }
    
    public String notAcquired() {
        return local("not_acquired");
    }
    
    public String acquire() {
        return local("acquire");
    }
    
    public String required() {
        return local("required");
    }
    
    public String unknownSkill() {
        return local("unknown_skill");
    }
    
    public String unknown() {
        return local("unknown");
    }
    
    public String learnSkill() {
        return local("learn_skill");
    }
    
    public String machineType(DeveloperType type) {
        return local("type_" + type.toString().toLowerCase());
    }
    
    public String estmCons(double amt) {
        return local("consumption", amt);
    }
    
    public String progress(double amt) {
        return local("progress", String.format("%.1f", amt * 100));
    }
    
    public String progressAborted() {
        return local("aborted");
    }
    
    public String successful() {
        return local("successful");
    }
    
    public String ok() {
        return local("ok");
    }
    
    public String abort() {
        return local("abort");
    }
    
    public String aborted() {
        return local("aborted");
    }
    
    public String energyDesc(double energy, double maxEnergy) {
        return String.format("%.0f/%.0fIF", energy, maxEnergy);
    }
    
    public String levelPrg(EntityPlayer player) {
        return local("level_prg", String.format("%.2f%%", CPData.get(player).getLevelProgress() * 100));
    }
    
    public String local(String key) {
        return StatCollector.translateToLocal("ac.skill_tree." + key);
    }
    
    public String local(String key, Object ...args) {
        return StatCollector.translateToLocalFormatted("ac.skill_tree." + key, args);
    }
    
}
