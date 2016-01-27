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
package cn.academy.crafting.client.gui;

import cn.academy.crafting.block.ContainerMetalFormer;
import cn.academy.crafting.block.TileMetalFormer;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.lambdalib.cgui.gui.CGuiScreenContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.font.IFont.FontOption;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 */
public class GuiMetalFormer extends CGuiScreenContainer {
    
    static WidgetContainer document;
    static {
        document = CGUIDocument.panicRead(new ResourceLocation("academy:guis/metalformer.xml"));
    }
    
    final EntityPlayer player;
    final TileMetalFormer tile;
    
    Widget main;
    
    public GuiMetalFormer(ContainerMetalFormer container) {
        super(container);
        tile = container.tile;
        player = container.player;
        
        initPages();
    }
    
    private void initPages() {
        main = document.getWidget("window_main").copy();
        
        EnergyUIHelper.initNodeLinkButton(tile, main.getWidget("btn_link"));

        main.getWidget("progress_pro").listen(FrameEvent.class, (w, e) -> {
            ProgressBar bar = ProgressBar.get(w);
            bar.progress = tile.getWorkProgress();
            if(bar.progress == 0)
                bar.progressDisplay = 0;
        });

        main.getWidget("progress_imag").listen(FrameEvent.class, (w, event) -> {
            ProgressBar bar = ProgressBar.get(w);
            bar.progress = tile.getEnergy() / tile.getMaxEnergy();

            if(event.hovering) {
                EnergyUIHelper.drawTextBox(
                        String.format("%.1f/%.1fIF", tile.getEnergy(), tile.getMaxEnergy()),
                        event.mx + 10, event.my, new FontOption(18));
            }
        });

        main.getWidget("mark_former").listen(FrameEvent.class, (w, event) -> {
            DrawTexture.get(w).texture = tile.mode.texture;

            if(event.hovering) {
                EnergyUIHelper.drawTextBox(
                        String.format(StatCollector.translateToLocal("ac.gui.metal_former.mode." + tile.mode.toString().toLowerCase()),
                                tile.getEnergy(), tile.getMaxEnergy()),
                        event.mx + 5, event.my, new FontOption(9));
            }
        });

        main.getWidget("mark_former").listen(LeftClickEvent.class, (w, event) -> {
            tile.cycleMode();
            MetalFormerSyncs.cycle(tile);
        });
        
        gui.addWidget(main);
    }
    
}
