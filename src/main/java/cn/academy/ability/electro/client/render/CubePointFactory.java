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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cn.liutils.util.misc.Pair;

/**
 * Randomly gens a point on face of a cube with size(w, h, l). (placed at 0, 0, 0)
 * It is guaranteed that every point has the same appearing probablity.
 * @author WeathFolD
 */
public class CubePointFactory implements IPointFactory {
	
	/*
	 * Face id: 
	 * 0 -Y
	 * 1 +Y
	 * 2 -Z
	 * 3 +Z
	 * 4 -X
	 * 5 +X
	 */
	
	private static final int[][] NORMALS = {
		{0, -1, 0},
		{0, 1, 0},
		{0, 0, -1},
		{0, 0, 1},
		{-1, 0, 0},
		{1, 0, 0}
	};
	
	private Set<Integer> available = new HashSet(Arrays.asList(0, 1, 2, 3, 4, 5)); //Enabled face(s)
	private List<Pair<Double, Integer>> probList = new ArrayList<Pair<Double, Integer>>(); //probLowerBound-face pairs
	private double w, h, l;
	private static final Random RNG = new Random();
	boolean centered;

	public CubePointFactory(double _w, double _h, double _l) {
		setSize(_w, _h, _l);
	}
	
	public void setCentered(boolean b) {
		centered = b;
	}
	
	public void setEnableFaces(Integer... faces) {
		if(faces.length == 0)
			throw new RuntimeException("No face to generate");
		available.clear();
		available.addAll(Arrays.asList(faces));
		bake();
	}
	
	public void setSize(double _w, double _h, double _l) {
		w = _w;
		h = _h;
		l = _l;
		bake();
	}
	
	//calculate the probablity list
	private void bake() {
		double totalArea = 2 * (w * l + h * l + h * w);
		for(int i = 0; i < 6; ++i) {
			if(!available.contains(i)) totalArea -= getArea(i);
		}
		
		probList.clear();
		double curArea = 0;
		for(int a : available) {
			probList.add(new Pair<Double, Integer>(curArea, a));
			curArea += getArea(a) / totalArea;
		}
		//System.out.println("BakedList: " + probList);
	}
	
	private double getArea(int f) {
		if(f == 0 || f == 1) {
			return w * l;
		}
		if(f == 2 || f == 3) {
			return h * l;
		}
		return h * w;
	}
	
	private int randFace() {
		double p = RNG.nextDouble();
		for(Pair<Double, Integer> pair : probList) {
			if(pair.first >= p) return pair.second;
		}
		return probList.get(probList.size() - 1).second;
	}

	@Override
	public NormalVert next() {
		int face = randFace();
		double a, b;
		double xOffset = 0, zOffset = 0;
		if(centered) {
			xOffset = -w * 0.5;
			zOffset = -l * 0.5;
		}
		switch(face) {
		case 0:
		case 1:
			a = RNG.nextDouble() * w;
			b = RNG.nextDouble() * l;
			return new NormalVert(a + xOffset, face == 0 ? 0 : h, b + zOffset, 
				NORMALS[face][0], NORMALS[face][1], NORMALS[face][2]);
		case 2:
		case 3:
			a = RNG.nextDouble() * h;
			b = RNG.nextDouble() * w;
			return new NormalVert(b + xOffset, a, (face == 2 ? 0 : l) + zOffset, 
				NORMALS[face][0], NORMALS[face][1], NORMALS[face][2]);
		case 4:
		case 5:
			a = RNG.nextDouble() * h;
			b = RNG.nextDouble() * l;
			return new NormalVert((face == 4 ? 0 : w) + xOffset, a, b + zOffset,
				NORMALS[face][0], NORMALS[face][1], NORMALS[face][2]);
		}
		return null; //Not supposed to happen
	}

}
