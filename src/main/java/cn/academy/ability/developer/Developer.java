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

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.core.AcademyCraft;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.liutils.ripple.ScriptNamespace;
import cpw.mods.fml.relauncher.Side;

/**
 * This class represents actions performed by the Developer. 
 * Separated from TileEntity to be used in both Portable and Block version.
 * 
 * Subclasses should provide an InstanceSerializer, so
 * 	we can synchronize the state and start the work in server side.
 * @author WeAthFolD
 */
public abstract class Developer {
	
	public enum DevState { IDLE, FAILED, DEVELOPING };
	
	IDevelopType current;
	
	// Synced states
	int stim;
	int maxStim;
	private DevState state = DevState.IDLE;
	// Synced states end
	
	int tickThisStim;
	
	public final DeveloperType type;

	public Developer(DeveloperType _type) {
		type = _type;
	}
	
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
	
	/**
	 * Must be called from SERVER. Start the develop action.
	 * @return Whether the action can really be started
	 */
	public boolean startDevelop(IDevelopType type) {
		if(!type.validate(getUser()))
			return false;
		
		reset();
		
		current = type;
		tickThisStim = stim = 0;
		maxStim = type.getStimulations(getUser());
		return true;
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
	public final int getTPS() {
		return type.getTPS();
	}
	
	/**
	 * @return IF consume per stimulation.
	 */
	public final double getCPS() {
		return type.getCPS();
	}
	
	/**
	 * Consume some amount of energy
	 * @return Whether the comsume action is successful.
	 */
	public abstract boolean pullEnergy(double amt);
	
	private void doSync() {
		synced(maxStim, stim, state);
	}
	
	@RegNetworkCall(side = Side.CLIENT, thisStorage = StorageOption.Option.INSTANCE)
	private void synced(@Data Integer _maxStim, @Data Integer _stim, @Instance DevState _state) {
		maxStim = _maxStim;
		stim = _stim;
		state = _state;
	}
	
}
