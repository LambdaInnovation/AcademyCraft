package cn.academy.terminal.app;

import cn.academy.Resources;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.DragBar;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.ResourceUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class AppAbout extends App {

    @RegApp(priority = -2)
    private static final AppAbout instance = new AppAbout();

    public AppAbout() {
        super("about");
        setPreInstalled();
    }

    @Override
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @SideOnly(Side.CLIENT)
            public void onStart() {
                Minecraft.getMinecraft().displayGuiScreen(new AboutUI());
            }
        };
    }

    static class AboutUI extends CGuiScreen {

        private static final WidgetContainer Prefab = CGUIDocument.read(Resources.getGui("about"));

        private static final int MaskZLevel = 100;

        private static final float FontSize = 30;

        private static final List<TextItem>
            CreditTexts = new ArrayList<>(),
            DonateTexts = new ArrayList<>();

        private static float creditsMaxY;

        static {
            Resources.preloadMipmapTexture("guis/about/bg");

            Config cfg = ConfigFactory.parseReader(
                new InputStreamReader(ResourceUtils.getResourceStream(Resources.res("config/about.conf")))
            );

            {
                Config root = cfg.getConfig("credits");
                List<TextItem> l = CreditTexts;

                float y = 2 * FontSize;
                for (String s : root.getStringList("header")) {
                    l.add(new TextItem(0, y, s, FontAlign.CENTER).setBold());
                    y += FontSize;
                }

                y += 2 * FontSize;
                for (ConfigValue e : root.getList("staff")) {
                    ConfigList el = (ConfigList) e;
                    String job = (String) el.get(0).unwrapped();
                    l.add(new TextItem(-30, y, job, FontAlign.RIGHT).setBold());

                    for (int i = 1; i < el.size(); ++i) {
                        String name = ((String) el.get(i).unwrapped());
                        l.add(new TextItem(30, y, name, FontAlign.LEFT));
                        y += FontSize;
                    }
                    y += 0.5f * FontSize;
                }

                y += FontSize;
                l.add(new TextItem(0, y, "Donators", FontAlign.CENTER).setBold());
                y += 1.5f * FontSize;

                List<String> donators = root.getStringList("donators");
                for (int i = 0; i < donators.size(); ++i) {
                    float x = (i % 2 == 0 ? 1 : -1) * (620 / 4.0f);
                    l.add(new TextItem(x, y, donators.get(i), FontAlign.CENTER));

                    if (i % 2 != 0) y += FontSize;
                }

                y += FontSize;
                l.add(
                    new TextItem(0, y, "Thank you for playing!", FontAlign.CENTER).setBold()
                );
                y += FontSize;

                creditsMaxY = y + 30;
            }

            {
                Config root = cfg.getConfig("donation");

                String lang = Minecraft.getMinecraft().gameSettings.language;
                if (!root.hasPath(lang))
                    lang = "en_us";

                List<String> l = root.getStringList(lang);

                float y = 100;
                for (int i = 0; i < l.size(); ++i) {
                    String s = l.get(i);
                    if (s.startsWith("!!")) {
                        int ix = s.indexOf('|');
                        String url = s.substring(ix + 1);
                        String text = s.substring(2, ix);

                        y += 10;
                        DonateTexts.add(
                            new LinkItem(0, y, text, url, FontAlign.CENTER)
                                .setFontSize(40)
                        );
                        y += 50;
                    } else {
                        DonateTexts.add(
                            new TextItem(0, y, l.get(i), FontAlign.CENTER)
                        );
                        y += 30;
                    }
                }
            }

        }

        private static final Color
            ColorTextDisable = Colors.white(),
            ColorTextEnable = Colors.fromRGB32(0x3d3f4b),
            ColorBtnDisable = Colors.whiteBlend(0.2f),
            ColorBtnEnable = Colors.whiteBlend(0.5f);

        private static class TextItem {
            public float x, y;
            public String text;
            public FontAlign align;
            public boolean bold;
            public float fontSize = FontSize;

            public TextItem(float x, float y, String text, FontAlign align) {
                this.x = x;
                this.y = y;
                this.text = text;
                this.align = align;
            }

            public TextItem setBold() {
                this.bold = true;
                return this;
            }

            public TextItem setFontSize(float sz) {
                fontSize = sz;
                return this;
            }
        }

        private static class LinkItem extends TextItem {
            public String url;

            public LinkItem(float x, float y, String text, String url, FontAlign align) {
                super(x, y, text, align);
                this.url = url;
            }
        }

        enum TabType { Credits, Donate }

        Widget _btnCredits, _btnDonate;

        TabType _tabType = TabType.Credits;

        DragBar _dragBar;

        Widget _scrollArea;

        String _hoveringURL;

        AboutUI() {
            Widget root = Prefab.getWidget("main").copy();
            gui.addWidget("main", root);

            _btnCredits = root.getWidget("area/btn_credits");
            _btnDonate = root.getWidget("area/btn_donate");
            _dragBar = root.getWidget("area/drag_bar").getComponent(DragBar.class);

            _btnCredits.listen(LeftClickEvent.class, (w, e) -> onTabTypeChanged(TabType.Credits));
            _btnDonate.listen(LeftClickEvent.class, (w, e) -> onTabTypeChanged(TabType.Donate));

            _dragBar.widget.listen(DragBar.DraggedEvent.class, (w, e) -> {
                Debug.log("Dragged: " + _dragBar.getProgress());
            });

            _scrollArea = root.getWidget("area/scroll_area");

            FontOption option = new FontOption(30, FontAlign.LEFT, Colors.white());

            Color
                textColor = Colors.white(),
                linkColor = Colors.fromRGB32(0x54a1f0),
                linkColorHighlight = Colors.fromRGB32(0x9fbfe0);

            _scrollArea.listen(LeftClickEvent.class, (w, e) -> {
                // This is somewhat hack, using manual position judgement.
                //  but it would otherwise be much more of a fuss,
                //  using widgets for every single line.
                Debug.log("Left click" + _hoveringURL);
                if (_hoveringURL != null) {
                    try {
                        // https://stackoverflow.com/questions/5226212/how-to-open-the-default-webbrowser-using-java
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(new URI(_hoveringURL));
                        }
                    } catch (URISyntaxException | IOException ex) {
                        ex.printStackTrace();
                    }
                    _hoveringURL = null;
                }
            });

            _scrollArea.listen(FrameEvent.class, -1, (w, e) -> {
                float yOffset = _tabType == TabType.Credits ?
                    _dragBar.getProgress() * (creditsMaxY - w.transform.height + 50) :
                    0;
                List<TextItem> list = _tabType == TabType.Credits ? CreditTexts : DonateTexts;

                GL11.glPushMatrix();

                GL11.glTranslated(w.transform.width / 2, 0, MaskZLevel);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_EQUAL);

                String hoveringURL = null;
                for (TextItem item : list) {
                    float y = item.y - yOffset;
                    if (y > -50 && y < w.transform.height + 50) {
                        boolean hovering = false;

                        IFont font = item.bold ? Resources.fontBold() : Resources.font();
                        if (item instanceof LinkItem) {
                            float width = font.getTextWidth(item.text, option);
                            float minX = w.transform.width / 2 - width / 2,
                                maxX = w.transform.width / 2 + width / 2;
                            float minY = y, maxY = y + item.fontSize;

                            if (minX <= e.mx && e.mx <= maxX &&
                                minY <= e.my && e.my <= maxY) {
                                hoveringURL = ((LinkItem) item).url;
                                hovering = true;
                            }
                        }

                        option.fontSize = item.fontSize;
                        option.color = item instanceof LinkItem ?
                            (hovering ? linkColorHighlight : linkColor) :
                                textColor;
                        option.align = item.align;
                        font.draw(item.text, item.x, y, option);
                    }
                }

                _hoveringURL = hoveringURL;

                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDisable(GL11.GL_DEPTH_TEST);

                GL11.glPopMatrix();
            });

            onTabTypeChanged(TabType.Credits);
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();

            int dwheel = Mouse.getEventDWheel();
            if (dwheel != 0) {
                float progress = _dragBar.getProgress() - dwheel * 0.001f * 0.2f;
                _dragBar.setProgress(MathUtils.clamp01(progress));
            }
        }

        private void onTabTypeChanged(TabType type) {
            _tabType = type;
            setTabButtonEnable(_btnCredits, _tabType == TabType.Credits);
            setTabButtonEnable(_btnDonate, _tabType == TabType.Donate);

            _dragBar.setProgress(0);
        }

        private void setTabButtonEnable(Widget btn, boolean enable) {
            btn.getWidget("glow").getComponent(DrawTexture.class).enabled = enable;
            btn.getWidget("text").getComponent(TextBox.class).option.color = enable ? ColorTextEnable : ColorTextDisable;
            btn.getComponent(DrawTexture.class).color = enable ? ColorBtnEnable : ColorBtnDisable;
        }

    }

}
