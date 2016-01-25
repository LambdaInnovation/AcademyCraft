/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.tutorial.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.academy.core.util.ACMarkdownRenderer;
import cn.academy.misc.tutorial.IPreviewHandler;
import cn.academy.misc.tutorial.TutorialRegistry;
import cn.lambdalib.cgui.gui.CGuiScreen;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.*;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontOption;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.misc.tutorial.ACTutorial;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.Transform.HeightAlign;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.util.markdown.GLMarkdownRenderer;
import cn.lambdalib.util.markdown.MarkdownParser;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author WeAthFolD
 */
public class GuiTutorial extends CGuiScreen {

    static final IFont font = Resources.font(),
            fontBold = Resources.fontBold(),
            fontItalic = Resources.fontItalic();

    static WidgetContainer loaded;
    static {
        loaded = CGUIDocument.panicRead(new ResourceLocation("academy:guis/tutorial.xml"));
    }

    static final double REF_WIDTH = 480;

    final Color GLOW_COLOR = Color.white();
    final FontOption fo_descTitle = new FontOption(10);

    double cachedWidth = -1;

    final EntityPlayer player;
    final List<ACTutorial> learned, unlearned;

    final boolean firstOpen;

    Widget frame;
    Widget leftPart, rightPart;

    Widget listArea;

    Widget showWindow, rightWindow, centerPart;

    Widget logo0, logo1, logo2, logo3;
    Widget centerText, briefText;
    Widget showArea;

    // Current displayed tutorial
    TutInfo currentTut = null;

    class CachedRenderInfo {
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
                String title = raw.substring(i1+8, i2),
                        brief = raw.substring(i2+8, i3),
                        content = raw.substring(i3+10);
                cached.put(tut, new CachedRenderInfo(title, brief, content));
            } else {
                throw new RuntimeException("Malformed tutorial " + tut.id);
            }
        }
        return cached.get(tut);
    }

    public GuiTutorial() {
        player = Minecraft.getMinecraft().thePlayer;
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
            frame.transform.scale = width / REF_WIDTH;
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

        centerText = centerPart.getWidget("text");
        briefText = rightPart.getWidget("text");

        showArea = showWindow.getWidget("area");

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
                double delta = VerticalDragBar.get(centerPart.getWidget("scroll_2")).getProgress() * ht;
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
                glTranslated(3, 18, 0);
                info.getBrief().render();
                glPopMatrix();
            }
        });

        showWindow.getWidget("button_left").listen(LeftClickEvent.class, (w, e) -> currentTut.cycle(-1));
        showWindow.getWidget("button_right").listen(LeftClickEvent.class, (w, e) -> currentTut.cycle(1));

        showArea.listen(FrameEvent.class, (w, e) -> {
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

            currentTut.curHandler().draw();

            glPopMatrix();

            glMatrixMode(GL11.GL_PROJECTION);
            glPopMatrix();

            glMatrixMode(GL11.GL_MODELVIEW);

            glEnable(GL11.GL_DEPTH_TEST);
            glEnable(GL11.GL_ALPHA_TEST);
            glCullFace(GL11.GL_BACK);
        });
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

                long startTime = GameTimer.getAbsTime();
                logo1.listen(FrameEvent.class, (__, e) -> {
                    final double
                            b1 = 0.3, // Blend stage 1
                            b2 = 0.2; // Blend stage 2

                    glPushMatrix();
                    glTranslated(logo1.transform.width / 2, logo1.transform.height / 2 + 15, 0);
                    double dt = (GameTimer.getAbsTime() - startTime) / 1000.0 - 0.4;
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
            w.addComponent(new Tint(Color.whiteBlend(0.0), Color.whiteBlend(0.3)));

            TextBox box = new TextBox(new FontOption(10, learned ? Color.white() : Color.mono(0.6)));
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
                    setCurrentTut(t);
                }
            });

            w.addComponent(box);
            e1.addWidget(w);
        }
    }

    private void rebuildList() {
        listArea.removeComponent("ElementList");
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
        long startTime = GameTimer.getAbsTime();
        double startAlpha = dt.color.a;
        dt.color.a = reverse ? startAlpha : 0;

        w.listen(FrameEvent.class, (__, e) ->
        {
            double delta = (GameTimer.getAbsTime() - startTime) / 1000.0;
            double alpha = startAlpha *
                    MathUtils.clampd(0, 1, delta < start ? 0 : (delta - start < tin ? (delta - start ) / tin : 1));
            if(reverse) {
                alpha = 1 - alpha;
                if(alpha == 0) {
                    w.dispose();
                }
            }
            dt.color.a = alpha;
        });
    }

    private void blendy(Widget w, double start, double tin, double y0, double y1) {
        long startTime = GameTimer.getAbsTime();
        w.transform.y = y0;
        w.dirty = true;

        w.listen(FrameEvent.class, (__, e) ->
        {
            double delta = (GameTimer.getAbsTime() - startTime) / 1000.0;
            double lambda = delta < start ? 0 : (delta - start < tin ? (delta - start ) / tin : 1);
            w.transform.y = MathUtils.lerp(y0, y1, lambda);
            w.dirty = true;
        });
    }

    private void setCurrentTut(ACTutorial tut) {
        currentTut = new TutInfo(tut, tut.isActivated(player));
        boolean cycleable = tut.getPreview().size() > 1;
        showArea.removeWidget("delegate");
        VerticalDragBar.get(centerPart.getWidget("scroll_2")).setProgress(0.0);
        showWindow.getWidget("button_left").transform.doesDraw
                = showWindow.getWidget("button_right").transform.doesDraw
                = cycleable;
        centerPart.transform.doesDraw = tut.isActivated(player);
        currentTut.cycle(0);
    }

    private class TutInfo {
        final ACTutorial tut;
        final boolean learned;

        int selection;

        TutInfo(ACTutorial _tut, boolean _learned) {
            tut = _tut;
            learned = _learned;
        }

        void cycle(int delta) {
            int len = tut.getPreview().size();
            selection += delta;
            if(selection >= len) selection = 0;
            else if(selection < 0) selection = len - 1;

            showArea.removeWidget("delegate");
            Widget w = curHandler().getDelegateWidget();
            if(w != null)
                showArea.addWidget("delegate", w);

            debug(curHandler());
        }

        IPreviewHandler curHandler() {
            return tut.getPreview().get(selection);
        }
    }

    private void debug(Object msg) {
        AcademyCraft.log.info("[Tut] " + msg);
    }

}
