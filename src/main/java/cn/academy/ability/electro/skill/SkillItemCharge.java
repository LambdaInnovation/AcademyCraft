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
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.skill.SRSmallCharge;
import cn.academy.ability.electro.entity.EntityChargingArc;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.render.SkillRenderManager;
import cn.academy.core.client.render.SkillRenderManager.RenderNode;
import cn.academy.core.register.ACItems;
import cn.academy.energy.util.EnergyUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Item/Block charging ability.
 * @author WeathFolD
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
public class SkillItemCharge extends SkillBase {
	
	@SideOnly(Side.CLIENT)
	static SkillRenderer charge;
	
	@SideOnly(Side.CLIENT)
	public static void init() {
		charge = new SRSmallCharge(5, 0.8);
	}

	public SkillItemCharge() {
		this.setLogo("electro/itemcharge.png");
		setName("em_itemcharge");
		setMaxLevel(10);
	}
	
	private static void playSound(EntityPlayer player, int n) {
		player.worldObj.playSoundAtEntity(player, "academy:elec.charge." + n, .5f, 1f);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {
			@Override
			public State createSkill(EntityPlayer player) {
				return new ChargeState(player);
			}
		}.setCooldown(50));
	}
	
	public static float getConsume(AbilityData data) {
		return 35 - data.getSkillLevel(CatElectro.itemCharge) * 0.6F + data.getLevelID() * 1.8F;
	}
	
	public static int getCharge(AbilityData data) {
		return 7 * (data.getLevelID() + 1) * data.getSkillLevel(CatElectro.itemCharge);
	}
	
	private static abstract class Callback {
		final AbilityData data;
		final EntityPlayer player;
		final int chg;
		final float cpt;
		
		public Callback(AbilityData _data) {
			data = _data;
			player = data.getPlayer();
			chg = getCharge(data);
			cpt = getConsume(data);
		}
		
		abstract void start();
		abstract boolean tick();
		abstract void end();
	}
	
	public static class ChargeState extends State {
		
		@SideOnly(Side.CLIENT)
		RenderNode node;
		
		Callback cb;

		public ChargeState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			ItemStack stack = player.getCurrentEquippedItem();
			AbilityData data = AbilityDataMain.getData(player);
			
			if(stack == null) {
				cb = new ChargeBlock(data);
			} else {
				cb = new ChargeItem(data);
			}
			
			cb.start();
			if(player.worldObj.isRemote) {
				node = SkillRenderManager.addEffect(charge);
			}
		}

		@Override
		public boolean onFinish(boolean fin) {
			if(player.worldObj.isRemote) {
				node.setDead();
			}
			cb.end();
			return true;
		}
		
		@Override
		protected boolean onTick(int time) {
			int judge = time - 1;
			if(judge == 0) { //begin sound
				playSound(player, 0);
			} else if(judge % 40 == 0) { //continuing sound
				playSound(player, ((judge - 40) / 40) % 4 + 1);
			}
			return cb.tick();
		}

		@Override
		public void onHold() {}
	}
	
	private static class ChargeItem extends Callback {
		public ChargeItem(AbilityData _data) {
			super(_data);
		}
		
		@Override
		public void start() {}

		@Override
		public boolean tick() {
			ItemStack stack = player.getCurrentEquippedItem();
			if(!EnergyUtils.isElecItem(stack)) {
				return true;
			}
			if(data.decreaseCP(cpt, CatElectro.itemCharge)) {
				EnergyUtils.tryCharge(stack, chg);
			} else {
				return true;
			}
			return false;
		}

		@Override
		public void end() {}
	}
	
	private static class ChargeBlock extends Callback {
		
		EntityChargingArc arc;

		public ChargeBlock(AbilityData _data) {
			super(_data);
		}
		
		@Override
		public void start() {
			if(!player.worldObj.isRemote) {
				player.worldObj.spawnEntityInWorld(arc = new EntityChargingArc(player, chg));
			}
		}

		@Override
		public boolean tick() {
			return !data.decreaseCP(cpt, CatElectro.itemCharge);
		}

		@Override
		public void end() {
			if(!player.worldObj.isRemote) {
				arc.setDead();
			}
		}
		
	}

}
