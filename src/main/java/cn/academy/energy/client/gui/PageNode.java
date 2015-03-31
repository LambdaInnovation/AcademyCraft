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

import cn.academy.generic.client.ClientProps;
import cn.liutils.api.gui.Widget;

/**
 * @author WeathFolD
 *
 */
public class PageNode extends Widget {
    
    static final double SCALE = 16.0 / 26.0;

    public PageNode() {
        this.setSize(280, 300);
        this.initTexDraw(ClientProps.TEX_GUI_NODE, 0, 0, 280, 300);
        this.scale = SCALE;
    }

}
