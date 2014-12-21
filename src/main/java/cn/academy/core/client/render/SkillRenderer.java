/**
 * 
 */
package cn.academy.core.client.render;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ctrl.pattern.IPattern;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The rendering handler per skill. You can judge the skill state by the pattern and pattern state.
 * TODO: Waiting for pattern state implementation
 * @author WeathFolD
 */
public class SkillRenderer {
	
	public enum HandRenderType {
		FIRSTPERSON,
		EQUIPPED
	}

	public SkillRenderer() {}
	
	@SideOnly(Side.CLIENT)
	public void renderHandEffect(EntityPlayer player, IPattern pattern, HandRenderType type) {
		
	}
	
	@SideOnly(Side.CLIENT)
	public void renderSurroundings(EntityPlayer player, IPattern pattern) {
		
	}

}
