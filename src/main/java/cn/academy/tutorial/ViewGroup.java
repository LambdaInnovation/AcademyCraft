package cn.academy.tutorial;

import cn.academy.client.gui.GuiTutorial.ViewRenderEvent;
import cn.academy.tutorial.ACTutorial.Tag;
import cn.lambdalib2.cgui.Widget;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A view group for tutorial. Provides a tag to be clicked on and a group of sub views.
 */
public interface ViewGroup {

    @SideOnly(Side.CLIENT)
    default Widget[] getSubViews() {
        return new Widget[0];
    }

    @SideOnly(Side.CLIENT)
    default Widget withDraw(Runnable action) {
        Widget ret = new Widget();
        ret.listen(ViewRenderEvent.class, (w, e) -> { action.run(); });
        return ret;
    }

    /**
     * @return The test displayed when hovering on the group button.
     */
    default String getDisplayText() {
        return "";
    }

    Tag getTag();

}