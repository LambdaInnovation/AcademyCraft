package cn.academy.ability;

import cn.academy.ability.develop.DeveloperType;
import net.minecraft.client.resources.I18n;

/**
 * This class should be used only in client part.
 */
public enum AbilityLocalization {
    instance;
    
    public String levelDesc(int level) {
        return I18n.format("ac.ability.level" + level);
    }
    
    public String machineType(DeveloperType type) {
        return local("type_" + type.toString().toLowerCase());
    }
    
    public String local(String key) {
        return I18n.format("ac.skill_tree." + key);
    }
    
    public String local(String key, Object ...args) {
        return I18n.format("ac.skill_tree." + key, args);
    }
    
}