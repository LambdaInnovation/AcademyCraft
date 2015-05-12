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
package cn.academy.terminal;

import net.minecraft.nbt.NBTTagCompound;

/**
 * It is enforced that you use Flyweight approach on each app.
 * Every time the player opens the app, the onActivated() function will get called,
 * When it was closed the onKilled() function will get called, and you are responsible for updating the states.
 * @author WeAthFolD, Jiangyue.
 */
public abstract class App {

	public abstract void onActivated(NBTTagCompound nbt);
	
	public abstract void onKilled(NBTTagCompound nbt);
	
}
