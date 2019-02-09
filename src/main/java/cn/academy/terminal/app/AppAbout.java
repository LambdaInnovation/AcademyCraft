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
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.Debug;
import net.minecraft.client.Minecraft;
import org.lwjgl.util.Color;

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
            public void onStart() {
                Minecraft.getMinecraft().displayGuiScreen(new AboutUI());
            }
        };
    }

    static class AboutUI extends CGuiScreen {

        private static final WidgetContainer Prefab = CGUIDocument.read(Resources.getGui("about"));

        private static final Color
            ColorTextDisable = Colors.white(),
            ColorTextEnable = Colors.fromRGB32(0x3d3f4b),
            ColorBtnDisable = Colors.whiteBlend(0.2f),
            ColorBtnEnable = Colors.whiteBlend(0.5f);

        enum TabType { Credits, Donate }

        Widget _btnCredits, _btnDonate;

        TabType _tabType = TabType.Credits;

        DragBar _dragBar;

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

            onTabTypeChanged(TabType.Credits);
        }

        private void onTabTypeChanged(TabType type) {
            _tabType = type;
            setTabButtonEnable(_btnCredits, _tabType == TabType.Credits);
            setTabButtonEnable(_btnDonate, _tabType == TabType.Donate);
        }

        private void setTabButtonEnable(Widget btn, boolean enable) {
            btn.getWidget("glow").getComponent(DrawTexture.class).enabled = enable;
            btn.getWidget("text").getComponent(TextBox.class).option.color = enable ? ColorTextEnable : ColorTextDisable;
            btn.getComponent(DrawTexture.class).color = enable ? ColorBtnEnable : ColorBtnDisable;
        }

    }

}
