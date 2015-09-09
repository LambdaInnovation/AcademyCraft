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
package cn.academy.vanilla.electromaster.client.effect;

import static org.lwjgl.opengl.GL11.*;

import cn.academy.core.client.Resources;
import cn.liutils.render.material.SimpleMaterial;
import cn.liutils.render.mesh.Mesh;
import cn.liutils.render.mesh.MeshUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.client.renderhook.PlayerRenderHook;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class RailgunHandEffect extends PlayerRenderHook {
	
	static final int PER_FRAME = 40, COUNT = 40;
	ResourceLocation[] textures;
	Mesh mesh;
	SimpleMaterial mat;
	
	public RailgunHandEffect() {
		textures = Resources.getEffectSeq("arc_burst", COUNT);
		mesh = MeshUtils.createBillboard(mesh, -1, -1, 1, 1);
		mat = new SimpleMaterial(null);
		mat.ignoreLight = true;
	}

	public void renderHand(boolean firstPerson) {
		long dt = getDeltaTime();
		if(dt >= PER_FRAME * COUNT) {
			dispose();
			return;
		}
		
		int frame = (int) (dt / PER_FRAME);
		glDisable(GL_ALPHA_TEST);
		glDisable(GL_CULL_FACE);
		glPushMatrix();
		if(firstPerson) {
			glTranslated(.26, -.12, -.24);
			glScalef(.4f, .4f, 1f);
		} else {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			glTranslated(0, 0.2, -1);
			glRotated(-player.rotationPitch, 1, 0, 0);
		}
		mat.mainTexture = textures[frame];
		mesh.draw(mat);
		glPopMatrix();
		glEnable(GL_CULL_FACE);
		glEnable(GL_ALPHA_TEST);
	}
	
}
