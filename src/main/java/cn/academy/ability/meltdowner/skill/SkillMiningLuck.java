/**
 * 
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
