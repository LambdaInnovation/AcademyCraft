/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.lambdalib.util.generic.MathUtils;

/**
 * Just a placeholder. implementation is in MDDamageHelper.
 * @author WeAthFolD
 */
public class RadiationIntensify extends Skill {

    public static final RadiationIntensify instance = new RadiationIntensify();

    private RadiationIntensify() {
        super("rad_intensify", 1);
        this.canControl = false;
        this.expCustomized = true;
    }
    
    @Override
    public float getSkillExp(AbilityData data) {
        CPData cpData = CPData.get(data.getEntity());
        return MathUtils.clampf(0, 1, cpData.getMaxCP() / CPData.get(data.getEntity()).getInitCP(5));
    }

    public float getRate(AbilityData data) {
        return MathUtils.lerpf(1.4f, 1.8f, data.getSkillExp(this));
    }
    
}
