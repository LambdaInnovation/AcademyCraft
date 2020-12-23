package cn.academy.ability.vanilla.teleporter;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.teleporter.passiveskill.*;
import cn.academy.ability.vanilla.teleporter.skill.*;

/**
 * @author WeAthFolD
 */
public class CatTeleporter extends Category {

    public static final Skill
        dimFolding = DimFoldingTheorem.instance,
        spaceFluct = SpaceFluctuation.instance,
        markTP = MarkTeleport$.MODULE$,
        locTP = LocationTeleport$.MODULE$,
        penetrateTP = PenetrateTeleport.instance,
        threateningTP = ThreateningTeleport.instance,
        shiftTP = ShiftTeleport.instance,
        fleshRipping = FleshRipping.instance,
        flashing = Flashing.instance;

    public CatTeleporter() {
        super("teleporter");
        setColorStyle(164, 164, 164, 145);

        threateningTP.setPosition(14, 42);
        dimFolding.setPosition(50, 75);
        penetrateTP.setPosition(60, 46);
        markTP.setPosition(70, 16);
        fleshRipping.setPosition(130, 12);
        locTP.setPosition(118, 50);
        shiftTP.setPosition(175, 47);
        spaceFluct.setPosition(160, 80);
        flashing.setPosition(220, 20);

        // Lv1
        this.addSkill(threateningTP);
        this.addSkill(dimFolding);

        // Lv2
        this.addSkill(penetrateTP);
        this.addSkill(markTP);

        // Lv3
        this.addSkill(fleshRipping);
        this.addSkill(locTP);

        // Lv4
        this.addSkill(shiftTP);
        this.addSkill(spaceFluct);

        // Lv5
        this.addSkill(flashing);

        VanillaCategories.addGenericSkills(this);

        // Assign deps
        dimFolding.setParent(threateningTP, 0.2f);
        penetrateTP.setParent(threateningTP, 0.5f);
        markTP.setParent(threateningTP, 0.4f);
        fleshRipping.setParent(markTP, 0.5f);
        fleshRipping.addSkillDep(penetrateTP, 0.5f);
        locTP.setParent(penetrateTP, 0.8f);
        locTP.addSkillDep(markTP, 0.8f);
        shiftTP.setParent(locTP, 0.5f);
        spaceFluct.setParent(shiftTP, 0.0f);
        flashing.setParent(shiftTP, 0.8f);
    }

}