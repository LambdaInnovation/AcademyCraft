package cn.academy.misc.tutorial;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Preview handler of ACTutorial. Draws stuffs in preview area.
 */
public interface IPreviewHandler {

	/**
	 * Draw the preview artifact. Range; (0, 0, 0) -> (1, 1, 1)
	 */
	@SideOnly(Side.CLIENT)
	void draw();

}
