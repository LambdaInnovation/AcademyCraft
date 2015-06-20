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
package cn.academy.misc.media;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.core.client.Resources;

/**
 * @author WeAthFolD
 */
public class Media {

	int id;
	
	final String name;
	final ResourceLocation cover;
	final int length; // Length in seconds.
	
	public Media(String _name, int _length) {
		name = _name;
		length = _length;
		cover = Resources.getTexture("/media/" + name + "_cover.png");
	}
	
	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal("ac.media." + name + ".name");
	}
	
	public String getDesc() {
		return StatCollector.translateToLocal("ac.media." + name + ".desc");
	}
	
	public String getLengthStr() {
		return getPlayingTime(length);
	}
	
	public static String getPlayingTime(int seconds) {
		return String.format("%2d:%2d", seconds / 60, seconds % 60);
	}

}
