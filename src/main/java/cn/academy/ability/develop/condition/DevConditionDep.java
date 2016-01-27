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
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class DevConditionDep implements IDevCondition {
    
    public final Skill dependency;
    public final float requiredExp;
    
    public DevConditionDep(Skill _dep) {
        this(_dep, 0.0f);
    }
    
    public DevConditionDep(Skill _dep, float _requiredExp) {
        dependency = _dep;
        requiredExp = _requiredExp;
    }

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return data.isSkillLearned(dependency) &&
                data.getSkillExp(dependency) >= requiredExp;
    }

    @Override
    public ResourceLocation getIcon() {
        return dependency.getHintIcon();
    }

    @Override
    public String getHintText() {
        return dependency.getDisplayName() + String.format(": %.0f%%", requiredExp * 100);
    }

}
