package cn.academy.ability.vanilla.teleporter.passiveskill;

import cn.academy.ability.Skill;

/**
 * Dummy placeholder. Impl at {@link cn.academy.ability.vanilla.teleporter.util.TPSkillHelper}
 *
 * @author WeAthFolD
 */
public class SpaceFluctuation  extends Skill
{
    public static final SpaceFluctuation instance = new SpaceFluctuation();
    public SpaceFluctuation()
    {
        super("space_fluct", 4);
        this.canControl = false;
    }
}
