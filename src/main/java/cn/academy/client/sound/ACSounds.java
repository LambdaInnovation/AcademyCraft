package cn.academy.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Generic sound playing utils.
 * @author WeAthFolD
 */
public class ACSounds {
    
    @SideOnly(Side.CLIENT)
    public static void playClient(Entity target, String name, SoundCategory category, float volume) {
        playClient(new FollowEntitySound(target, name, category).setVolume(volume));
    }
    
    @SideOnly(Side.CLIENT)
    public static void playClient(World world, double x, double y, double z, String name, SoundCategory category, float vol, float pitch) {
        world.playSound(x, y, z,
            new SoundEvent(new ResourceLocation("academy", name)),
            category,
            vol, pitch, false
        );
    }
    
    @SideOnly(Side.CLIENT)
    public static void playClient(ISound sound) {
        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
    }

}