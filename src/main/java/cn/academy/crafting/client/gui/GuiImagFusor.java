/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.client.gui;

import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.academy.crafting.block.ContainerImagFusor;
import cn.academy.crafting.block.TileImagFusor;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.cgui.gui.CGuiScreenContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.helper.Color;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
@Registrant
public class GuiImagFusor extends CGuiScreenContainer {
    
    static WidgetContainer document;

    @RegInitCallback
    public static void init() {
        document = CGUIDocument.panicRead(new ResourceLocation("academy:guis/imagfusor.xml"));
    }
    
    final TileImagFusor tile;
    
    Widget page;

    public GuiImagFusor(ContainerImagFusor c) {
        super(c);
        tile = c.tile;
        load();
    }
    
    private void wrapButton(final Widget but) {
        final Color idle = new Color(1, 1, 1, 0.3), hover = new Color(1, 1, 1, 1);
        but.listen(FrameEvent.class, (w, event) -> 
        {
            DrawTexture.get(but).color = event.hovering ? hover : idle;
        });
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        GL11.glPushMatrix();
        //Notice: We used a hack to get rid of MC's offset and use absolute offset.
        GL11.glTranslated(-this.guiLeft, -this.guiTop, 0);
        
         Widget widget = gui.getTopWidget(x, y);
         if(widget != null) {
             String text = null;
             if(widget.getName().equals("progress_imag")) {
                 text = tile.getEnergy() + "/" + tile.getMaxEnergy() + " IF";
             } else if(widget.getName().equals("progress_proj")) {
                 text = tile.getLiquidAmount() + "/" + tile.getTankSize() + " mB";
             }
             
             if(text != null) {
                 EnergyUIHelper.drawTextBox(text, x + 5, y + 2, new FontOption(9));
             }
         }
         
         GL11.glPopMatrix();
    }
    
    private void load() {
        gui.addWidget(page = document.getWidget("window_main"));

        ProgressBar progressProduct = ProgressBar.get(page.getWidget("progress_pro")),
                progressProj = ProgressBar.get(page.getWidget("progress_proj")),
                progressImag = ProgressBar.get(page.getWidget("progress_imag"));

        EnergyUIHelper.initNodeLinkButton(tile, page.getWidget("btn_link"));

        page.listen(FrameEvent.class, (w, event) -> {
            progressProduct.progress = tile.getWorkProgress();
            progressProj.progress = (double) tile.getLiquidAmount() / tile.getTankSize();
            progressImag.progress = tile.getEnergy() / tile.getMaxEnergy();

            String str;
            IFRecipe recipe = tile.getCurrentRecipe();
            if(recipe == null) {
                str = "";
            } else {
                str = "" + recipe.consumeLiquid;
            }

            TextBox.get(w.getWidget("text_req")).content = str;
        });
    }

}
