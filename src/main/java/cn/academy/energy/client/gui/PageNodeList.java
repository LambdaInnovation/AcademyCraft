package cn.academy.energy.client.gui;

import cn.academy.generic.client.ClientProps;
import cn.liutils.api.gui.Widget;

public class PageNodeList extends Widget {
    
    static final double SCALE = 0.4;

    public PageNodeList() {
        this.setSize(280, 320);
        this.initTexDraw(ClientProps.TEX_GUI_NODE_LIST, 0, 0, 280, 320);
        this.scale = SCALE;
        this.doesDraw = false;
    }

}
