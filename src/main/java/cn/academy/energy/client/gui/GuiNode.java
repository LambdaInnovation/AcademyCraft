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

import java.util.ArrayList;
import java.util.List;

import cn.academy.energy.block.ContainerNode;
import cn.academy.energy.block.TileNode;
import cn.liutils.api.gui.LIGuiContainer;
import cn.liutils.api.gui.Widget;

/**
 * @author WeathFolD
 *
 */
public class GuiNode extends LIGuiContainer {
    
    //Synchronized state
    boolean listLoaded = false;
    List<String> networkList;
    
    boolean isConnected = false;
    String name;
    boolean loaded = false;
    
    //GUI state
    Widget mainPage, listPage;

    TileNode node;
    
    public GuiNode(ContainerNode c) {
        super(c);
        node = c.node;
        initWidgets();
    }
    
    public void receivedSimpleMessage(boolean con, String _name) {
        isConnected = con;
        name = _name;
        loaded = true;
    }
    
    public void receivedListMessage(List<String> list) {
        networkList = list;
        listLoaded = true;
    }
    
    private void initWidgets() {
        gui.addWidget(mainPage = new PageNode());
        gui.addWidget(listPage = new PageNodeList());
    }

}
