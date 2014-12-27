package cn.academy.api.ability.levels;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;

public class LevelBase0 extends Level {

	public LevelBase0(Category cat) {
		super(cat);
	}

	@Override
	public int getLevelNum() {
		return 0;
	}

	@Override
	public float getMaxCP() {
		return 0;
	}

	@Override
	public float getInitRecoverCPRate() {
		return 0;
	}

	@Override
	public float getMaxRecoverCPRate() {
		return 0;
	}

	@Override
	public float getInitialCP() {
		return 0;
	}

}
