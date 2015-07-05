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
package cn.academy.vanilla.electromaster.client.renderer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.vanilla.electromaster.client.renderer.ArcFactory.Arc;
import cn.liutils.util.client.RenderUtils;

/**
 * Create one for each entity that you wanna use to draw subArc. Provide the template pre-generated and
 * this class handles everything else.
 * @author WeAthFolD
 */
public class SubArcHandler {

	public final Arc[] arcs;
	
	List<SubArc> list = new LinkedList();
	
	public double frameRate = 1.0, switchRate = 1.0;
	
	public SubArcHandler(Arc[] _arcs) {
		arcs = _arcs;
	}
	
	public SubArc generateAt(Vec3 pos) {
		SubArc sa = new SubArc(pos, arcs.length);
		sa.frameRate = frameRate;
		sa.switchRate = switchRate;
		list.add(sa);
		
		return sa;
	}
	
	public void tick() {
		Iterator<SubArc> iter = list.iterator();
		while(iter.hasNext()) {
			SubArc sa = iter.next();
			if(sa.dead)
				iter.remove();
			else
				sa.tick();
		}
	}
	
	public void clear() {
		list.clear();
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public void drawAll() {
		Iterator<SubArc> iter = list.iterator();
		
		GL11.glDepthMask(false);
		while(iter.hasNext()) {
			SubArc arc = iter.next();
			if(!arc.dead && arc.draw) {
				
				GL11.glPushMatrix();
				RenderUtils.glTranslate(arc.pos);
				GL11.glRotated(arc.rotZ, 0, 0, 1);
				GL11.glRotated(arc.rotY, 0, 1, 0);
				GL11.glRotated(arc.rotX, 1, 0, 0);
				
				final double scale = 0.3;
				GL11.glScaled(scale, scale, scale);
				GL11.glTranslated(-arcs[arc.texID].length / 2, 0, 0);
				arcs[arc.texID].draw();
				GL11.glPopMatrix();
			}
		}
		GL11.glDepthMask(true);
	}

}
