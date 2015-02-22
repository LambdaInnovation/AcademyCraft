/**
 * 
 */
package cn.academy.ability.meltdowner.skill;

/**
 * @author WeathFolD
 *
 */
public class SkillMiningExpert extends SkillMiningBase {

	/**
	 * 
	 */
	public SkillMiningExpert() {
		this.setLogo("meltdowner/mine_expert.png");
		this.setName("md_mineexpert");
	}
	
	@Override
	float getConsume(int slv, int lv) {
		return 0.4f * (35 - slv * 0.8f - lv * 1.6f);
	}

	@Override
	int getHarvestLevel() {
		return 3;
	}

	@Override
	int getSpawnRate() {
		return 25;
	}

}
