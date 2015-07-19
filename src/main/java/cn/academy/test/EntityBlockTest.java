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
package cn.academy.test;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityBeacon;

import org.lwjgl.input.Keyboard;

import cn.academy.core.entity.EntityBlock;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.liutils.util.helper.KeyHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
@Registrant
public class EntityBlockTest {
	
	@RegACKeyHandler(name = "IA", defaultKey = Keyboard.KEY_L)
	public static KeyHandler key = new KeyHandler() {
		@Override
		public void onKeyDown() {
			spawnEntityAtServer(getPlayer(), 
				Keyboard.isKeyDown(Keyboard.KEY_LCONTROL));
		}
	};
	
	@RegNetworkCall(side = Side.SERVER)
	public static void spawnEntityAtServer(@Instance EntityPlayer player, @Data Boolean option) {
		EntityBlock entity = new EntityBlock(player.worldObj);
		if(option) {
			entity.setBlock(Blocks.sandstone);
		} else {
			entity.setBlock(Blocks.beacon);
			entity.setTileEntity(new TileEntityBeacon());
		}
		
		entity.setPosition(player.posX, player.posY, player.posZ);
		
		player.worldObj.spawnEntityInWorld(entity);
	}
	
}
