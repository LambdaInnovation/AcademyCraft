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
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
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
public class SkillMiningLuck extends SkillMiningBase {
	
	@RegEntity
	public static class LuckyRay extends EntityMiningRayBase {

		public LuckyRay(AbilityData data) {
			super(data, CatMeltDowner.mineLuck);
		}
		
		public LuckyRay(World world) {
			super(world);
		}
		
		@Override
		protected void onDiggedBlock(Block b, int x, int y, int z, int meta) {
			int n = 1 + rand.nextInt(3);
			Item it = b.getItemDropped(meta, rand, meta);
			
			if(it instanceof ItemBlock) n = 1;
			for(int i = 0; i < n; ++i) {
				super.onDiggedBlock(b, x, y, z, meta);
			}
		}

		@Override
		protected float getSpeed(int slv, int lv) {
			return .22f;
		}

		@Override
		protected int getHarvestLevel() {
			return 3;
		}

		@Override
		public ResourceLocation[] getTexData() {
			return ACClientProps.ANIM_MD_RAY_SF;
		}

		@Override
		public float getRayWidth() {
			return .8f;
		}
	}

	public SkillMiningLuck() {
		super("academy:md.mine_luck_startup", "academy:md.mine_simple_loop");
		this.setLogo("meltdowner/mine_luck.png");
		this.setName("md_mineluck");
		setMaxLevel(10);
	}

	@Override
	float getConsume(int slv, int lv) {
		return 0.3f * (95 - slv * 2.5f);
	}

	@Override
	protected EntityMiningRayBase createEntity(AbilityData data) {
		return new LuckyRay(data);
	}

}
