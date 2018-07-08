package cn.academy.medicine.blocks;

import cn.academy.core.Resources;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.Page;
import cn.academy.core.client.ui.WirelessPage;
import cn.academy.medicine.MSNetEvents;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.Component;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.s11n.network.NetworkMessage;
import scala.collection.JavaConverters;

import java.util.Arrays;

public class GuiMedSynthesizer {
    private static Widget template = null;

    public static TechUI.ContainerUI apply(ContainerMedSynthesizer container){
        TileMedSynthesizer tile = container.tile;
        if(template==null)
            template = CGUIDocument.panicRead(Resources.getGui("rework/page_med_synth")).getWidget("main");//lazy load

        Widget invWidget = template.copy();

        Page invPage = InventoryPage.apply(invWidget);
        Page wirelessPage = WirelessPage.userPage(tile);

        invWidget.getWidget("btn_go").listen(LeftClickEvent.class,
                ((w, e) -> NetworkMessage.sendToServer(MSNetEvents.instance, MSNetEvents.MSG_BEGIN_SYNTH, tile)));

        {
            Widget widget = invWidget.getWidget("progress");
            ProgressBar progress = widget.getComponent(ProgressBar.class);
            widget.listen(FrameEvent.class, (w, e) -> progress.progress = tile.synthProgress());
        }

        return new TechUI.ContainerUI(container,  JavaConverters.asScalaIteratorConverter
                (Arrays.asList(invPage, wirelessPage).iterator()).asScala().toSeq());
    }
}
