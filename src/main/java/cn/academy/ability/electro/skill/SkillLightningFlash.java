/**
 * 
 */
package cn.academy.ability.electro.skill;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.entity.EntityLF;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.util.GenericUtils;

/**
 * 电光一闪技能
 * @author WeathFolD
 */
public class SkillLightningFlash extends SkillBase {
	
	Random rand = new Random();

	public SkillLightningFlash() {
		this.setLogo("electro/lf.png");
		setName("em_lf");
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new LFState(player);
			}
			
		});
	}
	
	public static class LFState extends PatternHold.State {

		public LFState(EntityPlayer player) {
			super(player);
		}
		
		@Override
		public void onStart() {
			AbilityData data = AbilityDataMain.getData(player);
			
			int slv = data.getSkillLevel(CatElectro.lightningFlash);
			int time = 100 + slv * 28;
			int ccp = 2200 - 20 * (slv * slv);
			float dmg = (float) GenericUtils.randIntv((double)5, 5 + slv * 2);
			
			//give buff
			if(!player.worldObj.isRemote) {
				player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, time, 4));
				player.addPotionEffect(new PotionEffect(Potion.jump.id, time, 4));
				player.worldObj.spawnEntityInWorld(new EntityLF(data, time));
			}
		}

		@Override
		public void onFinish() {}

		@Override
		public void onHold() {}
		
	}

}
