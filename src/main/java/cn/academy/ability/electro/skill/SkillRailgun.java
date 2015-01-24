/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.internal.PatternDown;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 传说中的超电磁炮~~！
 * TODO 施工中
 * @author WeathFolD
 */
@RegistrationClass
public class SkillRailgun extends SkillBase {

	public SkillRailgun() {
	}
	
	@RegistrationClass
	@RegAuxGui
	@SideOnly(Side.CLIENT)
	public static class GuiEffect extends AuxGui {

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void draw(ScaledResolution sr) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glPushMatrix(); {
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				GL11.glColor4d(0, 0, 0, 0.3);
				HudUtils.setZLevel(0);
				//HudUtils.drawModalRect(0, 0, sr.getScaledWidth_double(), sr.getScaledHeight_double());
				HudUtils.setZLevel(-90);
				
			} GL11.glPopMatrix();
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GLU.gluPerspective(120f, (float) (sr.getScaledWidth_double() / sr.getScaledHeight_double()), 0.05f, 100f);
			GL11.glPushMatrix(); {
				GL11.glColor4d(1, 1, 1, 1);
				HudUtils.setZLevel(10);
				HudUtils.drawModalRect(-1, -1, 100, 100);
				HudUtils.setZLevel(-90);
			} GL11.glPopMatrix();
			
			Minecraft.getMinecraft().entityRenderer.setupOverlayRendering();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
			
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public boolean onKeyDown(EntityPlayer player) {
				if(!player.worldObj.isRemote) {
					player.worldObj.spawnEntityInWorld(new EntityRailgun(player));
				}
				return true;
			}
			
		});
	}
	
	public String getInternalName() {
		return "em_railgun";
	}
	
	public int getMaxSkillLevel() {
		return 200;
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_RAILGUN;
	}

}
