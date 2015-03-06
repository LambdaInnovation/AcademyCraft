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
package cn.academy.ability.electro.client.render;

import net.minecraft.util.Vec3;
import cn.liutils.util.misc.Pair;

/**
 * @author WeathFolD
 *
 */
public interface IPointFactory {
	
	public static class NormalVert {
		public final Vec3 vert;
		public final Vec3 normal;
		
		public NormalVert(Vec3 v, Vec3 n) {
			vert = v;
			normal = n;
		}
		
		public NormalVert() {
			this(0, 0, 0, 0, 0, 0);
		}
		
		public NormalVert(double x, double y, double z, double u, double v, double w) {
			vert = vec(x, y, z);
			normal = vec(u, v, w);
		}
		
		protected static final Vec3 vec() {
			return vec(0, 0, 0);
		}
		
		protected static final Vec3 vec(double x, double y, double z) {
			return Vec3.createVectorHelper(x, y, z);
		}
		
		/**
		 * Return yaw and pitch in the order.
		 */
		public Pair<Double, Double> getNormalAngles() {
			double tmp = normal.xCoord * normal.xCoord + normal.zCoord * normal.zCoord;
			Pair<Double, Double> res = 
				new Pair(Math.atan2(normal.xCoord, normal.zCoord) * 180 / Math.PI, 
				Math.atan2(normal.yCoord, Math.sqrt(tmp) * 180 / Math.PI));
			return res;
		}
	}
	
	public NormalVert next();
	
}
