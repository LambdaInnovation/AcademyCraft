package cn.li.mc1211.shim;

import cn.li.mc1211.gui.CGuiContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import clojure.lang.IFn;

/** Universal CGuiContainerScreen skeleton — replaces all proxy sites
 *  extending CGuiContainerScreen.  Each IFn callback receives the
 *  DelegatingCGuiContainerScreen instance as first argument. */
public class DelegatingCGuiContainerScreen<T extends AbstractContainerMenu>
        extends CGuiContainerScreen<T> {

    private IFn renderFn;
    private IFn renderLabelsFn;
    private IFn renderBgFn;
    private IFn mouseClickedFn;
    private IFn mouseReleasedFn;
    private IFn mouseDraggedFn;
    private IFn mouseMovedFn;
    private IFn mouseScrolledFn;
    private IFn keyPressedFn;
    private IFn charTypedFn;
    private IFn removedFn;
    private IFn onCloseFn;

    public DelegatingCGuiContainerScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // -- with* setters --

    public DelegatingCGuiContainerScreen withRender(IFn fn) { this.renderFn = fn; return this; }
    public DelegatingCGuiContainerScreen withRenderLabels(IFn fn) { this.renderLabelsFn = fn; return this; }
    public DelegatingCGuiContainerScreen withRenderBg(IFn fn) { this.renderBgFn = fn; return this; }
    public DelegatingCGuiContainerScreen withMouseClicked(IFn fn) { this.mouseClickedFn = fn; return this; }
    public DelegatingCGuiContainerScreen withMouseReleased(IFn fn) { this.mouseReleasedFn = fn; return this; }
    public DelegatingCGuiContainerScreen withMouseDragged(IFn fn) { this.mouseDraggedFn = fn; return this; }
    public DelegatingCGuiContainerScreen withMouseMoved(IFn fn) { this.mouseMovedFn = fn; return this; }
    public DelegatingCGuiContainerScreen withMouseScrolled(IFn fn) { this.mouseScrolledFn = fn; return this; }
    public DelegatingCGuiContainerScreen withKeyPressed(IFn fn) { this.keyPressedFn = fn; return this; }
    public DelegatingCGuiContainerScreen withCharTyped(IFn fn) { this.charTypedFn = fn; return this; }
    public DelegatingCGuiContainerScreen withRemoved(IFn fn) { this.removedFn = fn; return this; }
    public DelegatingCGuiContainerScreen withOnClose(IFn fn) { this.onCloseFn = fn; return this; }

    // -- Delegated methods --

    @Override public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (renderFn != null) {
            renderFn.invoke(this, gg, mouseX, mouseY, partialTick);
        } else {
            super.render(gg, mouseX, mouseY, partialTick);
        }
    }

    @Override public void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        if (renderLabelsFn != null) {
            renderLabelsFn.invoke(this, gg, mouseX, mouseY);
        } else {
            super.renderLabels(gg, mouseX, mouseY);
        }
    }

    @Override public void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (renderBgFn != null) {
            renderBgFn.invoke(this, gg, partialTick, mouseX, mouseY);
        }
        // renderBg is abstract in AbstractContainerScreen; no default fallback
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseClickedFn != null) {
            Object r = mouseClickedFn.invoke(this, mouseX, mouseY, button);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseReleasedFn != null) {
            Object r = mouseReleasedFn.invoke(this, mouseX, mouseY, button);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mouseDraggedFn != null) {
            Object r = mouseDraggedFn.invoke(this, mouseX, mouseY, button, dragX, dragY);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public void mouseMoved(double mouseX, double mouseY) {
        if (mouseMovedFn != null) {
            mouseMovedFn.invoke(this, mouseX, mouseY);
        } else {
            super.mouseMoved(mouseX, mouseY);
        }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseScrolledFn != null) {
            Object r = mouseScrolledFn.invoke(this, mouseX, mouseY, scrollY);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyPressedFn != null) {
            Object r = keyPressedFn.invoke(this, keyCode, scanCode, modifiers);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        if (charTypedFn != null) {
            Object r = charTypedFn.invoke(this, (int) codePoint, modifiers);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override public void removed() {
        if (removedFn != null) {
            removedFn.invoke(this);
        } else {
            super.removed();
        }
    }

    /**
     * Real close only — JEI's RecipesGui replaces this screen via
     * setScreen() (which calls removed() but leaves the container open), then
     * restores it on ESC; the AC runtime must survive that swap and only be
     * disposed when the container is actually closed. Minecraft calls onClose
     * on every genuine close path (ESC, close button, container close) but
     * never on a setScreen() replacement, so the runtime teardown belongs
     * here, not in removed().
     */
    @Override public void onClose() {
        if (onCloseFn != null) {
            onCloseFn.invoke(this);
        }
        super.onClose();
    }
}
