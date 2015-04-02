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

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.ResourceLocation;
import cn.academy.energy.block.ContainerNode;
import cn.academy.energy.block.TileNode;
import cn.liutils.api.gui.LIGuiContainer;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.InputBox;
import cn.liutils.api.gui.widget.StateButton;

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
    PageNode mainPage;
    Widget openingPage;

    TileNode node;
    
    public GuiNode(ContainerNode c) {
        super(c);
        
        initWidgets();
        
        //DEBUG
        receivedSimpleMessage(true, "wwwwww");
        receivedListMessage(Arrays.asList("aaa", "bbb", "loooooooooooooooooooooooooooooooooooooooooooong", "FreakingOut", "Whatever", "EzioAuditore", "SakuraYuki", "Meow",
                "loooooooooooooooooooooooooooooooooooooooooooong"));
        //DEBUG END
        
        node = c.node;
    }
    
    public void receivedSimpleMessage(boolean con, String _name) {
        isConnected = con;
        name = _name;
        loaded = true;
        mainPage.finishedInit();
    }
    
    public void receivedListMessage(List<String> list) {
        networkList = list;
        listLoaded = true;
    }
    
    private void initWidgets() {
        gui.addWidget(mainPage = new PageNode(this));
    }
    
    public void openSubPage(Widget sub) {
        if(openingPage != null) {
            throw new RuntimeException();
        }
        gui.addWidget(openingPage = sub);
        mainPage.doesListenKey = false;
    }
    
    public void closeSubPage() {
        openingPage.dispose();
        openingPage = null;
        mainPage.doesListenKey = true;
    }
    
    @Override
    public boolean isSlotActive() {
        return mainPage.doesListenKey && loaded;
    }
    
    //Some publicly-used widgets
    static class Dialogue extends Widget {
        public Dialogue() {
            super(0, 310, 216, 189);
            initTexDraw(PageNode.TEX, 0, 310);
            this.alignStyle = alignStyle.CENTER;
            this.scale = 0.5;
        }
        
        @Override
        public void draw(double x, double y, boolean hov) {
            drawBlackout();
            super.draw(x, y, hov);
        }
    }
    
    static class IBox extends InputBox {
        public IBox(double x, double y) {
            super(x, y, 95, 20, 14, 1, 30);
            initTexDraw(PageNode.TEX, 231, 350);
        }
    }
    
    static abstract class Confirm extends StateButton {
        public Confirm(double x, double y) {
            super(x, y, 27, 27, PageNode.TEX, 27, 27, new double[][] {
                {330, 185}, 
                {330, 240}
            });
        }
    }
    
    static abstract class Cancel extends StateButton {
        public Cancel(double x, double y) {
            super(x, y, 27, 27, PageNode.TEX, 27, 27, new double[][] {
                {364, 185}, 
                {364, 240}
            });
        }
    }
    
    static abstract class OK extends StateButton {
        public OK(double x, double y) {
            super(x, y, 27, 27, PageNode.TEX, 27, 27, new double[][] {
                {330, 212}, 
                {330, 268}
            });
        }
    }

}
