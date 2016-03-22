/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.nei;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.*;
import codechicken.nei.recipe.IRecipeHandler;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * 
 * @author KSkun
 *
 */
public class NEIACConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.registerRecipeHandler(new FusorRecipeHandler());
        API.registerUsageHandler(new FusorRecipeHandler());
        API.registerRecipeHandler(new MetalFormerRecipeHandler());
        API.registerUsageHandler(new MetalFormerRecipeHandler());
        API.registerNEIGuiHandler(new INEIGuiHandler() {
            @Override
            public VisiblityData modifyVisiblity(GuiContainer guiContainer, VisiblityData visiblityData) {
                return null;
            }

            @Override
            public Iterable<Integer> getItemSpawnSlots(GuiContainer guiContainer, ItemStack itemStack) {
                return Collections.emptyList();
            }

            @Override
            public List<TaggedInventoryArea> getInventoryAreas(GuiContainer guiContainer) {
                return Collections.emptyList();
            }

            @Override
            public boolean handleDragNDrop(GuiContainer guiContainer, int i, int i1, ItemStack itemStack, int i2) {
                return false;
            }

            @Override
            public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
                ContainerUI ui = check(gui);
                System.out.println("Checked");
                return ui.getGui().getTopWidget(x, y) != null;
            }

            private ContainerUI check(GuiContainer gui) {
                if (gui instanceof ContainerUI) {
                    return ((ContainerUI) gui);
                } else {
                    return null;
                }
            }
        });
    }

    @Override
    public String getName() {
        return "AcademyCraft";
    }

    @Override
    public String getVersion() {
        return AcademyCraft.VERSION;
    }

}
