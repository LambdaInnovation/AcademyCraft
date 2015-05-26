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
package cn.academy.energy.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * @author WeAthFolD
 *
 */
public class ContainerImagFusor extends Container {

	public final TileImagFusor tile;
	public final EntityPlayer player;
	
	public ContainerImagFusor(TileImagFusor _tile, EntityPlayer _player) {
		tile = _tile;
		player = _player;
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer p_75145_1_) {
		return player.getDistanceSq(tile.xCoord + .5, tile.yCoord + .5, tile.zCoord + .5) < 64;
	}

}
