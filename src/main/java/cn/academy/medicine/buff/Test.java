package cn.academy.medicine.buff;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;

@Registrant
@RegEventHandler
public class Test {
	@SubscribeEvent
	public void test(LivingFallEvent e){
		if((!e.entity.worldObj.isRemote)&&e.entityLiving instanceof EntityPlayer){
			e.entityLiving.addPotionEffect(new BuffAllergic(999));
			AcademyCraft.log.info("test");
			e.entityLiving.getActivePotionEffects()
			.stream()
			.filter(b->b instanceof BuffAllergic)
			.forEach((b)->{
				AcademyCraft.log.info(((BuffAllergic)b).level);
			});
		}
	}
}
