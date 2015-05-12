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
import cn.liutils.util.VecUtils;

/**
 * Immutable line segment class.
 * @author WeAthFolD
 */
public class LineSegment implements ICurve {
	
	final List<Vec3> points = new ArrayList();
	final double length;
	final double[] lendp;
	
	public LineSegment(Vec3 ...vecs) {
		Vec3 last = null;
		double sum = 0;
		lendp = new double[vecs.length];
		for(int i = 0; i < vecs.length; ++i) {
			Vec3 v = vecs[i];
			points.add(v);
			if(last != null) {
				sum += v.distanceTo(last);
			}
			lendp[i] = sum;
			last = v;
		}
		length = sum;
	}

	@Override
	public Vec3 getTangent(double pos) {
		int i = getIndex(pos);
		Vec3 a = points.get(i), b = (i == points.size() - 1) ? null : points.get(i + 1);
		if(b == null) {
			return VecUtils.subtract(a, points.get(i - 1));
		} else {
			return VecUtils.subtract(b, a);
		}
	}

	@Override
	public Vec3 getPoint(double pos) {
		//Peform the search
		int i = getIndex(pos);
		Vec3 a = points.get(i), b = (i == points.size() - 1) ? null : points.get(i + 1);
		//Do the lerp
		return (b == null) ? null : VecUtils.lerp(a, b, (pos - lendp[i]) / (lendp[i + 1] - lendp[i]));
	}
	
	private int getIndex(double position) {
		int i = 0;
		for(; i < points.size(); ++i) {
			if(lendp[i] <= position) {
				break;
			}
		}
		return Math.min(i, points.size() - 1);
	}

	@Override
	public double getLength() {
		return length;
	}

}
