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

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.core.client.Resources;

/**
 */
public abstract class App {
	
	int appid;
	private final String name;
	protected ResourceLocation icon;
	
	private boolean preInstalled = false;
	
	public App(String _name) {
		name = _name;
		icon = getTexture("icon");
	}
	
	protected ResourceLocation getTexture(String texname) {
		return Resources.getTexture("guis/apps/" + name + "/" + texname);
	}
	
	protected String local(String key) {
		return StatCollector.translateToLocal("ac.app." + name + "." + key);
	}
	
	public ResourceLocation getIcon() {
		return icon;
	}
	
	public App setPreInstalled() {
		preInstalled = true;
		return this;
	}
	
	public int getID() {
		return appid;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return local("name");
	}
	
	public final boolean isPreInstalled() {
		return preInstalled;
	}
	
	void getEnvironment() {
		AppEnvironment env = createEnvironment();
		env.app = this;
	}

	public abstract AppEnvironment createEnvironment();
	
}
