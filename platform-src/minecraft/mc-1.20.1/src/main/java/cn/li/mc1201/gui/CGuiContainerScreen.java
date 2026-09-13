package cn.li.mc1201.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/**
 * Shared client-only helper subclass exposing selected protected screen APIs for Clojure proxy usage.
 *
 * <p>This class intentionally avoids loader-specific annotations so it can live in the shared mc1201
 * module. Callers must ensure this type is only instantiated from client execution paths.</p>
 */
public abstract class CGuiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public CGuiContainerScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * Explicit pass-through so container screens always invoke the vanilla/Screen background path.
     * Some mods (e.g. JEI) expect this override to exist on custom container screen subclasses.
     */
    @Override
    public void renderBackground(GuiGraphics gg) {
        super.renderBackground(gg);
    }

    /** Set both imageWidth and imageHeight. */
    public void setImageSize(int w, int h) {
        this.imageWidth = w;
        this.imageHeight = h;
    }

    public int getImageWidthPublic() {
        return this.imageWidth;
    }

    public int getImageHeightPublic() {
        return this.imageHeight;
    }

    public int getGuiLeft() {
        return this.leftPos;
    }

    public int getGuiTop() {
        return this.topPos;
    }

    public int getXSize() {
        return this.imageWidth;
    }

    public int getYSize() {
        return this.imageHeight;
    }

    public void callSuperSlotClicked(Slot slot, int slotId, int button, ClickType actionType) {
        super.slotClicked(slot, slotId, button, actionType);
    }

    public boolean callSuperMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean callSuperMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean callSuperMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean callSuperKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean callSuperCharTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    public void callSuperRenderBackground(GuiGraphics gg) {
        super.renderBackground(gg);
    }

    public void callSuperRender(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
    }

    public void callSuperRenderTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        super.renderTooltip(gg, mouseX, mouseY);
    }
}
