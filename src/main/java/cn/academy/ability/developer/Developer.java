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
package cn.academy.ability.developer;

import cn.academy.core.AcademyCraft;
import net.minecraft.entity.player.EntityPlayer;

/**
 * This class represents actions performed by the Developer. 
 * Separated from TileEntity to be used in both Portable and Block version.
 * @author WeAthFolD
 */
public abstract class Developer {
	
	public enum DevState { IDLE, FAILED, DEVELOPING };
	
	IDevelopType current;
	int stim;
	int maxStim;
	
	int tickThisStim;
	
	private DevState state = DevState.IDLE;

	public Developer() {}
	
	public DevState getState() {
		return state;
	}

	public void tick() {
		if(isDeveloping()) {
			if(++tickThisStim >= getTPS()) {
				tickThisStim = 0;
				
				if(tryConsume()) {
					++stim;
					if(stim == maxStim) {
						endDevelop();
					}
				} else {
					abort();
				}
			}
		}
	}
	
	void startDevelop(IDevelopType type) {
		reset();
		
		current = type;
		tickThisStim = stim = 0;
		maxStim = type.getStimulations(getUser());
	}
	
	public void reset() {
		current = null;
		state = DevState.IDLE;
	}
	
	public void abort() {
		current = null;
		state = DevState.FAILED;
	}
	
	private boolean tryConsume() {
		return pullEnergy(getCPS());
	}

	private void endDevelop() {
		if(current != null) {
			if(getUser() != null) {
				current.onLearned(getUser());
			} else {
				AcademyCraft.log.warn("BUG: Null user when ending develop.");
			}
		}
		
		reset();
	}
	
	public boolean isDeveloping() {
		return current != null;
	}
	
	public double getDevelopProgress() {
		return (double) stim / maxStim;
	}
	
	public abstract EntityPlayer getUser();
	
	/**
	 * @return Ticks per stimulation. (i.e. develop speed)
	 */
	public abstract int getTPS();
	
	/**
	 * @return IF consume per stimulation.
	 */
	public abstract double getCPS();
	
	/**
	 * Consume some amount of energy
	 * @return Whether the comsume action is successful.
	 */
	public abstract boolean pullEnergy(double amt);
	
}
