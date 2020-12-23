package cn.academy.client.auxgui;

import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.ClientRuntime.DelegateNode;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.DelegateState;
import cn.academy.datapart.CooldownData;
import cn.academy.datapart.CooldownData.SkillCooldown;
import cn.academy.datapart.CPData;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.Resources;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.render.legacy.ShaderMono;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.input.KeyManager;
import com.google.common.collect.Multimap;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class KeyHintUI extends Widget {

    static final float SCALE = 0.23f;

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        Widget child = new Widget()
                .size(128, 193)
                .addComponent(new DrawTexture()
                        .setTex(Resources.getTexture("guis/edit_preview/key_hint")));
        Widget display = new Widget()
                .size(140, 210)
                .scale(SCALE * 2)
                .walign(WidthAlign.RIGHT)
                .halign(HeightAlign.CENTER);
        display.addWidget(child);

        ACHud.instance.addElement(new KeyHintUI(), 
            () -> CPData.get(Minecraft.getMinecraft().player).isActivated(), "keyhint", display);
    }
    
    ResourceLocation 
        TEX_BACK = tex("back"),
        TEX_ICON_BACK = tex("icon_back"),
        TEX_KEY_LONG = tex("key_long"),
        TEX_KEY_SHORT = tex("key_short"),
        TEX_MOUSE_L = tex("mouse_left"), 
        TEX_MOUSE_R = tex("mouse_right"), 
        TEX_MOUSE_GENERIC = tex("mouse_generic");
    
    double lastFrameTime, showTime;
    double mAlpha;
    float sinAlpha; // Used when rendering active delegates
    boolean canUseAbility;

    final FontOption option = new FontOption(32, FontAlign.CENTER, Colors.fromHexColor(0xff194246));
    
    private KeyHintUI() {
        walign(WidthAlign.RIGHT);
        halign(HeightAlign.CENTER);
        size(140, 210);
        pos(0, 30);
        scale(SCALE);
        
        addDrawing();
    }
    
    private void addDrawing() {
        listen(FrameEvent.class, (w, e) -> {
            EntityPlayer player = Minecraft.getMinecraft().player;

            CPData cpData = CPData.get(player);
            
            canUseAbility = cpData.canUseAbility();
            
            double time = GameTimer.getTime();
            if(time - lastFrameTime > 0.3) {
                showTime = time;
            }
            
            if((time - showTime) < 0.3) {
                mAlpha = (time - showTime) / 0.3;
            } else {
                mAlpha = 1.0;
            }

            sinAlpha = 0.6f + (1 + MathHelper.sin(((float) time % 100) / 50.0f)) * 0.2f;

            if(cpData.isActivated()) {
                ClientRuntime rt = ClientRuntime.instance();
                CooldownData cd = CooldownData.of(rt.getEntity());

                Multimap<String, DelegateNode> map = rt.getDelegateRawData();
                List<String> groups = new ArrayList<>(map.keySet());
                groups.sort((s1, s2) -> {
                    if (s1.equals(ClientRuntime.DEFAULT_GROUP))      return -1;
                    else if (s2.equals(ClientRuntime.DEFAULT_GROUP)) return 1;
                    else return s1.compareTo(s2);
                });

                int availIdx = 0;
                for (int i = 0; i < groups.size(); ++i) {
                    Collection<DelegateNode> nodes = map.get(groups.get(i));
                    if (!nodes.isEmpty()) {
                        final double x = -200 - availIdx * 200;
                        double y = 0;
                        for (DelegateNode node : nodes) {
                            GL11.glPushMatrix();
                            GL11.glTranslated(x, y, 0);
                            drawSingle(node.keyID, node.delegate, cd.getSub(node.delegate.getSkill(), node.delegate.getIdentifier()));
                            GL11.glPopMatrix();
                            y += 92;
                        }
                        availIdx++;
                    }
                }
            }
            
            lastFrameTime = time;
            GL11.glColor4d(1, 1, 1, 1);
        });
    }
    
    private void drawSingle(int keyCode, KeyDelegate c, SkillCooldown data) {
        ResourceLocation icon = c.getIcon();
        
        // Back
        RenderUtils.loadTexture(TEX_BACK);
        color4d(1, 1, 1, 1);
        HudUtils.rect(122, 0, 185, 83);

        IFont font = Resources.font();
        
        // KeyHint
        {
            float wx = 180, wy = 27;
            if(!canUseAbility || data.getTickLeft() > 0) {
                color4d(0.7, 0.7, 0.7, 1);
                ShaderMono.instance().useProgram();
            }
            
            if(keyCode >= 0) {
                String name = Keyboard.getKeyName(keyCode);
                if(name.length() <= 2) { //Short preference
                    drawBack(TEX_KEY_SHORT);
                } else {
                    drawBack(TEX_KEY_LONG);
                }

                font.draw(name, wx, wy, option);
            } else {
                
                if(keyCode == KeyManager.MOUSE_LEFT) {
                    drawBack(TEX_MOUSE_L);
                } else if(keyCode == KeyManager.MOUSE_RIGHT) {
                    drawBack(TEX_MOUSE_R);
                } else {
                    drawBack(TEX_MOUSE_GENERIC);
                    font.draw("" + (keyCode + 100), wx, wy, option);
                }
                
            }
            
            color4d(1, 1, 1, 1);
        }
        
        // Logo
        color4d(1, 1, 1, 1);
        RenderUtils.loadTexture(TEX_ICON_BACK);
        HudUtils.rect(216, 5, 72, 72);


        DelegateState state = c.getState();
        float prog = (float) data.getTickLeft() / data.getMaxTick();

        float thisSinAlpha = (state.sinEffect ? sinAlpha : 1);

        float alpha;
        if (prog == 0.0f) {
            alpha = state.alpha * (0.4f + thisSinAlpha * 0.6f);
        } else {
            alpha = 0.4f;
        }
        
        final double ICON_SIZE = 62;
        color4d(1, 1, 1, alpha);
        RenderUtils.loadTexture(icon);
        HudUtils.rect(221, 10, ICON_SIZE, ICON_SIZE);

        int prevA = state.glowColor.getAlpha();
        state.glowColor.setAlpha((int) (state.glowColor.getAlpha() * thisSinAlpha));
        ACRenderingHelper.drawGlow(221, 10, ICON_SIZE, ICON_SIZE, 5, state.glowColor);
        state.glowColor.setAlpha(prevA);
        
        GL20.glUseProgram(0);
        
        if(prog != 0) {
            color4d(0.6, 0.6, 0.6, .3);
            HudUtils.colorRect(221, 10 + ICON_SIZE * (1 - prog), ICON_SIZE, ICON_SIZE * prog);
        }
    }
    
    private void drawBack(ResourceLocation tex) {
        RenderUtils.loadTexture(tex);
        color4d(1, 1, 1, 1);
        HudUtils.rect(146, 10, 70, 70);
    }
    
    private void color4d(double r, double g, double b, double a) {
        GL11.glColor4d(r, g, b, a * mAlpha);
    }
    
    private static ResourceLocation tex(String name) {
        return Resources.getTexture("guis/key_hint/" + name);
    }
    
}