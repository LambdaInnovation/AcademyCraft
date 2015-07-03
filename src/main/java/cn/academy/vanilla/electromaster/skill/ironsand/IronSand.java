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
package cn.academy.vanilla.electromaster.skill.ironsand;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.SpecialSkill;

/**
 * Iron sand manipulation
 * @author WeAthFolD
 */
public class IronSand extends SpecialSkill {

	public IronSand() {
		super("iron_sand", 4);
		
		addSubSkill(new ISSword());
		addSubSkill(new ISWhip());
		addSubSkill(new ISStorm());
		addSubSkill(new ISCone());
	}
	
	/**
	 * Called when player started SpecialSkill. Validate at SERVER to proceed.
	 * @param player
	 * @return
	 */
	@Override
	public boolean validateExecution(EntityPlayer player) {
		System.out.println("ValidateExecution");
		return true;
	}
	
	/**
	 * Called in both client and server when executing the SpecialSkill.
	 */
	@Override
	public void execute(EntityPlayer player) {
		System.out.println("execute " + player.worldObj.isRemote);
	}

}
