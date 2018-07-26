/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.tutorial;

import cn.academy.misc.tutorial.ACTutorial.Tag;
import cn.academy.misc.tutorial.client.ViewRenderEvent;
import cn.lambdalib2.cgui.gui.Widget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A view group for tutorial. Provides a tag to be clicked on and a group of sub views.
 */
public interface ViewGroup {

    Widget[] NO_VIEWS = new Widget[0];

    @SideOnly(Side.CLIENT)
    default Widget[] getSubViews() {
        return NO_VIEWS;
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
