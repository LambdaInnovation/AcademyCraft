/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.skill.SRSmallCharge;
import cn.academy.ability.electro.entity.EntityChargingArc;
import cn.academy.ability.electro.entity.fx.ChargeEffectS;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.render.SkillRenderManager;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.register.ACItems;
import cn.academy.core.util.EnergyUtils;
import cn.annoreg.core.RegistrationClass;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 对手上的物品充能
 * TODO 施工中 现在是实验场
 * @author WeathFolD
 */
@RegistrationClass
public class SkillItemCharge extends SkillBase {
	
	@SideOnly(Side.CLIENT)
	static SkillRenderer charge = new SRSmallCharge(5, 0.8);

	public SkillItemCharge() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(100) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new StateHold(player);
			}

		});
	}
	
	public String getInternalName() {
		return "em_itemcharge";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_CHARGE;
	}
	
	public float getCPT(AbilityData data) {
		return 35 - data.getSkillLevel(this)* 0.6F + data.getLevelID() * 1.8F;
	}
	
	public int getEPT(AbilityData data) {
		return 7 * (data.getLevelID() + 1) * (data.getSkillLevel(this) + 1);
	}
	
	public static class StateHold extends State {
		
		ItemStack stack;
		boolean isItem;
		
		EntityChargingArc arc;

		public StateHold(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() { 
			ItemStack stack = player.getCurrentEquippedItem();
			if(stack != null && !(stack.getItem() == ACItems.ivoid)) {
				isItem = true;
				System.out.println("a");
				if(!EnergyUtils.isElecItem(stack)) {
					System.out.println("b");
					this.finishSkill();
				} else {
					System.out.println("c");
					player.worldObj.spawnEntityInWorld(new ChargeEffectS(player, 40, 5));
					SkillRenderManager.addEffect(charge, 200);
				}
				return;
			}
			
			World world = player.worldObj;
			System.out.println("d");
			world.spawnEntityInWorld(arc = new EntityChargingArc(AbilityDataMain.getData(player)));
			if(world.isRemote) {
				world.spawnEntityInWorld(new ChargeEffectS(player, 40, 5));
				SkillRenderManager.addEffect(charge, 200);
			}
		}

		@Override
		public void onFinish() {
			if(arc != null) {
				arc.setDead();
			}
		}

		@Override
		public void onHold() {}
		
		protected boolean onTick() {
			AbilityData data = AbilityDataMain.getData(player);
			if(isItem) {
				ItemStack stack = player.getCurrentEquippedItem();
				if(stack == null) {
					return true;
				} else {
					EnergyUtils.tryCharge(stack, CatElectro.itemCharge.getEPT(data));
				}
			}
			if(!data.decreaseCP(CatElectro.itemCharge.getCPT(data))) {
				return true;
			}
			return false;
		}
		
	}

}
