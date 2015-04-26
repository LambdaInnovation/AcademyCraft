/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.client.gui;

import net.minecraft.util.ResourceLocation;
import cn.academy.generic.client.Resources;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.InputBox;
import cn.liutils.util.render.Font.Align;

/**
 * @author WeathFolD
 *
 */
public class PageNodeRename extends GuiNode.Dialogue {

    static final ResourceLocation TEX = PageNode.TEX;
    
    final GuiNode guiNode;
    InputBox name;
    
    public PageNodeRename(GuiNode _guiNode) {
        guiNode = _guiNode;
    }

    @Override
    public void onAdded() {
        addWidget(new Widget(110, 40, 0, 0) {
           @Override
           public void draw(double mx, double my, boolean h) {
               Resources.font().draw("Enter new name:", 0, 0, 13, 0x00ffff, Align.CENTER);
           }
        });
        addWidget(name = new GuiNode.IBox(60.5, 83).setTextColor(0x00ffff));
        addWidget(new GuiNode.OK(65, 130) {
            @Override
            public void buttonPressed(double mx, double my) {
                guiNode.name = name.getContent();
                //TODO: Send message
                guiNode.closeSubPage();
            }
        });
        addWidget(new GuiNode.Cancel(120, 130) {
            @Override
            public void buttonPressed(double mx, double my) {
                guiNode.closeSubPage();
            }
        });
    }
}
