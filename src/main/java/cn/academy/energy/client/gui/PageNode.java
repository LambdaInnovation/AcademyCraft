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

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.generic.client.ClientProps;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.RandBufProgressBar;
import cn.liutils.api.gui.widget.RandBufProgressBar.Direction;
import cn.liutils.api.gui.widget.StateButton;
import cn.liutils.util.render.Font.Align;

/**
 * @author WeathFolD
 *
 */
public class PageNode extends Widget {
    
    static final double SCALE = 16.0 / 26.0;
    static final ResourceLocation TEX = ClientProps.TEX_GUI_NODE;

    final GuiNode guiNode;
    
    public PageNode(GuiNode _guiNode) {
        guiNode = _guiNode;
        this.setSize(280, 300);
        this.initTexDraw(ClientProps.TEX_GUI_NODE, 0, 0, 280, 300);
        this.alignStyle = AlignStyle.CENTER;
        this.scale = SCALE;
    }
    
    @Override
    public void onAdded() {
        addWidget(new Widget(115, 42, 51, 57) {
            @Override
            public void draw(double mx, double my, boolean hov) {
                GL11.glPushMatrix();
                GL11.glTranslated(51 / 2, 57 / 2, 0);
                GL11.glRotated(Minecraft.getSystemTime() / 80.0, 0, 0, 1);
                GL11.glTranslated(-51 / 2, -57 / 2, 0);
                super.draw(mx, my, hov);
                GL11.glPopMatrix();
            }
        }.initTexDraw(TEX, 337, 121));
        
        addWidget(new Widget(141, 110, 0, 0) {
            @Override
            public void draw(double mx, double my, boolean hov) {
                ClientProps.font().draw("Loading...", 0, 0, 14, 0x00ffff, Align.CENTER);
            }
        });
    }
    
    void finishedInit() {
        getGui().clearSubWidgets(this);
        
        //Charge Slot
        addWidget(new Widget(29, 127, 38, 38).initTexDraw(TEX, 285, 37));
        
        //Be charged slot
        addWidget(new Widget(211, 69, 38, 38).initTexDraw(TEX, 285, 37));
        
        //Energy outline
        addWidget(new Widget(47, 36, 127, 103).initTexDraw(TEX, 331, 5));
        
        //Wireless signal
        addWidget(new StateButton(33, 19, 39, 34, TEX, new double[][] {
                {283, 78},
                {283, 117}
        }) {
            @Override
            public void buttonPressed(double mx, double my) {
                if(guiNode.listLoaded) {
                    guiNode.openSubPage(new PageNodeList(guiNode));
                }
            }
        });
        
        //Edit node name
        addWidget(new StateButton(163, 15, 12, 13, TEX, new double[][] {
                {311, 2},
                {311, 20}   
        }) {
            @Override
            public void buttonPressed(double mx, double my) {
                guiNode.openSubPage(new PageNodeRename(guiNode));
            }
        });
        
        //Energy bar
        addWidget(new RandBufProgressBar(113, 44, 54, 87, TEX, 396, 121, 54, 87) {
            @Override
            public double getProgress() {
                return 0.4;
            }
        }.setDirection(Direction.UP).setFluctRegion(0));
        
        //Node name
        addWidget(new Widget(139, 18, 0, 0) {
            @Override
            public void draw(double x, double y, boolean hov) {
                final float fSize = 10;
                GL11.glColor4d(0, 1, 1, 1);

                ClientProps.font().drawTrimmed(guiNode.name, 0, 0, fSize, 0x00ffff, Align.CENTER, 54, "...");
                
                GL11.glColor4d(1, 1, 1, 1);
            }
        });
        
        //Current connected ssid
        addWidget(new Widget(224, 128, 0, 0) {
            @Override
            public void draw(double x, double y, boolean hov) {
                final float fSize = 10, step = 15;
                GL11.glColor4d(0, 1, 1, 1);
                ClientProps.font().draw("SSID:", 0, 0, fSize, 0x00ffff, Align.CENTER);
                String ssid = "Imagination Fieldxxxxxxxxxxxx";
                ClientProps.font().drawTrimmed(ssid, 0, step, 10, 0x00ffff, Align.CENTER, 75, "...");
                GL11.glColor4d(1, 1, 1, 1);
            }
        });
    }
    
    abstract class Thunder extends Widget {
        Thunder(double x, double y) {
            super(x, y, 12, 19);
            initTexDraw(TEX, 287, 7);
        }
        
        @Override
        public void draw(double mx, double my, boolean hov) {
            if(!isActive()) return;
            super.draw(mx, my, hov);
        }
        
        abstract boolean isActive();
    }

}
