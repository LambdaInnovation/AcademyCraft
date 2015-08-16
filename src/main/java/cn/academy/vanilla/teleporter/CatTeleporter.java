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
	
	public static DimFoldingTheoreom dimFolding;
	
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
		
		this.addSkill(markTP = new MarkTeleport());
		this.addSkill(dimFolding = new DimFoldingTheoreom());
		
		this.addSkill(locTP = new LocationTeleport());
		this.addSkill(penetrateTP = new PenetrateTeleport());
		this.addSkill(threateningTP = new ThreateningTeleport());
		this.addSkill(shiftTP = new ShiftTeleport());
		this.addSkill(fleshRipping = new FleshRipping());
		
		this.addSkill(spaceFluct = new SpaceFluctuation());
		this.addSkill(flashing = new Flashing());
		
		ModuleVanilla.addGenericSkills(this);
		
		// Moving page
		markTP.guiPosition.set(100, 100);
		threateningTP.guiPosition.set(200, 100);
		flashing.guiPosition.set(350, 300);
		
		// Jumping pave
		locTP.guiPosition.set(100, 100);
		penetrateTP.guiPosition.set(200, 100);
		shiftTP.guiPosition.set(300, 200);
		
		// Passive page
		dimFolding.guiPosition.set(500, 100);
		spaceFluct.guiPosition.set(600, 200);
	}

}
