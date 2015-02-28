/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.meltdowner.skill;

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
public class SkillMiningBasic extends SkillMiningBase {

	@RegEntity
	public static class BasicRay extends EntityMiningRayBase {

		public BasicRay(AbilityData data) {
			super(data, CatMeltDowner.mineBasic);
		}
		
		public BasicRay(World world) {
			super(world);
		}

		@Override
		protected int getDigRate(int slv, int lv) {
			return 16;
		}

		@Override
		protected int getHarvestLevel() {
			return 2;
		}

		@Override
		public ResourceLocation[] getTexData() {
			return ACClientProps.ANIM_MD_RAY_S;
		}

		@Override
		public float getRayWidth() {
			return .2f;
		}
	}
	
	public SkillMiningBasic() {
		this.setLogo("meltdowner/mine_basic.png");
		this.setName("md_minebasic");
		setMaxLevel(15);
	}

	@Override
	float getConsume(int slv, int lv) {
		return 0.6f * (10 - slv * 0.3f - lv * 0.4f);
	}

	@Override
	protected EntityMiningRayBase createEntity(AbilityData data) {
		return new BasicRay(data);
	}
	
}
