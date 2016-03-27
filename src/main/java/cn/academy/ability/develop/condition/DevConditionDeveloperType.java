/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.develop.condition;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.client.AbilityLocalization;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class DevConditionDeveloperType implements IDevCondition {
    
    final DeveloperType type;
    
    public DevConditionDeveloperType(DeveloperType _type) {
        type = _type;
    }

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return developer.getType().ordinal() >= type.ordinal();
    }
    
    @Override
    public ResourceLocation getIcon() {
        return type.texture;
    }

    @Override
    public String getHintText() {
        return AbilityLocalization.instance.machineType(type);
    }

}
