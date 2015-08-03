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

/**
 * @author WeAthFolD
 */
public class CatTeleporter extends Category {

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
		
		this.defineTypes("default", "passive");
		
		this.addSkill("default", markTP = new MarkTeleport());
		this.addSkill("default", locTP = new LocationTeleport());
		this.addSkill("default", penetrateTP = new PenetrateTeleport());
		this.addSkill("default", threateningTP = new ThreateningTeleport());
		this.addSkill("default", shiftTP = new ShiftTeleport());
		this.addSkill("default", fleshRipping = new FleshRipping());
		this.addSkill("default", flashing = new Flashing());
		
		ModuleVanilla.addGenericSkills(this);
	}

}
