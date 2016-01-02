package cn.academy.misc.tutorial;

import cn.lambdalib.cgui.gui.Widget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Preview handler of ACTutorial. Draws stuffs in preview area.
 */
public interface IPreviewHandler {

    /**
     * Draw the preview artifact. Range; (-0.5, -0.5, -0.5) -> (0.5, 0.5, 0.5)
     */
    default void draw() {}

    /**
     * @return A delegating widget to draw the preview area.
     */
    @SideOnly(Side.CLIENT)
    default Widget getDelegateWidget() {
        return null;
    }

}
