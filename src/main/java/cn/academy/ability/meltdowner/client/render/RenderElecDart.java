package cn.academy.ability.meltdowner.client.render;

import java.util.Random;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import cn.academy.core.proxy.ACClientProps;
import cn.liutils.template.client.render.entity.RenderIcon;
import cn.liutils.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class RenderElecDart extends RenderIcon {
	public RenderElecDart() {
		super(null);
	}

	Random random = new Random();

	@Override
	public void doRender(Entity var1, double x, double y, double z, float f1, float f2) {
		//GL11.glPushMatrix();
		//GL11.glTranslated(x, y, z);
		//GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		//RenderUtils.drawCube(1, 1, 1, true);
        //GL11.glPopMatrix();
		this.alpha = .8F;
		this.hasLight = false;
		ResourceLocation[] anim = ACClientProps.ANIM_MDBALL_STB;
		RenderUtils.loadTexture(anim[0]);
		this.setSize(1.0f);
		GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		this.doSuperRender(var1, x, y, z, f1, f2);

		//GL11.glEnable(GL11.GL_BLEND);
		//GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		//GL11.glDisable(GL11.GL_CULL_FACE);
		//GL11.glDisable(GL11.GL_LIGHTING);
		//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	public void doSuperRender(Entity par1Entity, double par2, double par4,
			double par6, float par8, float par9) {
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);
			GL11.glDisable(GL11.GL_CULL_FACE);
			//GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_LIGHTING);
            
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			GL11.glPushMatrix(); {
				GL11.glTranslatef((float) par2, (float) par4, (float) par6);
				GL11.glScalef(1.0f, 1.0f, 1.0f);
				
				if(this.viewOptimize) {
					boolean firstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
					if(firstPerson) {
						GL11.glTranslated(fpOffsetX, fpOffsetY, fpOffsetZ);
					} else {
						GL11.glTranslated(tpOffsetX, tpOffsetY, tpOffsetZ);
					}
				}
				
				Tessellator t = Tessellator.instance;
				this.func_77026_a(t);
				
			} GL11.glPopMatrix();
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_CULL_FACE);
	}
	
	private void func_77026_a(Tessellator tessllator) {
		float f4 = 1.0F;
		float f5 = 0.5F;
		float f6 = 0.25F;
		GL11.glRotatef(180F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
		GL11.glColor4f(r, g, b, alpha);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
		tessllator.startDrawingQuads();
		
		
		tessllator.setBrightness(15728880);
		//tessllator.setBrightness(0);
		tessllator.addVertexWithUV(0.0F - f5, 0.0F - f6, 0.0D, 0, 1);
		tessllator.addVertexWithUV(f4 - f5, 0.0F - f6, 0.0D, 1, 1);
		tessllator.addVertexWithUV(f4 - f5, f4 - f6, 0.0D, 1, 0);
		tessllator.addVertexWithUV(0.0F - f5, f4 - f6, 0.0D, 0, 0);
		
		tessllator.draw();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
