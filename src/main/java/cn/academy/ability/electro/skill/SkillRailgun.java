/**
 * 
 */
package cn.academy.ability.electro.skill;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.client.render.skill.RailgunPlaneEffect;
import cn.academy.ability.electro.entity.EntityRailgun;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.internal.PatternDown;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.api.event.ThrowCoinEvent;
import cn.academy.core.client.render.SkillRenderManager;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.entity.EntityThrowingCoin;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 传说中的超电磁炮~~！
 * TODO 施工中
 * @author WeathFolD
 */
@RegistrationClass
@RegEventHandler(Bus.Forge)
public class SkillRailgun extends SkillBase {
	
	public static SkillRailgun instance;

	public SkillRailgun() {
		instance = this;
	}
	
	private static Map<EntityPlayer, Integer> etcData = new HashMap();
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public boolean onKeyDown(EntityPlayer player) {
				if(player.worldObj.isRemote) return false;
				
				
				Integer eid = etcData.get(player);
				if(eid == null) return false;
				
				Entity ent = player.worldObj.getEntityByID(eid);
				if(ent != null && ent instanceof EntityThrowingCoin) {
					EntityThrowingCoin etc = (EntityThrowingCoin) ent;
					if(!etc.isDead && etc.getProgress() > 0.8) {
						player.worldObj.spawnEntityInWorld(new EntityRailgun(AbilityDataMain.getData(player)));
						etc.setDead();
					}
				}
				return true;
			}
			
		});
	}
	
	@SubscribeEvent
	public void onThrowCoin(ThrowCoinEvent event) {
		etcData.put(event.entityPlayer, event.coin.getEntityId());
		if(event.entityPlayer.worldObj.isRemote) {
			SkillRenderManager.addEffect(RailgunPlaneEffect.instance, 
					RailgunPlaneEffect.getAnimLength());
		}
	}
	
	public String getInternalName() {
		return "em_railgun";
	}
	
	public int getMaxSkillLevel() {
		return 200;
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_RAILGUN;
	}

}
