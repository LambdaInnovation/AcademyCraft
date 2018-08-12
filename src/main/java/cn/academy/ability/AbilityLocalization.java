package cn.academy.ability;

import cn.academy.ability.develop.DeveloperType;
import net.minecraft.util.StatCollector;

public enum AbilityLocalization {
    instance;
    
    public String levelDesc(int level) {
        return StatCollector.translateToLocal("ac.ability.level" + level);
    }
    
    public String machineType(DeveloperType type) {
        return local("type_" + type.toString().toLowerCase());
    }
    
    public String local(String key) {
        return StatCollector.translateToLocal("ac.skill_tree." + key);
    }
    
    public String local(String key, Object ...args) {
        return StatCollector.translateToLocalFormatted("ac.skill_tree." + key, args);
    }
    
}