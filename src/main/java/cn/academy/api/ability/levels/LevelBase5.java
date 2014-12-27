package cn.academy.api.ability.levels;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;

public class LevelBase5 extends Level {

	public LevelBase5(Category cat) {
		super(cat);
	}

	@Override
	public int getLevelNum() {
		return 5;
	}

	@Override
	public float getInitialCP() {
		return 6000;
	}

	@Override
	public float getMaxCP() {
		return 10000;
	}

	@Override
	public float getInitRecoverCPRate() {
		return 4.0f;
	}

	@Override
	public float getMaxRecoverCPRate() {
		return 5.0f;
	}

}
