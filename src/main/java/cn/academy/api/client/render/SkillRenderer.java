/**
 * 
 */
package cn.academy.api.client.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.SkillState;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The render effect handler of skill. (More precisely, <code>SkillState</code>)
 * All rendering method takes <code>EntityPlayer</code> and <code>SkillState</code> as arguments, 
 * and you are expected to do the render according to <code>SkillState</code>'s state.
 * @author WeathFolD
 * @see cn.academy.api.ctrl.SkillState
 */
public class SkillRenderer {
	
	public static SkillRenderer EMPTY = new SkillRenderer();
	
	public enum HandRenderType {
		FIRSTPERSON,
		EQUIPPED
	}

	public SkillRenderer() {}
	
	/**
	 * Draw the effect on the hand. ideal drawing range (0, 0, 0) to (1, 1, 1)
	 * @param type Which type of render routine are we in?
	 */
	@SideOnly(Side.CLIENT)
	public void renderHandEffect(EntityPlayer player, SkillState state, HandRenderType type) {
		
	}
	
	/**
	 * Draw the effect of player surroundings. The origin point is player's center(at feet pos),
	 * a ideal render range would be (-0.5, 0, -0.5) to (0.5, 1, 0.5).
	 * The region is automatically rotated so the player facing direction is always (0, 0, 1).
	 */
	@SideOnly(Side.CLIENT)
	public void renderSurroundings(EntityPlayer player, SkillState state) {
		
	}
	
	/**
	 * Draw the effect on the screen.
	 */
	@SideOnly(Side.CLIENT)
	public void renderHud(EntityPlayer player, SkillState state, ScaledResolution sr) {
		
	}

}
