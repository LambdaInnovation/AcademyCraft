package cn.academy.util;

import cn.academy.AcademyCraft;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Utils to help us debug.
 * @author WeAthFolD
 */
public class DebugHelper {

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        if(AcademyCraft.DEBUG_MODE) {
            MinecraftForge.EVENT_BUS.register(new DebugHelper());
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingHurtEvent event) {
        if(event.getSource().getImmediateSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getImmediateSource();
            player.sendMessage(new TextComponentTranslation(
                String.format("%s: %.1f | %.1f/%.1f", event.getEntity().getClass().getSimpleName(), event.getAmount(),
                    event.getEntityLiving().getHealth(), event.getEntityLiving().getMaxHealth())));
        }
    }
    
}