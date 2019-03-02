package cn.academy.ability.vanilla.teleporter.passiveskill;

import cn.academy.ability.Skill;

/**
 * Dummy placeholder. Impl at {@link cn.academy.ability.vanilla.teleporter.util.TPSkillHelper}
 *
 * @author WeAthFolD
 */
public class DimFoldingTheorem extends Skill {
    public static final DimFoldingTheorem instance = new DimFoldingTheorem();
    
    public DimFoldingTheorem()
    {
        super("dim_folding_theorem", 1);
        canControl = false;
    }
}
