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
package cn.academy.ability.electro.client.render.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.client.render.CubePointFactory;
import cn.academy.ability.electro.client.render.IPointFactory;
import cn.academy.ability.electro.client.render.IPointFactory.NormalVert;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.draw.DrawObject;
import cn.liutils.api.draw.prop.AssignTexture;
import cn.liutils.api.draw.prop.DisableLight;
import cn.liutils.api.draw.prop.Transform;
import cn.liutils.api.draw.tess.Rect;
import cn.liutils.api.render.IDrawable;
import cn.liutils.util.misc.Pair;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class SRSmallCharge extends SkillRenderer implements IDrawable {
	
	IPointFactory poi;
	
	@SideOnly(Side.CLIENT)
	ResourceLocation[] TEX = ACClientProps.ANIM_SMALL_ARC;
	
	private static Random RNG = new Random();
	
	int itensity;
	
	double dra;
			
	class ArcObject extends DrawObject {
		public Rect rect;
		public AssignTexture tex;
		
		public ArcObject(NormalVert vt, double size) {
			addHandler(rect = new Rect(size, size));
			rect.setCentered();
			
			addHandler(tex = new AssignTexture(null) {
				long lastTime = 0;
				@Override
				public void onEvent(EventType event, DrawObject obj) {
					long time = Minecraft.getSystemTime();
					if(lastTime == 0 || time - lastTime > 100) {
						lastTime = time;
						texture = TEX[RNG.nextInt(TEX.length)];
					}
					super.onEvent(event, obj);
				}
			});
			
			Pair<Double, Double> angles = vt.getNormalAngles();
			addHandler(new Transform().setOffset(vt.vert.xCoord, vt.vert.yCoord, vt.vert.zCoord)
					.setRotation(angles.first + (RNG.nextDouble() - .5) * 60, angles.second + (RNG.nextDouble() - .5) * 60, 0));
			
			addHandler(DisableLight.instance());
		}
	}
	
	List<ArcObject> arcs = new ArrayList();
	
	public SRSmallCharge(int iten, double size) {
		this(iten, size, 1, 2, 1);
	}
	
	public SRSmallCharge(int iten, double size, double sx, double sy, double sz) {
		poi = new CubePointFactory(sx, sy, sz);
		int n = iten + RNG.nextInt((int)(iten * .4));
		for(int i = 0; i < n; ++i) {
			arcs.add(new ArcObject(poi.next(), size));
		}
	}
	
	public void setTex(ResourceLocation[] ts) {
		this.TEX = ts;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void renderHandEffect(EntityPlayer player, HandRenderType type, long time) {
		if(type == HandRenderType.EQUIPPED)
			return;
		draw();
	}

	@Override
	public void draw() {
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDepthMask(false);
		GL11.glPushMatrix(); 
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4d(1, 1, 1, 0.7);
		
		GL11.glTranslated(0, 0.9, 0.2);
		GL11.glRotated(120, 1, 0, 0);
		GL11.glScaled(0.5, 0.5, 0.5);
		//RenderUtils.drawCube(1, 1, 2);
		for(ArcObject arc : arcs) {
			arc.draw();
		}
		
		GL11.glPopMatrix();
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

}
