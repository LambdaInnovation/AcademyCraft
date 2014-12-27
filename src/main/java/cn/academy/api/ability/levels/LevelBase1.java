package cn.academy.api.ability.levels;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;

public class LevelBase1 extends Level {

	public LevelBase1(Category cat) {
		super(cat);
	}

	@Override
	public int getLevelNum() {
		return 1;
	}

	@Override
	public float getInitialCP() {
		return 400;
	}

	@Override
	public float getMaxCP() {
		return 800;
	}

	@Override
	public float getInitRecoverCPRate() {
		return 0.5f;
	}

	@Override
	public float getMaxRecoverCPRate() {
		return 1.0f;
	}

}
