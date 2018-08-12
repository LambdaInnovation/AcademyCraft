package cn.academy.client.gui;

import cn.academy.client.auxgui.ACHud;
import cn.academy.Resources;
import cn.academy.event.TutorialActivatedEvent;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class NotifyUI extends Widget {

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
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
                new Widget().size(517, 170).scale(0.25f).listen(FrameEvent.class, (w, e) -> {
                    drawBack(1);
                    drawIcon(dummy, end, 1);
                    drawText(dummy, 1);
                }) );
    }

    static final double KEEP_TIME = 6;
    static final double BLEND_IN_TIME = 0.5, SCAN_TIME = 0.5, BLEND_OUT_TIME = 0.3;
    static final ResourceLocation texture = Resources.getTexture("guis/notification/back");
    static final Vec3d
        start = new Vec3d(420, 42, 0),
        end = new Vec3d(34, 42, 0);
    
    INotification lastNotify;
    double lastReceiveTime;

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
                double dt = GameTimer.getTime() - lastReceiveTime;
                GL11.glEnable(GL11.GL_BLEND);
                
                if(dt < BLEND_IN_TIME) {
                    drawBack(Math.min(dt / 300.0, 1));
                    
                    //Draw the icon
                    double iconAlpha = Math.max(0, Math.min(1, (dt - 200) / 300.0));
                    drawIcon(lastNotify, start, iconAlpha);
                    
                    
                } else if(dt < SCAN_TIME + BLEND_IN_TIME) { //Slide-In stage
                    
                    float scanProgress = (float) ((dt - BLEND_IN_TIME) / SCAN_TIME);
                    scanProgress = MathHelper.sin(scanProgress * MathUtils.PI_F / 2); //Use sin to simulation speed-down effect
                    
                    drawBack(1);
                    drawIcon(lastNotify, VecUtils.lerp(start, end, scanProgress), 1);
                    drawText(lastNotify, scanProgress);
                    
                } else if(dt < KEEP_TIME - BLEND_OUT_TIME) {
                    
                    drawBack(1);
                    drawIcon(lastNotify, end, 1);
                    drawText(lastNotify, 1);
                    
                } else if(dt < KEEP_TIME) { 
                    
                    float alpha = 1 - (float) ((dt - (KEEP_TIME - BLEND_OUT_TIME)) / BLEND_OUT_TIME);
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
    
    private static void drawText(INotification notif, float alpha) {
        if(alpha < 1E-1f) alpha = 1E-1f;
        Color color = Colors.fromFloat(1, 1, 1, alpha);

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
    
    private static void drawIcon(INotification notf, Vec3d p, double alpha) {
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
                return I18n.translateToLocal("ac.tutorial.update");
            }

            @Override
            public String getContent() {
                return title;
            }
        });
    }
    
}