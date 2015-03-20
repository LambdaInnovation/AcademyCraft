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
package cn.academy.ability.meltdowner.skill;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMiningRayBase;
import cn.academy.api.data.AbilityData;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class SkillMiningExpert extends SkillMiningBase {

	@RegEntity
	public static class ExpertRay extends EntityMiningRayBase {

		public ExpertRay(AbilityData data) {
			super(data, CatMeltDowner.mineExpert);
		}
		
		public ExpertRay(World world) {
			super(world);
		}

		@Override
		protected float getSpeed(int slv, int lv) {
			return .25f;
		}

		@Override
		protected int getHarvestLevel() {
			return 3;
		}

		@Override
		public ResourceLocation[] getTexData() {
			return ACClientProps.ANIM_MD_RAY_S;
		}

		@Override
		public float getRayWidth() {
			return .8f;
		}
	}
	
	/**
	 * 
	 */
	public SkillMiningExpert() {
		super("academy:md.mine_simple_startup", "academy:md.mine_simple_loop");
		this.setLogo("meltdowner/mine_expert.png");
		this.setName("md_mineexpert");
		setMaxLevel(15);
	}
	
	@Override
	float getConsume(int slv, int lv) {
		return 0.4f * (35 - slv * 0.8f - lv * 1.6f);
	}

	@Override
	protected EntityMiningRayBase createEntity(AbilityData data) {
		return new ExpertRay(data);
	}

}
