/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

/**
 * @author WeathFolD
 *
 */
public class SkillMiningBasic extends SkillMiningBase {

	/**
	 * 
	 */
	public SkillMiningBasic() {
		this.setLogo("meltdowner/mine_basic.png");
		this.setName("md_minebasic");
	}

	@Override
	float getConsume(int slv, int lv) {
		return 0.6f * (10 - slv * 0.3f - lv * 0.4f);
	}

	@Override
	int getHarvestLevel() {
		return 1;
	}

	@Override
	int getSpawnRate() {
		return 30;
	}
	
}
