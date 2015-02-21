/**
 * 
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
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Item/Block charging ability.
 * @author WeathFolD
 */
@RegistrationClass
public class SkillItemCharge extends SkillBase {
	
	@SideOnly(Side.CLIENT)
	static SkillRenderer charge = new SRSmallCharge(5, 0.8);

	public SkillItemCharge() {
		this.setLogo("electro/itemcharge.png");
		setName("em_itemcharge");
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {
			@Override
			public State createSkill(EntityPlayer player) {
				return new ChargeState(player);
			}
		});
	}
	
	public static float getConsume(AbilityData data) {
		return 35 - data.getSkillLevel(CatElectro.itemCharge) * 0.6F + data.getLevelID() * 1.8F;
	}
	
	public static int getCharge(AbilityData data) {
		return 7 * (data.getLevelID() + 1) * (data.getSkillLevel(CatElectro.itemCharge) + 1);
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
			
			if(stack == null || stack.getItem() == ACItems.ivoid) {
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
		public void onFinish() {
			if(player.worldObj.isRemote) {
				node.setDead();
			}
			cb.end();
		}
		
		@Override
		protected boolean onTick(int time) {
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
			if(data.decreaseCP(cpt)) {
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
			return !data.decreaseCP(cpt);
		}

		@Override
		public void end() {
			if(!player.worldObj.isRemote) {
				arc.setDead();
			}
		}
		
	}

}
