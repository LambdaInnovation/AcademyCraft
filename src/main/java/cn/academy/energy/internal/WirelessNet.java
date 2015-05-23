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
package cn.academy.energy.internal;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cn.academy.energy.internal.VBlocks.VWMatrix;

/**
 * @author WeAthFolD
 */
public class WirelessNet {

	final World world;
	
	VWMatrix matrix;
	
	String ssid;
	String password;
	
	boolean alive = false;
	
	public WirelessNet(World _world) {
		world = _world;
	}
	
	public String getSSID() {
		return ssid;
	}
	
	/**
	 * Get whether this matrix is alive (That is, there are >=1 node loaded and should be ticked normally).
	 */
	public boolean isAlive() {
		return alive;
	}
	
	void dispose() {}
	
	void onCreate() {
		//TODO: Init the lookup tables
	}
	
	void onCleanup() {
		//TODO: Clean up the lookup tables.
	}
	
	/**
	 * This is a slightly costy function. You should buffer the result and query through isAlive().
	 * query it infrequently.
	 * @return same as isAlive
	 */
	private boolean checkIsAlive() {
		//IF: alive node count > 1
		return alive;
	}
	
	void tick() {
		
	}
	
	void loadFromNBT(NBTTagCompound tag) {
		
	}
	
	void saveToNBT(NBTTagCompound tag) {
		
	}
	
}
