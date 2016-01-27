/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.sound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

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
