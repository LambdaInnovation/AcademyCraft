/**
 * 
 */
package cn.academy.api.client;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.core.client.render.PRHSkillRender;
import cn.liutils.api.client.LIClientRegistry;
import cn.liutils.api.client.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public class SkillRenderingHandler {
	
	public static void init() {
		LIClientRegistry.addPlayerRenderingHelper(new PRHSkillRender());
	}
	
	public static void doRender(AbstractClientPlayer player) {
		GL11.glPushMatrix(); {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			//GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glCullFace(GL11.GL_FRONT);
			//GL11.glDepthFunc(GL11.GL_ALWAYS);
			
			//GL11.glTranslated(0.22, 1, -0.48);
			GL11.glTranslated(-0.5, 0.3, 0);
			GL11.glColor4f(1F, 1F, 1F, .5F);
			Vec3 vs[] = {
				vec(0, 0, 0), 
				vec(1, 0, 0), 
				vec(1, 0, 1), 
				vec(0, 0, 1),
				vec(0, 1, 0),
				vec(1, 1, 0),
				vec(1, 1, 1),
				vec(0, 1, 1)
			};
			//RenderUtils.loadTexture(TEXTURE);
			//MDL_SOLAR.renderAll();
			
			//GL11.glRotated(35, -1, 0, 0);
			int arr[] = {
				4, 3, 2, 1,
				5, 6, 7, 8,
				7, 3, 2, 6,
				//3, 7, 8, 4,
				//8, 6, 1, 5,
				//1, 2, 6, 5
			};
			float scale = 1F;
			GL11.glScalef(scale, scale, -scale);
			
			if(true) {
				Tessellator t = Tessellator.instance;
				t.startDrawingQuads();
				for(int i = 0; i < arr.length; ++i) {
					Vec3 vec = vs[arr[i] - 1];
					RenderUtils.addVertex(vec);
				}
				t.draw();
				
				GL11.glColor4f(1F, 0F, 0F, 1F);
				t.startDrawing(GL11.GL_LINES);
				t.addVertex(0, 0, 0);
				t.addVertex(.5, .5, .5);
				t.draw();
				GL11.glColor4f(0F, 1F, 0F, 1F);
				t.startDrawing(GL11.GL_LINES);
				t.addVertex(.5, .5, .5);
				t.addVertex(1, 1, 1);
				t.draw();
			}
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glCullFace(GL11.GL_BACK);
			//GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glColor4f(1F, 1F, 1F, 1F);
		} GL11.glPopMatrix();
		//System.out.println(type);
	}
	
	/**
	 * 插入ItemRenderer的渲染路径
	 */
	public static void renderFirstPerson() {
		//ItemRenderer a;
		//System.out.println("draw");

		//ItemRenderer
	}
	
	private static Vec3 vec(double x, double y, double z) {
		return Vec3.createVectorHelper(x, y, z);
	}
	
}
