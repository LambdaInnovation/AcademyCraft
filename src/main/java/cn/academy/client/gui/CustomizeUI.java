package cn.academy.client.gui;

import cn.academy.client.auxgui.ACHud;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.Outline;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Consumer;

@SideOnly(Side.CLIENT)
public class CustomizeUI extends CGuiScreen {

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        SettingsUI.addCallback("edit_ui", "misc", () -> {
            Minecraft.getMinecraft().displayGuiScreen(new CustomizeUI());
        }, false);

        doc = CGUIDocument.read(new ResourceLocation("academy:guis/ui_edit.xml"));
    }

    private static WidgetContainer doc;

    private Widget main;
    private Widget body;

    {
        main = doc.getWidget("main").copy();
        body = main.getWidget("body");

        ElementList list = new ElementList();
        for (ACHud.Node n : ACHud.instance.getNodes()) {
            double[] pos = n.getPosition();
            n.getPreview().pos((float) pos[0], (float) pos[1]);
            gui.addWidget(n.getPreview());
            n.getPreview().removeComponent(Outline.class);

            Widget elem = body.getWidget("template").copy();
            elem.transform.doesDraw = true;
            TextBox textBox = elem.getComponent(TextBox.class);
            textBox.localized = true;
            textBox.setContent("ac.gui.uiedit.elm." + n.getName());
            elem.listen(LeftClickEvent.class, (w, evt) -> {
                changeEditFocus(w, n);
            });

            list.addWidget(elem);
        }
        body.addComponent(list);

        gui.addWidget("main", main);
    }

    private ACHud.Node prevFocus;
    private Widget edit;

    private void changeEditFocus(Widget button, ACHud.Node node) {
        if (node == prevFocus) {
            return;
        }

        if (prevFocus != null) {
            prevFocus.getPreview().removeComponent(Outline.class);
            edit.dispose();
        }

        prevFocus = node;
        node.getPreview().addComponent(new Outline());

        edit = doc.getWidget("editbox").copy();
        double[] prevPos = node.getPosition();
        wrapEdit(edit.getWidget("edit_x"), (value_) -> {
            double[] pos = node.getPosition();
            float value = (float) (double) value_;
            node.setPosition(value, (float) pos[1]);
            node.getPreview().pos(value, ((float) pos[1]));
            node.getPreview().dirty = true;
        }, prevPos[0]);
        wrapEdit(edit.getWidget("edit_y"), (value_) -> {
            double[] pos = node.getPosition();
            float value = (float) (double) value_;
            node.setPosition((float) pos[0], value);
            node.getPreview().pos((float) pos[0], value);
            node.getPreview().dirty = true;
        }, prevPos[1]);

        edit.pos(button.x + button.transform.width*button.scale + 5,
                button.y + button.transform.height*button.scale/2 - edit.transform.height / 2);

        gui.addWidget(edit);
    }

    private void wrapEdit(Widget w, Consumer<Double> action, double defaultValue) {
        TextBox box = w.getComponent(TextBox.class);
        DrawTexture tex = w.getComponent(DrawTexture.class);
        box.content = String.valueOf(defaultValue);
        w.listen(ConfirmInputEvent.class, (w2, evt) -> {
            try {
                double x = Double.parseDouble(box.content);
                checkCoord(x);

                action.accept(x);
                tex.color = Colors.fromRGBA32(0x333333ff);
            } catch (NumberFormatException e) {
                tex.color = Colors.fromRGBA32(0xbb3333ff);
            }
        });
    }

    private void checkCoord(double val) {
        if (val < -512 || val > 512) {
            throw new NumberFormatException();
        }
    }

}