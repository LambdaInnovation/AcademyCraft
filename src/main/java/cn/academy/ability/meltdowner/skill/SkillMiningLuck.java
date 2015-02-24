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

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityMdBall;
import cn.academy.ability.meltdowner.entity.EntityMiningRay;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class SkillMiningLuck extends SkillMiningBase {

	public SkillMiningLuck() {
		this.setLogo("meltdowner/mine_luck.png");
		this.setName("md_mineluck");
		setMaxLevel(10);
	}

	@Override
	float getConsume(int slv, int lv) {
		return 0.3f * (95 - slv * 2.5f);
	}

	@Override
	int getHarvestLevel() {
		return 3;
	}

	@Override
	int getSpawnRate() {
		return 24;
	}
	
	protected EntityMiningRay createEntity(EntityPlayer player, EntityMdBall ball) {
		return new LuckyRay(player, ball);
	}
	
	@RegEntity
	public static class LuckyRay extends EntityMiningRay {
		public LuckyRay(EntityPlayer player, EntityMdBall ball) {
			super(player, ball, CatMeltDowner.mineLuck.getHarvestLevel());
		}
		
		public LuckyRay(World world) {
			super(world);
		}
		
		@Override
		protected void onDiggedBlock(Block b, int x, int y, int z, int meta) {
			int n = 1 + rand.nextInt(3);
			for(int i = 0; i < n; ++i) {
				super.onDiggedBlock(b, x, y, z, meta);
			}
		}
		@Override
		public ResourceLocation[] getTexData() {
			return ACClientProps.ANIM_MD_RAY_SF;
		}
	}

}
