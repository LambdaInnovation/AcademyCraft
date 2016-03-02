/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.generic.skill;

import cn.academy.ability.api.Skill;
import net.minecraftforge.common.MinecraftForge;

/**
 * Generic skill: Brain Course.
 * TODO rewrite
 * @author WeAthFolD
 */
public class SkillBrainCourse extends Skill {

    public SkillBrainCourse() {
        super("brain_course", 4);
        this.canControl = false;
        this.isGeneric = true;
        MinecraftForge.EVENT_BUS.register(this);
    }

}
