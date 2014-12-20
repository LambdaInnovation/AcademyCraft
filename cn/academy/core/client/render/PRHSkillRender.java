/**
 * 
 */
package cn.academy.core.client.render;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.pattern.IPattern;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.api.client.render.PlayerRenderHandler;

/**
 * @author WeathFolD
 *
 */
public class PRHSkillRender implements PlayerRenderHandler {

	public PRHSkillRender() {
	}

	@Override
	public boolean isActivated(EntityPlayer player, World world) {
		return true;
	}

	@Override
	public void renderHead(EntityPlayer player, World world) {}

	@Override
	public void renderBody(EntityPlayer player, World world) {
		AbilityData data = AbilityDataMain.getData(player);
		GL11.glPushMatrix();
		//TODO iterate each active skill
		processSkill(player, data.getCategory().getSkill(0), null);
		GL11.glPopMatrix();
	}
	
	private void processSkill(EntityPlayer player, SkillBase skill, IPattern pattern) {
		//System.out.println("Rendering " + skill.getInternalName());
		SkillRenderer render = skill.getRenderer();
		GL11.glPushMatrix(); {
			render.renderSurroundings(player, pattern);
		}; GL11.glPopMatrix();
	}

}
