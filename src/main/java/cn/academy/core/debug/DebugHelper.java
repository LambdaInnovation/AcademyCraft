/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.debug;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Utils to help us debug.
 * @author WeAthFolD
 */
@Registrant
public class DebugHelper {

    @RegInitCallback
    private static void init() {
        if(AcademyCraft.DEBUG_MODE) {
            MinecraftForge.EVENT_BUS.register(new DebugHelper());
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingHurtEvent event) {
        if(event.source.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.source.getEntity();
            player.addChatMessage(new ChatComponentTranslation(
                String.format("%s: %.1f | %.1f/%.1f", event.entity.getClass().getSimpleName(), event.ammount,
                    event.entityLiving.getHealth(), event.entityLiving.getMaxHealth())));
        }
    }
    
}
