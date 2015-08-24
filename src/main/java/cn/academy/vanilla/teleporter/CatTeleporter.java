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
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.teleporter.skills.*;
import cn.academy.vanilla.teleporter.passiveskills.*;

/**
 * @author WeAthFolD
 */
public class CatTeleporter extends Category {
	
	public static DimFoldingTheorem dimFolding;
	
	public static SpaceFluctuation spaceFluct;

	public static MarkTeleport markTP;
	
	public static LocationTeleport locTP;
	
	public static PenetrateTeleport penetrateTP;
	
	public static ThreateningTeleport threateningTP;
	
	public static ShiftTeleport shiftTP;
	
	public static FleshRipping fleshRipping;
	
	public static Flashing flashing;
	
	public CatTeleporter() {
		super("teleporter");
		colorStyle.setColor4i(164, 164, 164, 145);
		
		// Lv1
		this.addSkill(threateningTP = new ThreateningTeleport());
		this.addSkill(dimFolding = new DimFoldingTheorem());
		
		// Lv2
		this.addSkill(penetrateTP = new PenetrateTeleport());
		this.addSkill(markTP = new MarkTeleport());
		
		// Lv3
		this.addSkill(fleshRipping = new FleshRipping());
		this.addSkill(locTP = new LocationTeleport());
		
		// Lv4
		this.addSkill(shiftTP = new ShiftTeleport());
		this.addSkill(spaceFluct = new SpaceFluctuation());
		
		// Lv5
		this.addSkill(flashing = new Flashing());
		
		ModuleVanilla.addGenericSkills(this);
		
		// Assign deps
		dimFolding.setParent(threateningTP, 0.8f);
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
