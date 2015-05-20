package cn.academy.ability.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Means this class has ability to create a SkillInstance to override
 * a specific ability key. Used in Skill for indexing.
 * @author WeAthFolD
 */
public interface Controllable {

	SkillInstance createSkillInstance(EntityPlayer player);
	
	/**
	 * Return the icon of this controllable. Used in KeyHint display UI.
	 */
	ResourceLocation getHintIcon();
	
	/**
	 * Return the hint text of the controllable. Used in KeyHint display UI.
	 */
	String getHintText();
	
}
