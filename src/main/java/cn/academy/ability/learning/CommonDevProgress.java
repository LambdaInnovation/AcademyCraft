/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.learning;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.block.TileDeveloper;

/**
 * @author WeAthFolD
 *
 */
public class CommonDevProgress extends DevelopProgress {
	
	public interface DevCallback {
		void invoke(EntityPlayer target);
	}
	
	final DevCallback callback;
	
	final int stims;
	
	int stimCount;

	public CommonDevProgress(int _stims, DevCallback _callback) {
		stims = _stims;
		callback = _callback;
	}

	@Override
	public void onTick() {
		TileDeveloper developer = getDeveloper();
		double expect = developer.getConsumePerStim();
		if(developer.getEnergy() >= expect) {
			developer.pullEnergy(expect);
			stimCount++;
		}
	}

	@Override
	public void onSuccessful() {
		callback.invoke(getDeveloper().getUser());
	}

	@Override
	public double getProgress() {
		return (double) stimCount / stims;
	}

}
