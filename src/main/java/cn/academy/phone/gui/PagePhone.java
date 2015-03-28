/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.phone.gui;

import cn.academy.generic.client.ClientProps;
import cn.liutils.api.gui.Widget;

/**
 * @author WeathFolD
 *
 */
public class PagePhone extends Widget {

    public PagePhone() {
        super(317, 512);
        
        this.initTexDraw(ClientProps.TEX_GUI_PHONE_BACK, 0, 0, 317, 512);
        this.setTexResolution(512, 512);
    }
    
}
