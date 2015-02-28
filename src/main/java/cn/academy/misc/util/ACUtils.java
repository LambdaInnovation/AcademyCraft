/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.util;

import cn.academy.core.proxy.ACClientProps;
import cn.liutils.util.render.LambdaFont.Align;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Just some very generic utils.
 * @author WeathFolD
 */
public class ACUtils {

	@SideOnly(Side.CLIENT)
	public static void drawText(String text, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	@SideOnly(Side.CLIENT)
	public static void drawText(String text, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}
	
	@SideOnly(Side.CLIENT)
	public static void drawText(String text, double x, double y, double size, double cst) {
		ACClientProps.FONT_YAHEI_32.drawAdjusted(text, x, y, size, cst);
	}
	
	@SideOnly(Side.CLIENT)
	public static void drawText(String text, double x, double y, double size, Align align, double cst) {
		ACClientProps.FONT_YAHEI_32.drawAdjusted(text, x, y, size, align, cst);
	}

}
