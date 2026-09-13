package cn.li.mc262.shim;

import cn.li.mc262.gui.CGuiContainerScreen;
import clojure.lang.IFn;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Universal CGuiContainerScreen skeleton for 26.2.
 * extractRenderState / extractContents replace render / renderBg;
 * Clojure callbacks keep the 1.21.1-shaped args (unwrapped ints / doubles).
 */
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

    public DelegatingCGuiContainerScreen(T menu, Inventory playerInventory, Component title,
                                         int imageWidth, int imageHeight) {
        super(menu, playerInventory, title, imageWidth, imageHeight);
    }

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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (renderFn != null) {
            renderFn.invoke(this, graphics, mouseX, mouseY, partialTick);
        } else {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 26.2 has no renderBg — inject the Clojure bg callback before vanilla contents
     * (labels + slots), mirroring the old renderBg layering.
     */
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (renderBgFn != null) {
            renderBgFn.invoke(this, graphics, partialTick, mouseX, mouseY);
        }
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (renderLabelsFn != null) {
            renderLabelsFn.invoke(this, graphics, mouseX, mouseY);
        } else {
            super.extractLabels(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (mouseClickedFn != null) {
            Object r = mouseClickedFn.invoke(this, event.x(), event.y(), event.button());
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mouseReleasedFn != null) {
            Object r = mouseReleasedFn.invoke(this, event.x(), event.y(), event.button());
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (mouseDraggedFn != null) {
            Object r = mouseDraggedFn.invoke(this, event.x(), event.y(), event.button(), dragX, dragY);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (mouseMovedFn != null) {
            mouseMovedFn.invoke(this, mouseX, mouseY);
        } else {
            super.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseScrolledFn != null) {
            Object r = mouseScrolledFn.invoke(this, mouseX, mouseY, scrollY);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (keyPressedFn != null) {
            Object r = keyPressedFn.invoke(this, event.key(), event.scancode(), event.modifiers());
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (charTypedFn != null) {
            Object r = charTypedFn.invoke(this, (int) event.codepoint(), 0);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
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
     * disposed when the container is actually closed.
     */
    @Override
    public void onClose() {
        if (onCloseFn != null) {
            onCloseFn.invoke(this);
        }
        super.onClose();
    }
}
