package cn.academy.core.debug;

import cn.academy.core.AcademyCraft;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * Utils to help us debug.
 * @author WeAthFolD
 */
@Registrant
@RegInit
public class DebugHelper {

	public static void init() {
		if(AcademyCraft.DEBUG_MODE) {
			MinecraftForge.EVENT_BUS.register(new DebugHelper());
		}
	}
	
	@SubscribeEvent
	public void onLivingAttack(LivingAttackEvent event) {
		if(event.source.getEntity() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.source.getEntity();
			player.addChatMessage(new ChatComponentTranslation("Damage to " + event.entity.getClass().getSimpleName() + 
				": " + event.ammount + "/" + event.entityLiving.getMaxHealth()));
		}
	}
	
}
