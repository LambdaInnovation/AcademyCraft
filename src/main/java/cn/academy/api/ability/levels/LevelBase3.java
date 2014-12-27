package cn.academy.api.ability.levels;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;

public class LevelBase3 extends Level {

	public LevelBase3(Category cat) {
		super(cat);
	}

	@Override
	public int getLevelNum() {
		return 3;
	}

	@Override
	public float getInitialCP() {
		return 2000;
	}

	@Override
	public float getMaxCP() {
		return 3500;
	}

	@Override
	public float getInitRecoverCPRate() {
		return 2.2f;
	}

	@Override
	public float getMaxRecoverCPRate() {
		return 2.6f;
	}

}
