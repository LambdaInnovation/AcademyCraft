package cn.academy.terminal.client;

import cn.academy.core.ACKeyManager;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.IGuiEventHandler;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.PlayerUtils;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class TerminalInstallEffect extends AuxGui {
    
    private static final long ANIM_LENGTH = 4000L;
    private static final long WAIT = 700L;
    private static final long BLEND_IN = 200L, BLEND_OUT = 200L;

    private static WidgetContainer loaded;
    @StateEventCallback
    private static void __init(FMLInitializationEvent ev) {
        loaded = CGUIDocument.read(new ResourceLocation("academy:guis/terminal_installing.xml"));
    }
    
    CGui gui = new CGui();
    
    public TerminalInstallEffect() {
        gui.addWidget("main", loaded.getWidget("main").copy());
        gui.getWidget("main/progbar").listen(FrameEvent.class, (w, e) -> {
            double prog = (double) this.getTimeActive() / ANIM_LENGTH;
            if(this.getTimeActive() >= ANIM_LENGTH + WAIT) {
                dispose();
                TerminalUI.keyHandler.onKeyUp();
                PlayerUtils.sendChat(Minecraft.getMinecraft().player, "ac.terminal.key_hint",
                        KeyManager.getKeyName(ACKeyManager.instance.getKeyID(TerminalUI.keyHandler)));
            }

            if(prog > 1.0) {
                prog = 1.0;
            }
            ProgressBar.get(w).progress = prog;
        });
        
        Widget main = gui.getWidget("main");
        initBlender(main);
        for(Widget w : main.getDrawList())
            initBlender(w);
    }

    @Override
    public void draw(ScaledResolution sr) {
        gui.resize((float) sr.getScaledWidth_double(), (float) sr.getScaledHeight_double());
        gui.draw();
    }

    private void initBlender(Widget w) {
        w.listen(FrameEvent.class, new IGuiEventHandler<FrameEvent>() {
            DrawTexture tex = DrawTexture.get(w);
            TextBox text = TextBox.get(w);
            ProgressBar bar = ProgressBar.get(w);
            
            int texA, textA, barA;
            {
                if(tex != null) texA = tex.color.getAlpha();
                if(text != null) textA = text.option.color.getAlpha();
                if(bar != null) barA = bar.color.getAlpha();
            }

            @Override
            public void handleEvent(Widget w, FrameEvent event) {
                double alpha;
                long dt = (long) (getTimeActive() * 1000);
                if(dt < BLEND_IN) {
                    alpha = (double) (dt) / BLEND_IN;
                } else if(dt > ANIM_LENGTH) {
                    alpha = Math.max(0, 1 - (double) (dt - ANIM_LENGTH) / BLEND_OUT);
                } else {
                    alpha = 1;
                }
                
                DrawTexture tex = DrawTexture.get(w);
                TextBox text = TextBox.get(w);
                ProgressBar bar = ProgressBar.get(w);
                if(tex != null) tex.color.setAlpha((int)(texA * alpha));
                if(text != null) text.option.color.setAlpha((int)(Colors.f2i(0.1f) + 0.9 * textA * alpha));
                if(bar != null) bar.color.setAlpha((int)(barA * alpha));
            }
        });
    }

}