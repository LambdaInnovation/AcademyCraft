/**
 * 
 */
package cn.academy.core.client.render;

import java.lang.annotation.Inherited;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.SkillStateManager;
import cn.academy.api.ctrl.pattern.IPattern;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.api.render.IPlayerRenderHook;

/**
 * @author WeathFolD
 *
 */
public class PRHSkillRender implements IPlayerRenderHook {

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
		GL11.glPushMatrix(); {
			for(SkillState s : SkillStateManager.getState(player)) {
				processSkill(player, s);
			}
		} GL11.glPopMatrix();
	}
	
	private void processSkill(EntityPlayer player, SkillState state) {
		SkillRenderer render = state.getRender();
		GL11.glPushMatrix(); {
			render.renderSurroundings(player, state);
		}; GL11.glPopMatrix();
	}

}
