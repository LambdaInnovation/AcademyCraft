/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.passiveskills;

import cn.academy.ability.api.Skill;
import net.minecraftforge.common.MinecraftForge;

/**
 * Dummy placeholder. Impl at {@link cn.academy.vanilla.teleporter.util.TPSkillHelper}
 * @author WeAthFolD
 */
public class DimFoldingTheorem extends Skill {

    public static final DimFoldingTheorem instance = new DimFoldingTheorem();

    private DimFoldingTheorem() {
        super("dim_folding_theorem", 1);
        this.canControl = false;
    }

}
