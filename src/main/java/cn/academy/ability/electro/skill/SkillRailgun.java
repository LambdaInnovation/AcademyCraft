/**
 * 
 */
package cn.academy.ability.electro.skill;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.skill.RailgunPlaneEffect;
import cn.academy.ability.electro.entity.EntityRailgun;
import cn.academy.ability.electro.entity.fx.ChargeEffectS;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.internal.PatternDown;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.api.event.ThrowCoinEvent;
import cn.academy.core.client.render.SkillRenderManager;
import cn.academy.misc.entity.EntityThrowingCoin;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 传说中的超电磁炮~~！
 * TODO 施工中
 * @author WeathFolD
 */
@RegistrationClass
@RegEventHandler(Bus.Forge)
public class SkillRailgun extends SkillBase {

	public SkillRailgun() {
		setLogo("electro/railgun.png");
		setName("em_railgun");
		setMaxLevel(6);
	}
	
	private static Map<EntityPlayer, Integer> etcData = new HashMap();
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public boolean onKeyDown(EntityPlayer player) {
				AbilityData data = AbilityDataMain.getData(player);
				int slv = data.getSkillID(CatElectro.railgun), lv = data.getLevelID() + 1;
				
				Integer eid = etcData.get(player);
				if(eid == null) return false;
				
				
				
				Entity ent = player.worldObj.getEntityByID(eid);
				if(ent != null && ent instanceof EntityThrowingCoin) {
					if(player.worldObj.isRemote) {
						player.worldObj.spawnEntityInWorld(
							new ChargeEffectS.Strong(data.getPlayer(), 30, 4));
					}
					float consume = 2000 - 15 * (slv * slv);
					if(!data.decreaseCP(consume, SkillRailgun.this))
						return false;
					EntityThrowingCoin etc = (EntityThrowingCoin) ent;
					if(!etc.isDead && etc.getProgress() > 0.8) {
						//player.playSound("academy:elec.railgun", 0.5f, 1.0f);
						if(!player.worldObj.isRemote) {
							player.worldObj.playSoundAtEntity(player, "academy:elec.railgun", 0.5f, 1.0f);
							player.worldObj.spawnEntityInWorld(new EntityRailgun(AbilityDataMain.getData(player)));
							etc.setDead();
						}
					}
				}
				return true;
			}
			
		});
	}
	
	@SubscribeEvent
	public void onThrowCoin(ThrowCoinEvent event) {
		AbilityData data = AbilityDataMain.getData(event.entityPlayer);
		
		if(data.getCategory() != CatElectro.INSTANCE || 
			!data.isSkillLearned(CatElectro.railgun)) {
				return;
		}
		if(data.getPlayer().worldObj.isRemote) {
			if(!EventHandlerClient.isSkillEnabled() || 
			!EventHandlerClient.isSkillMapped(data.getSkillID(CatElectro.railgun)))
				return;
		}
		
		etcData.put(event.entityPlayer, event.coin.getEntityId());
		if(event.entityPlayer.worldObj.isRemote && data.isSkillLearned(CatElectro.railgun)) {
			SkillRenderManager.addEffect(RailgunPlaneEffect.instance, 
					RailgunPlaneEffect.getAnimLength());
		}
	}

}
