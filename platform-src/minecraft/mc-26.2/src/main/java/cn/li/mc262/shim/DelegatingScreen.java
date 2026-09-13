package cn.li.mc262.shim;

import cn.li.mcbase.clj.ClojureInterop;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Universal Screen skeleton for 26.2 — GuiGraphicsExtractor + KeyEvent/MouseButtonEvent.
 * Clojure callbacks keep the 1.21.1-shaped args (unwrapped ints / doubles).
 */
public class DelegatingScreen extends Screen {
    private IFn renderFn;
    private IFn keyPressedFn;
    private IFn charTypedFn;
    private IFn mouseClickedFn;
    private IFn removedFn;
    private IFn onCloseFn;

    private IFn mouseReleasedFn;
    private IFn mouseDraggedFn;
    private IFn mouseMovedFn;
    private IFn mouseScrolledFn;
    private IFn isPauseScreenFn;

    public long leftOffset;
    public long topOffset;

    private String sessionId;

    private static final IFn PUSH_SESSION_CTX;
    private static final IFn POP_SESSION_CTX;
    private static final IFn CLEAR_SESSION_CTX;
    static {
        IFn push = null, pop = null, clear = null;
        try {
            ClojureInterop.requireNamespace("cn.li.platform.neutral.hooks");
            push = Clojure.var("cn.li.platform.neutral.hooks", "push-session-context!");
            pop = Clojure.var("cn.li.platform.neutral.hooks", "pop-session-context!");
            clear = Clojure.var("cn.li.platform.neutral.hooks", "clear-session-context!");
        } catch (Exception ignored) {
            // AOT / early load: session context optional
        }
        PUSH_SESSION_CTX = push;
        POP_SESSION_CTX = pop;
        CLEAR_SESSION_CTX = clear;
    }

    public DelegatingScreen(Component title,
                            IFn renderFn, IFn keyPressedFn, IFn charTypedFn,
                            IFn mouseClickedFn, IFn removedFn, IFn onCloseFn) {
        super(title);
        this.renderFn = renderFn;
        this.keyPressedFn = keyPressedFn;
        this.charTypedFn = charTypedFn;
        this.mouseClickedFn = mouseClickedFn;
        this.removedFn = removedFn;
        this.onCloseFn = onCloseFn;
    }

    /** Background path for reactive hosts (26.2: extractBackground). */
    public void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    public DelegatingScreen withMouseReleased(IFn fn) { this.mouseReleasedFn = fn; return this; }
    public DelegatingScreen withMouseDragged(IFn fn) { this.mouseDraggedFn = fn; return this; }
    public DelegatingScreen withMouseMoved(IFn fn) { this.mouseMovedFn = fn; return this; }
    public DelegatingScreen withMouseScrolled(IFn fn) { this.mouseScrolledFn = fn; return this; }
    public DelegatingScreen withIsPauseScreen(IFn fn) { this.isPauseScreenFn = fn; return this; }
    public DelegatingScreen withClientSession(String id) { this.sessionId = id; return this; }

    private Object pushCtx() {
        if (sessionId != null && !sessionId.isEmpty() && PUSH_SESSION_CTX != null) {
            return PUSH_SESSION_CTX.invoke(sessionId);
        }
        return null;
    }

    private void popCtx(Object old) {
        if (sessionId != null && !sessionId.isEmpty() && POP_SESSION_CTX != null) {
            POP_SESSION_CTX.invoke(old);
        }
    }

    private void clearCtx() {
        if (sessionId != null && !sessionId.isEmpty() && CLEAR_SESSION_CTX != null) {
            CLEAR_SESSION_CTX.invoke();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (renderFn != null) {
            Object oldCtx = pushCtx();
            try {
                renderFn.invoke(this, graphics, mouseX, mouseY, partialTick);
            } finally {
                popCtx(oldCtx);
            }
        } else {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
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
            Object r = charTypedFn.invoke(this, (char) event.codepoint(), 0);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (mouseClickedFn != null) {
            Object oldCtx = pushCtx();
            try {
                Object r = mouseClickedFn.invoke(this, event.x(), event.y(), event.button());
                return r instanceof Boolean ? (Boolean) r : false;
            } finally {
                popCtx(oldCtx);
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mouseReleasedFn != null) {
            Object oldCtx = pushCtx();
            try {
                Object r = mouseReleasedFn.invoke(this, event.x(), event.y(), event.button());
                return r instanceof Boolean ? (Boolean) r : false;
            } finally {
                popCtx(oldCtx);
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (mouseDraggedFn != null) {
            Object oldCtx = pushCtx();
            try {
                Object r = mouseDraggedFn.invoke(this, event.x(), event.y(), event.button(), dragX, dragY);
                return r instanceof Boolean ? (Boolean) r : false;
            } finally {
                popCtx(oldCtx);
            }
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
    public boolean isPauseScreen() {
        if (isPauseScreenFn != null) {
            Object r = isPauseScreenFn.invoke(this);
            return r instanceof Boolean ? (Boolean) r : true;
        }
        return super.isPauseScreen();
    }

    @Override
    public void removed() {
        if (removedFn != null) {
            Object oldCtx = pushCtx();
            try {
                removedFn.invoke(this);
            } finally {
                popCtx(oldCtx);
                clearCtx();
            }
        }
        super.removed();
    }

    /**
     * Real close only — JEI's RecipesGui replaces this screen via
     * setScreen() (which calls removed() but leaves state open), then
     * restores it on ESC; the AC runtime must survive that swap and only be
     * disposed when the screen is actually closed.
     */
    @Override
    public void onClose() {
        if (onCloseFn != null) {
            onCloseFn.invoke(this);
        }
        super.onClose();
    }
}