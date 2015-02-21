/**
 * 
 */
package cn.academy.api.client.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The render effect handler of skill.
 * This class works independently and can be added any time into SkillRenderManager.
 * You can either specify a life time and let the manager handle the renderer, or you
 * can handle yourself by calling setDead().
 * For efficiency reason, you can add one instance multiple times simultaneously.
 * That is, you can use only one instance for stateless render effects.
 * @author WeathFolD
 * @see cn.academy.core.client.render.SkillRenderManager
 */
public class SkillRenderer {
	
	public static SkillRenderer EMPTY = new SkillRenderer();
	
	public enum HandRenderType {
		FIRSTPERSON,
		EQUIPPED
	}

	public SkillRenderer() {}
	
	/**
	 * Return true if you want to end this effect.
	 */
	public boolean tickUpdate(EntityPlayer player, long time) {
		return false;
	}
	
	/**
	 * Draw the effect on the hand. ideal drawing range (0, 0, 0) to (1, 1, 1)
	 * @param type Which type of render routine are we in?
	 */
	@SideOnly(Side.CLIENT)
	public void renderHandEffect(EntityPlayer player, HandRenderType type, long time) {
		
	}
	
	/**
	 * Draw the effect of player surroundings. The origin point is player's center(at feet pos),
	 * a ideal rendering range would be (-0.5, 0, -0.5) to (0.5, 1, 0.5).
	 * The region is automatically rotated so the player facing direction is always (0, 0, 1).
	 */
	@SideOnly(Side.CLIENT)
	public void renderSurroundings(EntityPlayer player, long time) {
		
	}
	
	/**
	 * Draw the effect on the screen.
	 */
	@SideOnly(Side.CLIENT)
	public void renderHud(EntityPlayer player, ScaledResolution sr, long time) {
		
	}

}
