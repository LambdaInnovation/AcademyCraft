package cn.academy.ability.vanilla.vecmanip;

import cn.academy.ability.Category;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.vecmanip.skill.*;

public class CatVecManip extends Category {

    public CatVecManip() {
        super("vecmanip");
        setColorStyle(0,0,0);

        DirectedShock.setPosition(16, 45);
        Groundshock.setPosition(64, 85);
        VecAccel.setPosition(76, 40);
        VecDeviation.setPosition(145, 53);
        DirectedBlastwave.setPosition(136, 80);
        StormWing.setPosition(130, 20);
        BloodRetrograde.setPosition(204, 83);
        VecReflection.setPosition(210, 50);
        PlasmaCannon.setPosition(175, 14);

        // Level 1
        addSkill(DirectedShock$.MODULE$);
        addSkill(Groundshock$.MODULE$);

        // 2
        addSkill(VecAccel$.MODULE$);
        addSkill(VecDeviation$.MODULE$);

        // 3
        addSkill(DirectedBlastwave$.MODULE$);
        addSkill(StormWing$.MODULE$);

        // 4
        addSkill(BloodRetrograde$.MODULE$);
        addSkill(VecReflection$.MODULE$);

        // 5
        addSkill(PlasmaCannon$.MODULE$);

        Groundshock.setParent(DirectedShock$.MODULE$);
        VecAccel.setParent(DirectedShock$.MODULE$);
        VecDeviation.setParent(VecAccel$.MODULE$);
        DirectedBlastwave.setParent(Groundshock$.MODULE$);
        StormWing.setParent(VecAccel$.MODULE$);
        BloodRetrograde.setParent(DirectedBlastwave$.MODULE$);
        VecReflection.setParent(VecDeviation$.MODULE$);
        PlasmaCannon.setParent(StormWing$.MODULE$);

        VanillaCategories.addGenericSkills(this);
    }
}