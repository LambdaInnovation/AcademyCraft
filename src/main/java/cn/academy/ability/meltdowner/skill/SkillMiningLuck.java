/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cn.academy.ability.meltdowner.entity.EntityMdBall;
import cn.academy.ability.meltdowner.entity.EntityMiningRay;

/**
 * @author WeathFolD
 *
 */
public class SkillMiningLuck extends SkillMiningBase {

	public SkillMiningLuck() {
		this.setLogo("meltdowner/mine_luck.png");
		this.setName("md_mineluck");
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
		return new EntityMiningRay(player, ball, getHarvestLevel()) {
			@Override
			protected void onDiggedBlock(Block b, int x, int y, int z, int meta) {
				int n = 1 + rand.nextInt(3);
				for(int i = 0; i < n; ++i) {
					super.onDiggedBlock(b, x, y, z, meta);
				}
			}
		};
	}

}
