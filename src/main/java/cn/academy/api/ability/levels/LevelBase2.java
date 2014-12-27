package cn.academy.api.ability.levels;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;

public class LevelBase2 extends Level {

	public LevelBase2(Category cat) {
		super(cat);
	}

	@Override
	public int getLevelNum() {
		return 2;
	}

	@Override
	public float getInitialCP() {
		return 800;
	}

	@Override
	public float getMaxCP() {
		return 2000;
	}

	@Override
	public float getInitRecoverCPRate() {
		return 1.5f;
	}

	@Override
	public float getMaxRecoverCPRate() {
		return 1.8f;
	}

}
