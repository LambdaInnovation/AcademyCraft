package cn.academy.support.jei;

import cn.academy.AcademyCraft;
import codechicken.nei.api.*;

/**
 * 
 * @author KSkun
 *
 */
public class NEIACConfig implements IConfigureNEI {
    @Override
    public void loadConfig() {
        /*
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
                return ui != null && ui.getGui().getTopWidget(x, y) != null;
            }

            private ContainerUI check(GuiContainer gui) {
                if (gui instanceof ContainerUI) {
                    return ((ContainerUI) gui);
                } else {
                    return null;
                }
            }
        });*/
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