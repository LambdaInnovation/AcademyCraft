/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.meltdowner.entity.EntityMdBall;
import cn.academy.ability.meltdowner.entity.EntityMiningRay;
import cn.academy.core.proxy.ACClientProps;

/**
 * @author WeathFolD
 *
 */
public class SkillMiningAcc extends SkillMiningBase {

	public SkillMiningAcc() {
		this.setLogo("meltdowner/mine_acc.png");
		this.setName("md_mineacc");
	}

	@Override
	float getConsume(int slv, int lv) {
		return 0.5f * (95 - slv *2.5f);
	}

	@Override
	int getHarvestLevel() {
		return 4;
	}

	@Override
	int getSpawnRate() {
		return 25;
	}
	
	protected EntityMiningRay createEntity(EntityPlayer player, EntityMdBall ball) {
		return new EntityMiningRay(player, ball, getHarvestLevel()) {
			@Override
			protected void onDiggedBlock(Block b, int x, int y, int z, int meta) {
				Item i = Item.getItemFromBlock(b);
				if(i != null) {
					ItemStack toDrop = new ItemStack(i);
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, x + .5, y + .5, z + .5, toDrop));
				} else {
					super.onDiggedBlock(b, x, y, z, meta);
				}
			}
			@Override
			public ResourceLocation[] getTexData() {
				return ACClientProps.ANIM_MD_RAY_SA;
			}
		};
	}

}
