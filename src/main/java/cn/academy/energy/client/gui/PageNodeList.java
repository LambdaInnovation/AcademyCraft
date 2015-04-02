package cn.academy.energy.client.gui;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.generic.client.ClientProps;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.DragBar;
import cn.liutils.api.gui.widget.ListVertical;
import cn.liutils.util.HudUtils;
import cn.liutils.util.render.Font.Align;

/**
 * It should guaranteed that when this gui is created, the node list info is ALREADY loaded.
 * @author WeathFolD
 *
 */
public class PageNodeList extends Widget {
    
    static final double SCALE = 0.4;
    static final ResourceLocation TEX = ClientProps.TEX_GUI_NODE_LIST;
    
    final GuiNode guiNode;

    public PageNodeList(GuiNode _guiNode) {
        guiNode = _guiNode;
        this.setSize(280, 320);
        this.initTexDraw(ClientProps.TEX_GUI_NODE_LIST, 0, 0, 280, 320);
        this.scale = SCALE;
        this.alignStyle = AlignStyle.CENTER;
    }
    
    @Override
    public void onAdded() {
        DragBar bar = new ScrollBar();
        addWidget(bar);
        addWidget(new SSIDList().setDragBar(bar));
        
    }
    
    @Override
    public void draw(double mx, double my, boolean hov) {
        this.drawBlackout();
        super.draw(mx, my, hov);
        String header = "Channel select";
        ClientProps.font().draw(header, 160, 26, 16, 0x3cffff, Align.CENTER);
        GL11.glColor4d(1, 1, 1, 1);
    }
    
    class ScrollBar extends DragBar {

        public ScrollBar() {
            super(245, 76, 14, 234, 42);
            this.setTexMapping(TEX, 293, 6, 293, 56, 14, 42);
        }
        
    }
    
    class SSIDList extends ListVertical {

        public SSIDList() {
            super("ssidlist", 24, 87, 208, 230);
            this.setYMargin(6);
        }
        
        @Override
        public void onAdded() {
            for(String s : guiNode.networkList) {
                addWidget(new SingleSSID(s));
            }
        }
        
    }
    
    class SingleSSID extends Widget {
        
        final String ssid;
        
        public SingleSSID(String _ssid) {
            super(208, 26);
            ssid = _ssid;
        }
        
        @Override
        public void draw(double mx, double my, boolean hov) {
            if(hov) {
                GL11.glColor4d(1, 1, 1, .3);
                HudUtils.drawModalRect(0, 0, width, height);
                GL11.glColor4d(1, 1, 1, 1);
            }
            
            final float fSize = 15;
            ClientProps.font().drawTrimmed(ssid, 10, 4, fSize, 0x00ffff,Align.LEFT, 180, "...");
            GL11.glColor4d(1, 1, 1, 1);
        }
    }

}
