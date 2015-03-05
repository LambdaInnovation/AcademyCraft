/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
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
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
public class SkillMiningAcc extends SkillMiningBase {
	
	@RegEntity
	public static class AccRay extends EntityMiningRayBase {

		public AccRay(AbilityData data) {
			super(data, CatMeltDowner.mineAcc);
		}
		
		public AccRay(World world) {
			super(world);
		}
		
		@Override
		protected void onDiggedBlock(Block b, int x, int y, int z, int meta) {
			Item i = Item.getItemFromBlock(b);
			if(i != null) {
				ItemStack toDrop = new ItemStack(i, 1, meta);
				worldObj.spawnEntityInWorld(new EntityItem(worldObj, x + .5, y + .5, z + .5, toDrop));
			} else {
				super.onDiggedBlock(b, x, y, z, meta);
			}
		}

		@Override
		protected int getDigRate(int slv, int lv) {
			return 14;
		}

		@Override
		protected int getHarvestLevel() {
			return 4;
		}

		@Override
		public ResourceLocation[] getTexData() {
			return ACClientProps.ANIM_MD_RAY_SA;
		}

		@Override
		public float getRayWidth() {
			return .35f;
		}
	}

	public SkillMiningAcc() {
		this.setLogo("meltdowner/mine_acc.png");
		this.setName("md_mineacc");
		setMaxLevel(10);
	}

	@Override
	float getConsume(int slv, int lv) {
		return 0.5f * (95 - slv *2.5f);
	}

	@Override
	protected EntityMiningRayBase createEntity(AbilityData data) {
		return new AccRay(data);
	}
	
}
