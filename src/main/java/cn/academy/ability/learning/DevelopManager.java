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

import cn.academy.ability.block.TileDeveloper;

/**
 * An independent tickable ability developement progress tracker.
 * SERVER only.
 * @author WeAthFolD
 */
public class DevelopManager {
	
	private DevelopProgress handler;
	
	public final TileDeveloper developer;

	public DevelopManager(TileDeveloper _developer) {
		developer = _developer;
	}
	
	public void tick() {
		if(handler != null) {
			handler.onTick();
			if(handler.getProgress() >= 1.0) {
				// End the developing
				handler.onSuccessful();
				handler = null;
			}
		}
	}
	
	public void startDevelop(DevelopProgress devprog) {
		if(devprog.manager != null)
			throw new RuntimeException("Trying to use a DevelopProgress twice!");
		handler = devprog;
		handler.manager = this;
	}
	
	public void abortDeveloping() {
		handler = null;
	}
	
	public boolean isDeveloping() {
		return handler != null;
	}
	
	public double getDevelopProgress() {
		return handler == null ? 1.0 : handler.getProgress();
	}

}
