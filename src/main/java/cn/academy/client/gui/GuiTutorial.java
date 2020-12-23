package cn.academy.client.gui;

import cn.academy.AcademyCraft;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.Resources;
import cn.academy.tutorial.ACTutorial;
import cn.academy.tutorial.TutorialRegistry;
import cn.academy.tutorial.ViewGroup;
import cn.academy.tutorial.client.ACMarkdownRenderer;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.*;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.GuiEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.MathUtils;
//import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.markdown.GLMarkdownRenderer;
import cn.lambdalib2.util.markdown.MarkdownParser;
import com.google.common.base.Preconditions;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.glu.GLU;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class GuiTutorial extends CGuiScreen {

    private static IFont font, fontBold, fontItalic;

    private static WidgetContainer loaded;

    @StateEventCallback
    private static void __init(FMLInitializationEvent ev) {
        Resources.preloadMipmapTexture("guis/tutorial/logo0");
        Resources.preloadMipmapTexture("guis/tutorial/logo1");
        Resources.preloadMipmapTexture("guis/tutorial/logo2");
        Resources.preloadMipmapTexture("guis/tutorial/logo3");
        loaded = CGUIDocument.read(new ResourceLocation("academy:guis/tutorial.xml"));
        font = Resources.font();
        fontBold = Resources.fontBold();
        fontItalic = Resources.fontItalic();
    }

    private static final double REF_WIDTH = 480;

    private final Color GLOW_COLOR = Colors.white();
    private final FontOption fo_descTitle = new FontOption(10);

    private double cachedWidth = -1;

    private final EntityPlayer player;
    private final List<ACTutorial> learned, unlearned;

    private final boolean firstOpen;

    private Widget frame;
    private Widget leftPart, rightPart;

    private Widget listArea;

    private Widget showWindow, rightWindow, centerPart;

    private Widget logo0, logo1, logo2, logo3;
    private Widget showArea, tagArea;

    // Current displayed tutorial
    private TutInfo currentTut = null;

    private class CachedRenderInfo {
        final String title, rawBrief, rawContent;
        private GLMarkdownRenderer brief_;
        private GLMarkdownRenderer content_;

        CachedRenderInfo(String _title, String _brief, String _content) {
            title = _title;
            rawBrief = _brief;
            rawContent = _content;
        }

        GLMarkdownRenderer getBrief() {
            if (brief_ == null) {
                GLMarkdownRenderer renderer = new ACMarkdownRenderer();
                renderer.setFonts(font, fontBold, fontItalic);
                renderer.widthLimit_$eq(130);
                renderer.fontSize_$eq(8);
                MarkdownParser.accept(rawBrief, renderer);

                brief_ = renderer;
            }
            return brief_;
        }

        GLMarkdownRenderer getContent() {
            if (content_ == null) {
                GLMarkdownRenderer renderer = new ACMarkdownRenderer();
                renderer.setFonts(font, fontBold, fontItalic);
                renderer.widthLimit_$eq(150);
                renderer.fontSize_$eq(8);
                MarkdownParser.accept(rawContent, renderer);

                content_ = renderer;
            }
            return content_;
        }
    }

    private Map<ACTutorial, CachedRenderInfo> cached = new HashMap<>();

    private CachedRenderInfo renderInfo(ACTutorial tut) {
        if (!cached.containsKey(tut)) {
            String raw = tut.getContent();
            int i1 = raw.indexOf("![title]"),
                    i2 = raw.indexOf("![brief]"),
                    i3 = raw.indexOf("![content]");
            if (i1 < i2 && i2 < i3 && i1 != -1) {
                String title = trimHead(raw.substring(i1+8, i2)),
                        brief = trimHead(raw.substring(i2+8, i3)),
                        content = trimHead(raw.substring(i3+10));
                cached.put(tut, new CachedRenderInfo(title, brief, content));
            } else {
                throw new RuntimeException("Malformed tutorial " + tut.id);
            }
        }
        return cached.get(tut);
    }

    private String trimHead(String str) {
        int idx = 0;
        while (idx < str.length() &&
                (str.charAt(idx) == '\r' || str.charAt(idx) == '\n' || str.charAt(idx) == ' ')) {
            idx++;
        }
        return str.substring(idx);
    }

    public GuiTutorial() {
        player = Minecraft.getMinecraft().player;
        Pair<List<ACTutorial>, List<ACTutorial>> p = TutorialRegistry.groupByLearned(player);
        learned = p.getLeft();
        unlearned = p.getRight();

        final String tagName = "AC_Tutorial_Open";
        firstOpen = !player.getEntityData().getBoolean(tagName);
        player.getEntityData().setBoolean(tagName, true);

        initUI();
    }

    @Override
    public void drawScreen(int mx, int my, float w) {
        // Make the whole screen scale with width, for better display effect
        if(cachedWidth != width) {
            frame.transform.scale = (float) (width / REF_WIDTH);
            frame.dirty = true;
        }
        cachedWidth = width;
        super.drawScreen(mx, my, w);
    }

    private void initUI() {
        frame = loaded.getWidget("frame").copy();

        leftPart = frame.getWidget("leftPart");
        listArea = leftPart.getWidget("list");

        rightPart = frame.getWidget("rightPart");

        showWindow = rightPart.getWidget("showWindow");
        rightWindow = rightPart.getWidget("rightWindow");
        centerPart = rightPart.getWidget("centerPart");
        logo0 = rightPart.getWidget("logo0");
        logo1 = rightPart.getWidget("logo1");
        logo2 = rightPart.getWidget("logo2");
        logo3 = rightPart.getWidget("logo3");

        showArea = showWindow.getWidget("area");
        tagArea = showWindow.getWidget("tag_area");

        showWindow.transform.doesDraw = false;
        rightWindow.transform.doesDraw = false;
        centerPart.transform.doesDraw = false;

        // Event handlers
        centerPart.getWidget("text").listen(FrameEvent.class, (w, e) -> {
            if(currentTut != null) {
                GLMarkdownRenderer renderer = renderInfo(currentTut.tut).getContent();

                glPushMatrix();
                glTranslated(0, 0, 10);

                glColorMask(false, false, false, false);
                glDepthMask(true);

                HudUtils.colorRect(0, 0, w.transform.width, w.transform.height);
                glColorMask(true, true, true, true);

                double ht = Math.max(0, renderer.getMaxHeight() - w.transform.height + 10);
                double delta = DragBar.get(centerPart.getWidget("scroll_2")).getProgress() * ht;
                glTranslated(3, 3 - delta, 0);
                glDepthFunc(GL_EQUAL);
                renderer.render();
                glDepthFunc(GL_LEQUAL);
                glPopMatrix();
            }
        });

        rightWindow.getWidget("text").listen(FrameEvent.class, (w, e) -> {
            if(currentTut != null) {
                CachedRenderInfo info = renderInfo(currentTut.tut);

                font.draw(info.title, 3, 3, fo_descTitle);

                glPushMatrix();
                glTranslated(3, 15, 0);
                info.getBrief().render();
                glPopMatrix();
            }
        });

        showArea.listen(FrameEvent.class, (w, e) -> {
            final Widget view = currentView();
            if (view == null) {
                return;
            }

            glMatrixMode(GL11.GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();

            double scale = 366.0 / width * frame.scale;
            float aspect = (float) mc.displayWidth / mc.displayHeight;

            glTranslated(
                    -1 + 2.0 * (w.scale + w.x) / width,
                    1 - 2.0 * (w.scale + w.y) / height,
                    0);
            GL11.glScaled(scale, -scale * aspect, -0.5);

            GLU.gluPerspective(50, 1, 1f, 100);

            glMatrixMode(GL11.GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();

            // glCullFace(GL_FRONT);
            // glDisable(GL11.GL_DEPTH_TEST);
            glDisable(GL11.GL_ALPHA_TEST);
            glEnable(GL11.GL_BLEND);
            glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            glCullFace(GL_FRONT);
            glColor4d(1, 1, 1, 1);

            glTranslated(0, 0, -4);

            glTranslated(.55, .55, .5);

            glScaled(.75, -.75, .75);

            glRotated(-20, 1, 0, 0.1);

            view.post(new ViewRenderEvent());

            glPopMatrix();

            glMatrixMode(GL11.GL_PROJECTION);
            glPopMatrix();

            glMatrixMode(GL11.GL_MODELVIEW);

            glEnable(GL11.GL_DEPTH_TEST);
            glEnable(GL11.GL_ALPHA_TEST);
            glCullFace(GL11.GL_BACK);
        });

        // Left and right button of the preview view.
        // It is assumed when event is triggered, current preview is present and can be switched.

        showWindow.getWidget("btn_left").listen(LeftClickEvent.class, (w, e) -> {
            PreviewInfo info = Preconditions.checkNotNull(currentPreview());
            info.viewIndex -= 1;
            if (info.viewIndex < 0) {
                info.viewIndex = info.subViews.length - 1;
            }

            updateView();
        });

        showWindow.getWidget("btn_right").listen(LeftClickEvent.class, (w, e) -> {
            PreviewInfo info = Preconditions.checkNotNull(currentPreview());
            info.viewIndex = (info.viewIndex + 1) % info.subViews.length;

            updateView();
        });

        {
            FontOption option = new FontOption(10);
            tagArea.listen(FrameEvent.class, (w, evt) -> {
                Widget hovering = gui.getHoveringWidget();
                if (hovering != null) {
                    ViewGroupButton comp = hovering.getComponent(ViewGroupButton.class);
                    if (comp != null) {
                        font.draw(comp.group.getDisplayText(), 0, -8, option);
                    }
                }
            });
        }


        //


        rebuildList();

        final double ln = 500, ln2 = 300, cl = 50;
        final float ht = 5;
        if (!firstOpen) {
            logo1.listen(FrameEvent.class, (w, e) -> {
                glPushMatrix();
                glTranslated(logo1.transform.width / 2, logo1.transform.height / 2 + 15, 0);
                lineglow(ln - ln2, ln, ht);
                lineglow(-ln, -(ln - ln2), ht);
                glPopMatrix();
            });
        } else {
            listArea.transform.doesDraw = false;

            /* Start animation controller */ {
                blend(logo2, 0.65, 0.3);
                blend(logo0, 1.75, 0.3);
                blend(leftPart, 1.75, 0.3);
                blend(logo1, 1.3, 0.3);
                blend(logo3, 0.1, 0.3);
                blendy(logo3, 0.7, 0.4, 63, -36);

                double startTime = GameTimer.getAbsTime();
                logo1.listen(FrameEvent.class, (__, e) -> {
                    final double
                            b1 = 0.3, // Blend stage 1
                            b2 = 0.2; // Blend stage 2

                    glPushMatrix();
                    glTranslated(logo1.transform.width / 2, logo1.transform.height / 2 + 15, 0);
                    double dt = GameTimer.getAbsTime() - startTime - 0.4;
                    if(dt < 0) dt = 0;
                    if(dt < b1) {
                        if(dt > 0) {
                            double len = MathUtils.lerp(0, ln, dt / b1);
                            if(len > cl) {
                                lineglow(cl, len, ht);
                                lineglow(-len, -cl, ht);
                            }
                        }
                    } else {
                        double ldt = dt - b1;
                        if(ldt > b2) {
                            ldt = b2;
                        }
                        double len = ln;
                        double len2 = MathUtils.lerp(ln - 2 * cl, ln2, ldt / b2);
                        lineglow(ln - len2, len, ht);
                        lineglow(-len, -(ln - len2), ht);
                    }

                    glPopMatrix();

                    listArea.transform.doesDraw = dt > 2.0;
                });
            }
        }

        gui.addWidget("frame", frame);
    }

    private void _build(ElementList e1, List<ACTutorial> list, boolean learned) {
        for(ACTutorial t : list) {
            Widget w = new Widget();
            w.transform.setSize(72, 12);
            w.addComponent(new Tint(Colors.whiteBlend(0.0f), Colors.whiteBlend(0.3f)));

            TextBox box = Resources.newTextBox(new FontOption(10, learned ? Colors.white() : Colors.fromFloatMono(0.6f)));
            box.xOffset = 3;
            box.content = renderInfo(t).title;
            box.localized = true;
            box.emit = true;
            box.heightAlign = HeightAlign.CENTER;

            w.listen(LeftClickEvent.class, (__, e) ->
            {
                if(currentTut == null) {
                    // Start blending view area!
                    for(Widget old : new Widget[] { logo2, logo0, logo1, logo3 }) {
                        blend(old, 0, 0.3, true);
                    }
                    centerPart.transform.doesDraw = learned;
                    rightWindow.transform.doesDraw = true;
                    showWindow.transform.doesDraw = true;
                }

                if (currentTut == null || currentTut.tut != t) {
                    updateTutorial(t);
                }
            });

            w.addComponent(box);
            e1.addWidget(w);
        }
    }

    private void rebuildList() {
        listArea.removeComponent(ElementList.class);
        ElementList el = new ElementList();
        _build(el, learned, true);
        _build(el, unlearned, false);
        listArea.addComponent(el);
    }

    private void lineglow(double x0, double x1, float ht) {
        ACRenderingHelper.drawGlow(x0, -1, x1-x0, ht-2, 5, GLOW_COLOR);
        glColor4d(1, 1, 1, 1);
        ACRenderingHelper.lineSegment(x0, 0, x1, 0, ht);
    }

    private void blend(Widget w, double start, double tin) {
        blend(w, start, tin, false);
    }

    private void blend(Widget w, double start, double tin, boolean reverse) {
        DrawTexture dt = DrawTexture.get(w);
        double startTime = GameTimer.getAbsTime();
        int startAlpha = dt.color.getAlpha();
        dt.color.setAlpha(reverse ? startAlpha : 0);

        w.listen(FrameEvent.class, (__, e) ->
        {
            double delta = (GameTimer.getAbsTime() - startTime);
            float alpha = Colors.i2f(startAlpha) *
                    (float)MathUtils.clampd(0, 1, delta < start ? 0 : (delta - start < tin ? (delta - start ) / tin : 1));
            if(reverse) {
                alpha = 1 - alpha;
                if(alpha == 0) {
                    w.dispose();
                }
            }
            dt.color.setAlpha(Colors.f2i(alpha));
        });
    }

    private void blendy(Widget w, double start, double tin, double y0, double y1) {
        double startTime = GameTimer.getAbsTime();
        w.transform.y = (float) y0;
        w.dirty = true;

        w.listen(FrameEvent.class, (__, e) ->
        {
            double delta = (GameTimer.getAbsTime() - startTime);
            double lambda = delta < start ? 0 : (delta - start < tin ? (delta - start ) / tin : 1);
            w.transform.y = (float) MathUtils.lerp(y0, y1, lambda);
            w.dirty = true;
        });
    }

    private void updateTutorial(ACTutorial tut) {
        currentTut = new TutInfo(tut, tut.isActivated(player));
        DragBar.get(centerPart.getWidget("scroll_2")).setProgress(0.0f);

        centerPart.transform.doesDraw = tut.isActivated(player);

        tagArea.clear();

        {
            float sz = tagArea.transform.height;
            double step = sz - 1;

            float x = 0;

            for (int i = 0; i < currentTut.previews.length; ++i) {
                final int i2 = i;
                ViewGroup h = currentTut.previews[i].handler;
                Widget w = new Widget()
                        .size(sz, sz)
                        .pos(x, 0)
                        .addComponent(new ViewGroupButton(h))
                        .addComponent(new DrawTexture(h.getTag().icon, Colors.monoBlend(1, .7f)))
                        .addComponent(new Tint(Colors.monoBlend(1, .7f), Colors.monoBlend(1, 1)).setAffectTexture())
                        .listen(LeftClickEvent.class, (w_, e) -> {
                            currentTut.previewIndex = i2;
                            updatePreview();
                        });
                tagArea.addWidget(w);

                x += step;
            }
        }

        updatePreview();
    }

    private PreviewInfo currentPreview() {
        return currentTut == null ? null : currentTut.currentPreview();
    }

    private Widget currentView() {
        PreviewInfo cp = currentPreview();
        return cp == null ? null : cp.currentView();
    }

    private void updateView() {
        showArea.removeWidget("delegate");

        Widget view = currentView();
        if (view != null) {
            showArea.addWidget("delegate", view);
        }
    }

    private void updatePreview() {
        PreviewInfo current = currentPreview();

        Widget btn_left = showWindow.getWidget("btn_left");
        Widget btn_right = showWindow.getWidget("btn_right");

        boolean hides = current == null || current.subViews.length < 2;

        btn_left.transform.doesDraw = !hides;
        btn_right.transform.doesDraw = !hides;

        updateView();
    }

    private class TutInfo {
        final ACTutorial tut;
        final boolean learned;

        final PreviewInfo[] previews;
        int previewIndex;

        TutInfo(ACTutorial _tut, boolean _learned) {
            tut = _tut;
            learned = _learned;

            previews = tut.getPreview().stream()
                    .map(PreviewInfo::new)
                    .toArray(PreviewInfo[]::new);
        }

        public PreviewInfo currentPreview() {
            return previews.length == 0 ? null : previews[previewIndex];
        }
    }

    private class PreviewInfo {
        final ViewGroup handler;
        final Widget[] subViews;
        int viewIndex;

        public PreviewInfo(ViewGroup handler) {
            this.handler = handler;
            subViews = handler.getSubViews();
        }

        public Widget currentView() {
            return subViews.length == 0 ? null : subViews[viewIndex];
        }
    }

    private class ViewGroupButton extends Component {

        public final ViewGroup group;

        public ViewGroupButton(ViewGroup _group) {
            super("VGB");
            group = _group;
        }

    }

    private void debug(Object msg) {
        AcademyCraft.log.info("[Tut] " + msg);
    }

    public class ViewRenderEvent implements GuiEvent {
    }
}