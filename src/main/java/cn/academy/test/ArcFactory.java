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
package cn.academy.test;

import static cn.weaponry.core.blob.VecUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import cn.liutils.util.GenericUtils;
import cn.liutils.util.RenderUtils;
import cn.weaponry.core.blob.VecUtils;

/**
 * Used the concept of L-system and recursion to generate a lightning pattern.
 * @TODO Support animating
 * @TODO Support combining of multiple arcs
 * @author WeAthFolD
 */
public class ArcFactory {
	
	static final ResourceLocation TEXTURE = new ResourceLocation("academy:textures/effects/arc/line_segment.png");
	
	static Random rand = new Random();
	static Matrix4f matrix = new Matrix4f();
	
	public double length = 15.0;
	public double width = 0.1;
	//public ICurve curve;
	public double lengthShrink = 0.7;
	public double alphaShrink = 0.9;
	public int passes = 6;
	public double maxOffset = 1.5;
	public double branchFactor = 0.4;
	public double widthShrink = 0.7;
	public Vec3 normal = vec(0, 0, 1);
	
	//States only used when generating
	List< List<Segment> > listAll = new ArrayList();
	List< List<Segment> > bufferAll = new ArrayList();
	
	/**
	 * Handle a single list for 1 pass.
	 * @param list
	 * @param buffer
	 */
	private void handleSingle(List<Segment> list, List<Segment> buffer, double offset) {
		buffer.clear();
		
		for(Segment s : list) {
			Point ave = average(s.start, s.end);
			float theta = (float) (rand.nextFloat() * Math.PI * 2); //Rand dir across YZ plane
			double sin = MathHelper.sin(theta), cos = MathHelper.cos(theta);
			double off = rand.nextFloat() * offset;
			ave.pt.yCoord += off * sin;
			ave.pt.zCoord += off * cos;
			
			Segment s1 = s, s2 = new Segment(ave, s.end, s.alpha);
			s1.end = ave;
			buffer.add(s1);
			buffer.add(s2);
			
			// Branching with probability
			if(rand.nextDouble() < branchFactor) {
				matrix.setIdentity();
				Vector3f v3f;
				Vec3 dir = scalarMultiply(subtract(ave.pt, s.start.pt), lengthShrink);
				dir = randomRotate(10, dir);
				//matrix.rotate(GenericUtils.randIntv(-50, 50) / 180 * (float)Math.PI, asV3f(random()));
				//dir = applyMatrix(matrix, dir);
				
				double w2 = ave.width * widthShrink;
				Point p1 = new Point(ave.pt, w2),
					p2 = new Point(add(ave.pt, dir), w2);
				List<Segment> toAdd = new ArrayList();
				toAdd.add(new Segment(p1, p2, s.alpha * alphaShrink));
				bufferAll.add(toAdd);
				listAll.add(new ArrayList());
			}
		}
	}
	
	public Arc generate() {
		listAll.clear();
		bufferAll.clear();
		
		Vec3 v0 = vec(0, 0, 0), v1 = vec(length, 0, 0);
		List<Segment> init = new ArrayList();
		init.add(new Segment(
			new Point(v0, width),
			new Point(v1, width),
				1.0));
		listAll.add(init);
		bufferAll.add(new ArrayList());
		
		boolean flip = false;
		double offset = maxOffset;
		for(int i = 0; i < passes; ++i) {
			if(flip) {
				for(int j = 0; j < listAll.size(); ++j) {
					handleSingle(bufferAll.get(j), listAll.get(j), offset);
				}
			} else { 
				for(int j = 0; j < listAll.size(); ++j) {
					handleSingle(listAll.get(j), bufferAll.get(j), offset);
				}
			}
			
			flip = !flip;
			offset /= 2;
		}
		
		return new Arc(flip ? bufferAll : listAll, normal);
	}
	
	static private Vec3 randomRotate(float range, Vec3 dir) {
		float a = (float) (GenericUtils.randIntv(-range, range) / 180 * Math.PI);
		Vec3 ret = VecUtils.copy(dir);
		ret.rotateAroundX((float) GenericUtils.randIntv(-a, a));
		ret.rotateAroundY((float) GenericUtils.randIntv(-a, a));
		ret.rotateAroundZ((float) GenericUtils.randIntv(-a, a));
		return ret;
	}
	
	class Point {
		Vec3 pt;
		double width;
		
		public Point(Vec3 _pt, double _w) {
			pt = _pt;
			width = _w;
		}
	}
	
	private Point average(Point pa, Point pb) {
		Vec3 v = VecUtils.lerp(pa.pt, pb.pt, 0.5);
		return new Point(v, (pa.width + pb.width) / 2);
	}
	
	class Segment {
		Point start, end;
		double alpha;
		
		public Segment(Point s, Point e, double a) {
			start = s;
			end = e;
			alpha = a;
		}
	}
	
	public static class Arc {
		int listId;
		
		public Arc(List< List<Segment> > list, Vec3 normal) {
			buildList(list, normal);
		}
		
		public void draw() {
			RenderUtils.loadTexture(TEXTURE);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_LIGHTING);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glLineWidth(0.4f);
			GL11.glCallList(listId);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_LIGHTING);
			//System.out.println("draw");
		}
		
		private void buildList(List< List<Segment> > list, Vec3 normal) {
			listId = GL11.glGenLists(1);
			
			GL11.glNewList(listId, GL11.GL_COMPILE);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glBegin(GL11.GL_QUADS);
			
			for(List<Segment> l : list) {
				handleSingle(l, normal);
			}
			
			GL11.glEnd();
			
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glEndList();
		}
		
		private void handleSingle(List<Segment> list, Vec3 normal) {
			Vec3 lastDir = null;
			for(Segment s : list) {
				Vec3 dir = randomRotate(10, crossProduct(subtract(s.end.pt, s.start.pt), normal)).normalize();
				if(lastDir == null) lastDir = dir;
				
				Vec3 p1 = add(s.start.pt, scalarMultiply(lastDir, s.start.width)),
					p2 = add(s.start.pt, scalarMultiply(lastDir, -s.start.width)),
					p3 = add(s.end.pt, scalarMultiply(dir, s.end.width)),
					p4 = add(s.end.pt, scalarMultiply(dir, -s.end.width));
				
				GL11.glColor4d(1, 1, 1, s.alpha);
				addVert(p1, 0, 0);
				addVert(p2, 0, 1);
				addVert(p4, 1, 1);
				addVert(p3, 1, 0);
				
				lastDir = dir;
			}
		}
		
		private void addVert(Vec3 vec, double u, double v) {
			GL11.glTexCoord2d(u, v);
			GL11.glVertex3d(vec.xCoord, vec.yCoord, vec.zCoord);
		}
	}
	
}
