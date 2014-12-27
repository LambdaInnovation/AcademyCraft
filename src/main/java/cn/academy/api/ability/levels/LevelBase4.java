package cn.academy.api.ability.levels;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;

public class LevelBase4 extends Level {

	public LevelBase4(Category cat) {
		super(cat);
	}

	@Override
	public int getLevelNum() {
		return 5;
	}

	@Override
	public float getInitialCP() {
		return 3500;
	}

	@Override
	public float getMaxCP() {
		return 6000;
	}

	@Override
	public float getInitRecoverCPRate() {
		return 3.0f;
	}

	@Override
	public float getMaxRecoverCPRate() {
		return 3.5f;
	}

}
