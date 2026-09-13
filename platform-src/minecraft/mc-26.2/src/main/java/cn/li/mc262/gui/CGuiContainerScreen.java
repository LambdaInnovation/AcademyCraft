package cn.li.mc262.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Shared client-only helper subclass.
 * 26.2: imageWidth/imageHeight are final — pass size via constructor.
 * Mouse/key supers wrap 1.21.1-shaped Clojure args into KeyEvent/MouseButtonEvent.
 */
public class CGuiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public CGuiContainerScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    public CGuiContainerScreen(T menu, Inventory inv, Component title, int imageWidth, int imageHeight) {
        super(menu, inv, title, imageWidth, imageHeight);
    }

    /** Background path for reactive hosts (26.2: extractBackground). */
    public void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    /** @deprecated 26.2 image size is constructor-final; no-op kept for call-site compatibility. */
    @Deprecated
    public void setImageSize(int w, int h) {
        // imageWidth/imageHeight are final in 26.2 — construct with sized ctor instead
    }

    public int getImageWidthPublic() {
        return this.imageWidth;
    }

    public int getImageHeightPublic() {
        return this.imageHeight;
    }

    public void setGuiLeft(int left) {
        this.leftPos = left;
    }

    public void setGuiTop(int top) {
        this.topPos = top;
    }

    public int getGuiLeft() {
        return this.leftPos;
    }

    public int getGuiTop() {
        return this.topPos;
    }

    public boolean callSuperMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
    }

    public boolean callSuperMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    public boolean callSuperMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), dragX, dragY);
    }

    public boolean callSuperKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean callSuperCharTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent((int) codePoint));
    }

    public void callSuperRenderBackground(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gg, mouseX, mouseY, partialTick);
    }

    public void callSuperRender(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gg, mouseX, mouseY, partialTick);
    }
}
