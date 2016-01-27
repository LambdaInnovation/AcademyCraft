/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.gui.wind;

import cn.academy.energy.block.wind.ContainerWindGenMain;
import cn.academy.energy.block.wind.TileWindGenMain;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.lambdalib.cgui.gui.CGuiScreenContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.font.IFont.FontAlign;
import cn.lambdalib.util.client.font.IFont.FontOption;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 */
public class GuiWindGenMain extends CGuiScreenContainer {
    
    static final WidgetContainer loaded = CGUIDocument.panicRead(new ResourceLocation("academy:guis/wind_main.xml"));
    
    TileWindGenMain tile;
    
    Widget main;

    public GuiWindGenMain(ContainerWindGenMain c) {
        super(c);
        tile = c.tile;
        
        initScene();
    }
    
    void initScene() {
        main = loaded.getWidget("main").copy();
        
        gui.addWidget(main);

        main.getWidget("disabled").listen(FrameEvent.class, (w, e) -> {
            DrawTexture dt = DrawTexture.get(w);
            dt.enabled = !tile.complete;

            if(!tile.complete) {
                String text = StatCollector.translateToLocal("ac.gui.wind.structure");
                EnergyUIHelper.drawTextBox(text, 10, -40, 233333, new FontOption(20, FontAlign.CENTER));
            } else if(!tile.noObstacle) {
                String text = StatCollector.translateToLocal("ac.gui.wind.obstacle");
                EnergyUIHelper.drawTextBox(text, 10, -40, 233333, new FontOption(20, FontAlign.CENTER));
            }
        });
    }

}
