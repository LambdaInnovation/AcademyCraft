/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.teleporter;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.teleporter.skills.*;
import cn.academy.vanilla.teleporter.passiveskills.*;

/**
 * @author WeAthFolD
 */
public class CatTeleporter extends Category {

	public static final Skill
		dimFolding = DimFoldingTheorem.instance,
		spaceFluct = SpaceFluctuation.instance,
		markTP = MarkTeleport.instance,
		locTP = LocationTeleport.instance,
		penetrateTP = PenetrateTeleport.instance,
		threateningTP = ThreateningTeleport.instance,
		shiftTP = ShiftTeleport.instance,
		fleshRipping = FleshRipping.instance,
		flashing = Flashing.instance;

	public CatTeleporter() {
		super("teleporter");
		colorStyle.setColor4i(164, 164, 164, 145);

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

		ModuleVanilla.addGenericSkills(this);

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
