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
package cn.academy.ability.developer;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.block.TileDeveloper;
import cn.annoreg.core.Registrant;

/**
 * @author WeAthFolD
 */
@Registrant
public class DeveloperBlock extends Developer {
	
	final TileDeveloper tile;

	public DeveloperBlock(TileDeveloper _tile) {
		super(_tile.getType());
		tile = _tile;
	}

	@Override
	public EntityPlayer getUser() {
		return tile.getUser();
	}

	@Override
	public boolean pullEnergy(double amt) {
		return tile.pullEnergy(amt) == amt;
	}

	@Override
	public double getEnergy() {
		return tile.getEnergy();
	}

}
