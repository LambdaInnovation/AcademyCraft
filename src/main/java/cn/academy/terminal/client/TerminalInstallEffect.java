/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.client;

import cn.academy.core.ModuleCoreClient;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.cgui.gui.CGui;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.IGuiEventHandler;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.auxgui.AuxGui;
import cn.lambdalib.util.key.KeyManager;
import cn.lambdalib.util.mc.PlayerUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
public class TerminalInstallEffect extends AuxGui {
    
    private static final long ANIM_LENGTH = 4000L;
    private static final long WAIT = 700L;
    private static final long BLEND_IN = 200L, BLEND_OUT = 200L;

    private static WidgetContainer loaded;
    @RegInitCallback
    private static void __init() {
        loaded = CGUIDocument.panicRead(new ResourceLocation("academy:guis/terminal_installing.xml"));
    }
    
    CGui gui = new CGui();
    
    public TerminalInstallEffect() {
        gui.addWidget("main", loaded.getWidget("main").copy());
        gui.getWidget("main/progbar").listen(FrameEvent.class, (w, e) -> {
            double prog = (double) this.getTimeActive() / ANIM_LENGTH;
            if(this.getTimeActive() >= ANIM_LENGTH + WAIT) {
                dispose();
                TerminalUI.keyHandler.onKeyUp();
                PlayerUtils.sendChat(Minecraft.getMinecraft().thePlayer, "ac.terminal.key_hint",
                        KeyManager.getKeyName(ModuleCoreClient.keyManager.getKeyID(TerminalUI.keyHandler)));
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
    public boolean isForeground() {
        return false;
    }

    @Override
    public void draw(ScaledResolution sr) {
        gui.resize(sr.getScaledWidth_double(), sr.getScaledHeight_double());
        gui.draw();
    }
    
    private void initBlender(Widget w) {
        w.listen(FrameEvent.class, new IGuiEventHandler<FrameEvent>() {
            DrawTexture tex = DrawTexture.get(w);
            TextBox text = TextBox.get(w);
            ProgressBar bar = ProgressBar.get(w);
            
            double texA, textA, barA;
            {
                if(tex != null) texA = tex.color.a;
                if(text != null) textA = text.option.color.a;
                if(bar != null) barA = bar.color.a;
            }

            @Override
            public void handleEvent(Widget w, FrameEvent event) {
                double alpha;
                long dt = getTimeActive();
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
                if(tex != null) tex.color.a = texA * alpha;
                if(text != null) text.option.color.a = 0.1 + 0.9 * textA * alpha;
                if(bar != null) bar.color.a = barA * alpha;
            }
        });
    }

}
