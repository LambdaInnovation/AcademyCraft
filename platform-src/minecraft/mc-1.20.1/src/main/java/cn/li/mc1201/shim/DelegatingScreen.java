package cn.li.mc1201.shim;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import clojure.lang.IFn;
import clojure.java.api.Clojure;
import cn.li.mcbase.clj.ClojureInterop;

/** Universal Screen skeleton — replaces all Screen proxy sites.
 *  Takes IFn callbacks via constructor and chained with* setters.
 *  Each callback receives the DelegatingScreen instance as first argument. */
public class DelegatingScreen extends Screen {
    private IFn renderFn;
    private IFn keyPressedFn;
    private IFn charTypedFn;
    private IFn mouseClickedFn;
    private IFn removedFn;
    private IFn onCloseFn;

    // Optional callbacks — set via with* methods
    private IFn mouseReleasedFn;
    private IFn mouseDraggedFn;
    private IFn mouseMovedFn;
    private IFn mouseScrolledFn;
    private IFn isPauseScreenFn;

    // -- Zero-allocation primitive offsets (replace Clojure atom for left/top) --
    public long leftOffset;
    public long topOffset;

    // -- Session context (set once at screen creation, reused every frame) --
    private String sessionId;

    // -- Static Clojure interop for context management (defensive init) --
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
        } catch (Exception e) {
            // Graceful degradation: context push/pop becomes no-op if hooks aren't loaded.
            // This is safe for AOT edge cases — session context is only needed for
            // content-owned event handlers, not CGUI framework itself.
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

    /** Public accessor so Clojure code can call renderBackground on this instance. */
    @Override public void renderBackground(GuiGraphics gg) {
        super.renderBackground(gg);
    }

    // -- with* setters for optional callbacks (builder pattern) --

    public DelegatingScreen withMouseReleased(IFn fn) { this.mouseReleasedFn = fn; return this; }
    public DelegatingScreen withMouseDragged(IFn fn) { this.mouseDraggedFn = fn; return this; }
    public DelegatingScreen withMouseMoved(IFn fn) { this.mouseMovedFn = fn; return this; }
    public DelegatingScreen withMouseScrolled(IFn fn) { this.mouseScrolledFn = fn; return this; }
    public DelegatingScreen withIsPauseScreen(IFn fn) { this.isPauseScreenFn = fn; return this; }
    public DelegatingScreen withClientSession(String id) { this.sessionId = id; return this; }

    // -- Context management helpers (called before/after each callback) --

    /** Push session context, return previous context for restoration. */
    private Object pushCtx() {
        if (sessionId != null && !sessionId.isEmpty() && PUSH_SESSION_CTX != null) {
            return PUSH_SESSION_CTX.invoke(sessionId);
        }
        return null;
    }

    /** Restore session context from pushCtx return value. */
    private void popCtx(Object old) {
        if (sessionId != null && !sessionId.isEmpty() && POP_SESSION_CTX != null) {
            POP_SESSION_CTX.invoke(old);
        }
    }

    /** Clear context after final removed() callback. */
    private void clearCtx() {
        if (sessionId != null && !sessionId.isEmpty() && CLEAR_SESSION_CTX != null) {
            CLEAR_SESSION_CTX.invoke();
        }
    }

    // -- Delegated methods (this passed as first arg to each IFn) --

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (renderFn != null) {
            Object oldCtx = pushCtx();
            try {
                renderFn.invoke(this, graphics, mouseX, mouseY, partialTick);
            } finally {
                popCtx(oldCtx);
            }
        }
    }

    @Override public boolean keyPressed(int key, int scancode, int modifiers) {
        if (keyPressedFn != null) {
            Object r = keyPressedFn.invoke(this, key, scancode, modifiers);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    @Override public boolean charTyped(char ch, int modifiers) {
        if (charTypedFn != null) {
            Object r = charTypedFn.invoke(this, ch, modifiers);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.charTyped(ch, modifiers);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseClickedFn != null) {
            Object oldCtx = pushCtx();
            try {
                Object r = mouseClickedFn.invoke(this, mouseX, mouseY, button);
                return r instanceof Boolean ? (Boolean) r : false;
            } finally {
                popCtx(oldCtx);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseReleasedFn != null) {
            Object oldCtx = pushCtx();
            try {
                Object r = mouseReleasedFn.invoke(this, mouseX, mouseY, button);
                return r instanceof Boolean ? (Boolean) r : false;
            } finally {
                popCtx(oldCtx);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mouseDraggedFn != null) {
            Object oldCtx = pushCtx();
            try {
                Object r = mouseDraggedFn.invoke(this, mouseX, mouseY, button, dragX, dragY);
                return r instanceof Boolean ? (Boolean) r : false;
            } finally {
                popCtx(oldCtx);
            }
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

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseScrolledFn != null) {
            Object r = mouseScrolledFn.invoke(this, mouseX, mouseY, delta);
            return r instanceof Boolean ? (Boolean) r : false;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() {
        if (isPauseScreenFn != null) {
            Object r = isPauseScreenFn.invoke(this);
            return r instanceof Boolean ? (Boolean) r : true;
        }
        return super.isPauseScreen();
    }

    @Override public void removed() {
        if (removedFn != null) {
            Object oldCtx = pushCtx();
            try {
                removedFn.invoke(this);
            } finally {
                popCtx(oldCtx);
                clearCtx();  // Final cleanup — prevent ThreadLocal context leak
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