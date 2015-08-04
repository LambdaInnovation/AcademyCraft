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

import java.util.Random;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import cn.academy.core.command.ACCommand;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.raytrace.Raytrace;

/**
 * @author WeAthFolD
 */
@Registrant
@RegCommand
public class CommandGenChest extends ACCommand {
	
	Random rnd = new Random();

	public CommandGenChest() {
		super("g");
	}

	@Override
	public void processCommand(ICommandSender ics, String[] str) {
		EntityPlayer player = this.getCommandSenderAsPlayer(ics);
		String cat = str[0];
		ChestGenHooks hooks = ChestGenHooks.getInfo(cat);
		
		MovingObjectPosition result = Raytrace.traceLiving(player, 10, EntitySelectors.nothing);
		int x, y, z;
		if(result != null) {
			x = result.blockX;
			y = result.blockY;
			z = result.blockZ;
		} else {
			Motion3D mo = new Motion3D(player, true).move(5);
			x = (int) mo.px;
			y = (int) mo.py;
			z = (int) mo.pz;
		}
		
		for(int i = x - 10; i <= x + 10; i += 2) {
			player.worldObj.setBlock(i, y, z, Blocks.chest);
			TileEntityChest tile = (TileEntityChest) player.worldObj.getTileEntity(i, y, z);
			WeightedRandomChestContent.generateChestContents(rnd, hooks.getItems(rnd), tile, 10);
		}
	}

}
