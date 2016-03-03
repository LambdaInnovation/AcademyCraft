/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.develop.condition;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.core.client.Resources;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 */
public class DevConditionAnySkillOfLevel implements IDevCondition {
    
    final int level;

    final ResourceLocation res;

    public DevConditionAnySkillOfLevel(int _level) {
        level = _level;
        res = Resources.getTexture("abilities/condition/any" + level);
    }
    
    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        if(!data.hasCategory())
            return false;

        for(Skill s : data.getCategory().getSkillsOfLevel(level)) {
            if(data.isSkillLearned(s))
                return true;
        }
        return false;
    }

    @Override
    public ResourceLocation getIcon() {
        return res;
    }

    @Override
    public String getHintText() {
        return StatCollector.translateToLocalFormatted("ac.skill_tree.anyskill", level);
    }

}
