/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.ui;

import cn.academy.core.client.Resources;
import cn.academy.misc.tutorial.TutorialActivatedEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
@Registrant
public class NotifyUI extends Widget {

    @RegInitCallback
    public static void init() {
        final ResourceLocation texture = Resources.getTexture("guis/edit_preview/notify_logo");
        final INotification dummy = new INotification() {
            @Override
            public ResourceLocation getIcon() {
                return texture;
            }

            @Override
            public String getTitle() {
                return "Some Notification";
            }

            @Override
            public String getContent() {
                return "blablabla";
            }
        };
        ACHud.instance.addElement(new NotifyUI(), () -> true, "notification",
                new Widget().size(517, 170).scale(0.25).listen(FrameEvent.class, (w, e) -> {
                    drawBack(1);
                    drawIcon(dummy, end, 1);
                    drawText(dummy, 1);
                }) );
    }

    static final double KEEP_TIME = 6000;
    static final double BLEND_IN_TIME = 500, SCAN_TIME = 500, BLEND_OUT_TIME = 300;
    static final ResourceLocation texture = Resources.getTexture("guis/notification/back");
    static final Vec3
        start = VecUtils.vec(420, 42, 0),
        end = VecUtils.vec(34, 42, 0);
    
    INotification lastNotify;
    long lastReceiveTime;

    public NotifyUI() {
        addDrawing();

        transform.scale = 0.25f;
        transform.setPos(0, 15);
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    public void addDrawing() {
        listen(FrameEvent.class, (w, e) -> {
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            if(lastNotify != null) {
                long dt = GameTimer.getTime() - lastReceiveTime;
                GL11.glEnable(GL11.GL_BLEND);
                
                if(dt < BLEND_IN_TIME) {
                    drawBack(Math.min(dt / 300.0, 1));
                    
                    //Draw the icon
                    double iconAlpha = Math.max(0, Math.min(1, (dt - 200) / 300.0));
                    drawIcon(lastNotify, start, iconAlpha);
                    
                    
                } else if(dt < SCAN_TIME + BLEND_IN_TIME) { //Slide-In stage
                    
                    double scanProgress = (dt - BLEND_IN_TIME) / SCAN_TIME;
                    scanProgress = Math.sin(scanProgress * Math.PI / 2); //Use sin to simulation speed-down effect
                    
                    drawBack(1);
                    drawIcon(lastNotify, VecUtils.lerp(start, end, scanProgress), 1);
                    drawText(lastNotify, scanProgress);
                    
                } else if(dt < KEEP_TIME - BLEND_OUT_TIME) {
                    
                    drawBack(1);
                    drawIcon(lastNotify, end, 1);
                    drawText(lastNotify, 1);
                    
                } else if(dt < KEEP_TIME) { 
                    
                    double alpha = 1 - (dt - (KEEP_TIME - BLEND_OUT_TIME)) / BLEND_OUT_TIME;
                    drawBack(alpha);
                    drawIcon(lastNotify, end, alpha);
                    drawText(lastNotify, alpha);
                    
                } else {
                    //Blah, kill it
                    lastNotify = null;
                }
                
                GL11.glColor4d(1, 1, 1, 1);
            }
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        });
    }
    
    private static void drawText(INotification notif, double alpha) {
        if(alpha < 1E-1) alpha = 1E-1;
        Color color = new Color(1, 1, 1, alpha);

        FontOption optTitle = new FontOption(38, color);
        FontOption optContent = new FontOption(54, color);

        IFont font = Resources.font();
        
        font.draw(notif.getTitle(), 137, 32, optTitle);
        font.draw(notif.getContent(), 137, 81, optContent);
    }
    
    private static void drawBack(double alpha) {
        GL11.glColor4d(1, 1, 1, alpha);
        RenderUtils.loadTexture(texture);
        HudUtils.rect(517, 170);
    }
    
    private static void drawIcon(INotification notf, Vec3 p, double alpha) {
        GL11.glColor4d(1, 1, 1, alpha);
        GL11.glPushMatrix();
        RenderUtils.glTranslate(p);
        RenderUtils.loadTexture(notf.getIcon());
        HudUtils.rect(83, 83);
        GL11.glPopMatrix();
    }
    
    private void notify(INotification n) {
        lastNotify = n;
        lastReceiveTime = GameTimer.getTime();
    }
    
    @SubscribeEvent
    public void onAcquiredKnowledge(TutorialActivatedEvent evt) {
        String title = evt.tutorial.getTitle();
        notify(new INotification() {
            @Override
            public ResourceLocation getIcon() {
                return Resources.getTexture("tutorial/update_notify");
            }

            @Override
            public String getTitle() {
                return "Research note updated!";
            }

            @Override
            public String getContent() {
                return title;
            }
        });
    }
    
}
