package cn.academy.vanilla;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModuleSoundEvent
{
    static{
        MinecraftForge.EVENT_BUS.register(new ModuleSoundEvent());
    }
    public static SoundEvent silbarn_sound = new SoundEvent(new ResourceLocation( "random.bow"));
    public static SoundEvent md_ray_small = new SoundEvent(new ResourceLocation( "academy","md.ray_small"));
    public static SoundEvent silbarn_light = new SoundEvent(new ResourceLocation( "academy","entity.silbarn_light"));
    public static SoundEvent silbarn_heavy = new SoundEvent(new ResourceLocation( "academy","entity.silbarn_heavy"));

    @SubscribeEvent
    public void onRegisterSound(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(silbarn_sound, md_ray_small);
    }
}
