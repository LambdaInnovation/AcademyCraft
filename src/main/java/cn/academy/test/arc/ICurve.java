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
package cn.academy.test.arc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Vec3;

/**
 * Defines a curve in arbitary shape or length, defined with a FIXED number of points.
 * This curve have a 'position' 'iterator'. You can use nextPoint to get a new point whith is going further
 * than the last point (denoted by 'getPosition') some step.
 * @author WeAthFolD
 */
public interface ICurve {
	
	public double getLength();
	
	public abstract Vec3 getTangent(double pos);
	
	public abstract Vec3 getPoint(double pos);
	
}
