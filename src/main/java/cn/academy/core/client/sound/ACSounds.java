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
package cn.academy.core.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Generic sound playing utils.
 * @author WeAthFolD
 */
public class ACSounds {
	
	@SideOnly(Side.CLIENT)
	public static void playClient(Entity target, String name, float volume) {
		playClient(new FollowEntitySound(target, name).setVolume(volume));
	}
	
	@SideOnly(Side.CLIENT)
	public static void playClient(World world, double x, double y, double z, String name, float vol, float pitch) {
		world.playSound(x, y, z, "academy:" + name, vol, pitch, false);
	}
	
	@SideOnly(Side.CLIENT)
	public static void playClient(ISound sound) {
		Minecraft.getMinecraft().getSoundHandler().playSound(sound);
	}

}
