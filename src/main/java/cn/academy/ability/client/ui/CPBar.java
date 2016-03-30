/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.ui;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.context.ContextManager;
import cn.academy.ability.api.context.IConsumptionProvider;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.academy.core.client.ui.ACHud;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.ForcePreloadTexture;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.Transform.WidthAlign;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontAlign;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.client.shader.ShaderProgram;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.vis.curve.CubicCurve;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import javax.vecmath.Vector2d;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@ForcePreloadTexture
public class CPBar extends Widget {

    public static final CPBar instance = new CPBar();

    static final double WIDTH = 964, HEIGHT = 147;
    static final double SCALE = 0.2;

    static final float CP_BALANCE_SPEED = 2.0f, O_BALANCE_SPEED = 2.0f;
    
    static double sin41 = Math.sin(Math.toRadians(44.0));
    
    static IConsumptionHintProvider chProvider;

    @RegInitCallback
    public static void init() {
        ACHud.instance.addElement(instance, () -> true, "cpbar",
                new Widget().size(WIDTH, HEIGHT)
                        .scale(SCALE)
                        .walign(WidthAlign.RIGHT)
                        .addComponent(new DrawTexture().setTex(Resources.getTexture("guis/edit_preview/cpbar"))));
    }

    /**
     * Please use IConsumptionProvider with {@link cn.academy.ability.api.context.Context} instead.
     */
    @Deprecated
    public static void setHintProvider(IConsumptionHintProvider provider) {
        chProvider = provider;
    }
    
    public static ResourceLocation
        TEX_BACK_NORMAL = tex("back_normal"),
        TEX_BACK_OVERLOAD = tex("back_overload"),
        TEX_CP = tex("cp"),
        TEX_FRONT_OVERLOAD = tex("front_overload"),
        TEX_OVERLOADED = tex("overloaded"),
        TEX_OVERLOAD_HIGHLIGHT = tex("highlight_overload"),
        TEX_MASK = tex("mask");
    
    List<ProgColor> cpColors = new ArrayList<>(), overrideColors = new ArrayList<>();

    @Deprecated
    public interface IConsumptionHintProvider {
        boolean alive();
        float getConsumption();
    }

    long presetChangeTime, lastPresetTime;
    
    boolean lastFrameActive;
    long lastDrawTime;
    long showTime;

    boolean showingNumbers;
    long lastShowValueChange;
    
    float mAlpha; //Master alpha, used for blending in.
    
    float bufferedCP;
    float bufferedOverload;

    boolean shaderLoaded = false;

    ResourceLocation overlayTexture;

    // Inteference display

    long maxtime;
    List<OffsetKeyframe> frames = new ArrayList<>();
    CubicCurve alphaCurve = new CubicCurve();

    class OffsetKeyframe {
        long time;
        Vector2d direction;
    }

    {
        final double aspect = WIDTH / HEIGHT, offsetMax = 9;
        final int iteration = 60;

        alphaCurve.addPoint(0, RandUtils.ranged(0.2, 0.8));

        int sum = 0;
        for (int i = 0; i < iteration; ++i) {
            OffsetKeyframe frame = new OffsetKeyframe();
            int thistime = RandUtils.rangei(80, 400);
            float offsetNorm = RandUtils.rangef(0, 1);
            float theta = RandUtils.rangef(0, MathUtils.PI_F * 2);
            offsetNorm = offsetNorm * offsetNorm * offsetNorm;

            sum += thistime;

            frame.time = sum;
            frame.direction = new Vector2d(
                    Math.sin(theta) * offsetNorm * offsetMax * aspect,
                    Math.cos(theta) * offsetNorm * offsetMax);
            frames.add(frame);

            alphaCurve.addPoint(sum, RandUtils.ranged(0.4, 0.7));
        }

        maxtime = sum;
    }

    OffsetKeyframe int_get() {
        long timeInput = GameTimer.getAbsTime() % maxtime;
        return frames.stream()
                .filter(f -> f.time > timeInput)
                .findFirst().get();
    }

    //


    private CPBar() {
        try { // Safety check. If loading failed, fallback to not using shader.
            this.shaderCPBar = new ShaderCPBar();
            this.shaderOverloaded = new ShaderOverloaded();
            shaderLoaded = true;
        } catch(Exception e) {
            AcademyCraft.log.error("Errow while loading CPBar shader", e);
        }
        
        transform.setSize(WIDTH, HEIGHT);
        transform.scale = SCALE;
        transform.alignWidth = WidthAlign.RIGHT;
        transform.setPos(-12, 12);
        
        initEvents();
        
        cpColors.add(new ProgColor(0.0, new Color(0xfff06767)));
        cpColors.add(new ProgColor(0.35, new Color(0xffffae44)));
        cpColors.add(new ProgColor(1.0, new Color(0xffffffff)));
        
        overrideColors.add(new ProgColor(0.0, new Color(0x0Adfdfdf)));
        overrideColors.add(new ProgColor(0.55, new Color(0x23f0d49d)));
        overrideColors.add(new ProgColor(1.0, new Color(0x50f56464)));
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void onSwitchPreset(PresetSwitchEvent event) {
        lastPresetTime = presetChangeTime;
        presetChangeTime = GameTimer.getTime();
    }

    public void startDisplayNumbers() {
        showingNumbers = true;
        lastShowValueChange = GameTimer.getTime();
    }

    public void stopDisplayNumbers() {
        showingNumbers = false;
        long time = GameTimer.getTime();
        if (time - lastShowValueChange > 400) {
            lastShowValueChange = time;
        } else lastShowValueChange = 0;
    }
    
    private void initEvents() {
        listen(FrameEvent.class, (w, e) -> 
        {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            CPData cpData = CPData.get(player);
            AbilityData aData = AbilityData.get(player);
            if (!aData.hasCategory()) return;

            Category c = aData.getCategory();
            overlayTexture = c.getOverlayIcon();
            
            boolean active = cpData.isActivated();
            
            // Calculate alpha
            long time = GameTimer.getTime();
            if(!lastFrameActive && active) {
                showTime = time;
            }

            // Takes account of interference
            long deltaTime = Math.min(100L, time - lastDrawTime);
            
            final long BLENDIN_TIME = 200L;
            mAlpha = (time - showTime < BLENDIN_TIME) ? (float) (time - showTime) / BLENDIN_TIME :
                (active ? 1.0f : Math.max(0.0f, 1 - (time - lastDrawTime) / 200.0f));

            boolean interf = cpData.isInterfering();
            boolean overloadRecovering = cpData.isOverloadRecovering();

            if (interf) {
                OffsetKeyframe frame = int_get();
                GL11.glTranslated(frame.direction.x, frame.direction.y, 0);
                long timeInput = GameTimer.getAbsTime() % maxtime;
                timeInput = (timeInput / 10) * 10; // Lower the precision to produce 'jagged' effect
                mAlpha *= alphaCurve.valueAt(timeInput);
            }

            GL11.glPushMatrix(); // PUSH 1
            
            float poverload = mAlpha > 0 ? cpData.getOverload() / cpData.getMaxOverload() : 0;
            bufferedOverload = balance(bufferedOverload, poverload, deltaTime * 1E-3f * O_BALANCE_SPEED);
            
            float pcp = mAlpha > 0 ? cpData.getCP() / cpData.getMaxCP() : 0;
            bufferedCP = balance(bufferedCP, pcp, deltaTime * 1E-3f * CP_BALANCE_SPEED);
            
            if(mAlpha > 0) {
                /* Draw CPBar */ {
                    if(bufferedOverload < 1) {
                        drawNormal(bufferedOverload);
                    } else {
                        drawOverload(bufferedOverload);
                    }
                    
                    if(chProvider != null && !chProvider.alive())
                        chProvider = null;

                    float estmCons = getConsumptionHint();
                    boolean low = interf || overloadRecovering;

                    if(estmCons != 0) {
                        float ncp = Math.max(0, cpData.getCP() - estmCons);
                        
                        float oldAlpha = mAlpha;
                        mAlpha *= 0.2f + 0.1f * (1 + Math.sin(time / 80.0f));
                        
                        drawCPBar(pcp, low);
                        
                        mAlpha = oldAlpha;
                        
                        drawCPBar(ncp / cpData.getMaxCP(), low);
                    } else {
                        drawCPBar(bufferedCP, low);
                    }
                }
                
                /* Draw Preset Hint */ {
                    final long preset_wait = 2000L;
                    if(time - presetChangeTime < preset_wait)
                        drawPresetHint((double)(time - presetChangeTime) / preset_wait,
                            time - lastPresetTime);
                }

                // Draw data
                {
                    float alpha;
                    long dt = lastShowValueChange == 0 ? Long.MAX_VALUE : time - lastShowValueChange;

                    if (cpData.isOverloaded()) {
                       alpha = 0.0f;
                    } else if (showingNumbers) {
                        alpha = MathUtils.clampf(0, 1, (dt - 200) / 400f); // Delay display by 200ms for visual pleasure
                    } else if(dt < 300f) {
                        alpha = 1 - dt / 300f;
                    } else {
                        alpha = 0.0f;
                    }

                    if (alpha > 0) {
                        final double x0 = 110;

                        IFont font = Resources.font();
                        FontOption option = new FontOption(40);
                        option.color.a = 0.6f * mAlpha * alpha;

                        String str10 = "CP ";
                        String str11 = String.format("%.0f", cpData.getCP());
                        String str12 = String.format("/%.0f", cpData.getMaxCP());

                        String str20 = "OL ";
                        String str21 = String.format("%.0f", cpData.getOverload());
                        String str22 = String.format("/%.0f", cpData.getMaxOverload());

                        double len10 = font.getTextWidth(str10, option),
                                len11 = font.getTextWidth(str11, option),
                                len20 = font.getTextWidth(str20, option),
                                len21 = font.getTextWidth(str21, option);

                        double len0 = Math.max(len10, len20);
                        double len1 = len0 + Math.max(len11, len21);

                        font.draw(str10, x0, 55, option);
                        font.draw(str12, x0+len1, 55, option);
                        font.draw(str20, x0, 85, option);
                        font.draw(str22, x0+len1, 85, option);

                        option.align = FontAlign.RIGHT;
                        font.draw(str11, x0+len1, 55, option);
                        font.draw(str21, x0+len1, 85, option);
                    }
                }
                
                drawActivateKeyHint();
            }
            
            if(active) {
                lastDrawTime = time;
            }

            lastFrameActive = active;
            
            GL11.glColor4d(1, 1, 1, 1);
            GL11.glPopMatrix(); // Pop 1
        });
    }
    
    private void drawOverload(float overload) {
        //Draw plain background
        color4d(1, 1, 1, 0.8);
        RenderUtils.loadTexture(TEX_BACK_OVERLOAD);
        HudUtils.rect(WIDTH, HEIGHT);
        
        // Draw back
        color4d(1, 1, 1, 1);
        
        if(shaderLoaded) {
            shaderOverloaded.useProgram();
            shaderOverloaded.updateTexOffset((GameTimer.getTime() % 10000L) / 10000.0f);
        }
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        RenderUtils.loadTexture(TEX_MASK);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        RenderUtils.loadTexture(TEX_FRONT_OVERLOAD);
        
        final double x0 = 30, width2 = WIDTH - x0 - 20;
        HudUtils.rect(x0, 0, 0, 0, width2, HEIGHT, width2, HEIGHT);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        
        GL20.glUseProgram(0);
        
        // Highlight
        color4d(1, 1, 1, 0.3 + 0.35 * (Math.sin(GameTimer.getTime() / 200.0) + 1));
        RenderUtils.loadTexture(TEX_OVERLOAD_HIGHLIGHT);
        HudUtils.rect(WIDTH, HEIGHT);
    }
    
    private void drawNormal(float overload) {
        RenderUtils.loadTexture(TEX_BACK_NORMAL);
        
        color4d(1, 1, 1, .8);
        HudUtils.rect(WIDTH, HEIGHT);
        
        //Overload progress
        final double X0 = 0, Y0 = 21, WIDTH = 943, HEIGHT = 104;
        
        autoLerp(overrideColors, overload);
        double len = overload * WIDTH;
        
        RenderUtils.loadTexture(TEX_MASK);
        subHud(X0 + WIDTH - len, Y0, len, HEIGHT);
    }

    private float getConsumptionHint() {
        Optional<IConsumptionProvider> provider =
                ContextManager.instance.findLocal(IConsumptionProvider.class);

        if (provider.isPresent()) return provider.get().getConsumptionHint();
        if (chProvider != null) return chProvider.getConsumption(); // Legacy fallback

        return 0;
    }
    
    private void drawCPBar(float prog, boolean cantuse) {
        float pre_mAlpha = mAlpha;
        if (cantuse) {
            mAlpha *= 0.3f;
        }

        //We need a cut-angle effect so this must be done manually
        autoLerp(cpColors, prog);
        
        prog = 0.16f + prog * 0.8f;
        
        final double OFF = 103 * sin41, X0 = 47, Y0 = 30, WIDTH = 883, HEIGHT = 84;
        Tessellator t = Tessellator.instance;
        double len = WIDTH * prog, len2 = len - OFF;
        
        if(shaderLoaded) {
            shaderCPBar.useProgram();
        }
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        RenderUtils.loadTexture(overlayTexture);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        RenderUtils.loadTexture(TEX_CP);
        
        t.startDrawingQuads();
        addVertex(X0 + (WIDTH - len), Y0);
        addVertex(X0 + (WIDTH - len2), Y0 + HEIGHT);
        addVertex(X0 + WIDTH, Y0 + HEIGHT);
        addVertex(X0 + WIDTH, Y0);
        t.draw();
        
        GL20.glUseProgram(0);
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        mAlpha = pre_mAlpha;
    }
    
    final Color CRL_P_BACK = new Color().setColor4i(48, 48, 48, 160),
            CRL_P_FORE = new Color().setColor4i(255, 255, 255, 200);
    final Color temp = new Color();

    FontOption fo_PresetHint = new FontOption(46, FontAlign.CENTER);

    private void drawPresetHint(double progress, long untilLast) {
        final double x0 = 580, y0 = 136;
        final double size = 52, step = size + 10;
        
        double x = x0, y = y0;
        
        int cur = PresetData.get(
            Minecraft.getMinecraft().thePlayer).getCurrentID();
        
        double alpha;
        if(untilLast > 3000 && progress < 0.2) {
            alpha = progress / 0.2;
        } else if(progress > 0.8) {
            alpha = (1 - progress) / 0.2;
        } else {
            alpha = 1;
        }
        alpha *= 0.75;
        
        for(int i = 0; i < 4; ++i) {
            CRL_P_BACK.a = alpha;
            CRL_P_BACK.bind();
            HudUtils.colorRect(x, y, size, size);
            
            temp.a = Math.max(0.05, alpha * 0.8);

            fo_PresetHint.color = temp;
            Resources.fontBold().draw(String.valueOf(i + 1), x + size / 2, y + 5, fo_PresetHint);
            
            temp.bind();
            if(i == cur) {
                ACRenderingHelper.drawGlow(x, y, size, size, 5, CRL_P_FORE);
            }
            
            x += step;
        }
        
    }
    
    static final Color 
        CRL_KH_BACK = new Color().setColor4i(65, 65, 65, 70), 
        CRL_KH_GLOW = new Color().setColor4i(255, 255, 255, 40);

    FontOption fo_ActivateHint = new FontOption(44, FontAlign.RIGHT, new Color(0xa0ffffff));

    private void drawActivateKeyHint() {
        Optional<String> hint = ClientRuntime.instance().getActivateHandler().getHintTranslated();
        
        if(hint.isPresent()) {
            String str = hint.get();

            final double x0 = 500, y0 = 140, MARGIN = 8;
            CRL_KH_BACK.bind();

            IFont font = Resources.font();
            double len = font.getTextWidth(str, fo_ActivateHint);
            HudUtils.colorRect(x0 - MARGIN - len, y0 - MARGIN, len + MARGIN * 2, 44 + MARGIN * 2);
            ACRenderingHelper.drawGlow(x0 - MARGIN - len, y0 - MARGIN, len + MARGIN * 2, 44 + MARGIN * 2, 5, CRL_KH_GLOW);
            font.draw(str, x0, y0, fo_ActivateHint);
        }
    }
    
    private void color4d(double r, double g, double b, double a) {
        GL11.glColor4d(r, g, b, mAlpha * a);
    }
    
    private void subHud(double x, double y, double width, double height) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        addVertex(x,          y);
        addVertex(x,          y + height);
        addVertex(x + width, y + height);
        addVertex(x + width, y);
        t.draw();
    }
    
    private void addVertex(double x, double y) {
        double width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH),
                height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        Tessellator.instance.addVertexWithUV(x, y, -90, x / width, y / height);
    }
    
    private void lerpBindColor(Color a, Color b, double factor) {
        color4d(lerp(a.r, b.r, factor), lerp(a.g, b.g, factor), lerp(a.b, b.b, factor), lerp(a.a, b.a, factor));
    }
    
    private void autoLerp(List<ProgColor> list, double prog) {
        for(int i = 0; i < list.size(); ++i) {
            ProgColor cur = list.get(i);
            if(cur.prog >= prog) {
                if(i == 0) {
                    list.get(i).color.bind();
                } else {
                    ProgColor last = list.get(i - 1);
                    lerpBindColor(last.color, cur.color, (prog - last.prog) / (cur.prog - last.prog));
                }
                return;
            }
        }
        throw new RuntimeException("bad progress: " + prog); //Should never reach here
    }
    
    private double lerp(double a, double b, double factor) {
        return a * (1 - factor) + b * factor;
    }
    
    private float balance(float from, float to, float max) {
        float delta = to - from;
        delta = Math.signum(delta) * Math.min(max, Math.abs(delta));
        
        return from + delta;
    }
    
    private static ResourceLocation tex(String name) {
        return new ResourceLocation("academy:textures/guis/cpbar/" + name + ".png");
    }
    
    private static class ProgColor {
        double prog;
        Color color;
        
        public ProgColor(double _p, Color _c) {
            prog = _p;
            color = _c;
        }
    }
    
    private static class ShaderOverloaded extends ShaderProgram {
        
        final int locTexOffset;
        
        private ShaderOverloaded() {
            this.linkShader(new ResourceLocation("lambdalib:shaders/simple.vert"), GL20.GL_VERTEX_SHADER);
            this.linkShader(new ResourceLocation("academy:shaders/cpbar_overload.frag"), GL20.GL_FRAGMENT_SHADER);
            this.compile();
            
            useProgram();
            GL20.glUniform1i(getUniformLocation("samplerTex"), 0);
            GL20.glUniform1i(getUniformLocation("samplerMask"), 4);
            GL20.glUseProgram(0);
            
            locTexOffset = getUniformLocation("texOffset");
        }
        
        public void updateTexOffset(float val) {
            GL20.glUniform1f(locTexOffset, val);
        }
        
    }
    
    private static class ShaderCPBar extends ShaderProgram {
        
        private ShaderCPBar() {
            this.linkShader(new ResourceLocation("lambdalib:shaders/simple.vert"), GL20.GL_VERTEX_SHADER);
            this.linkShader(new ResourceLocation("academy:shaders/cpbar_cp.frag"), GL20.GL_FRAGMENT_SHADER);
            this.compile();
            
            useProgram();
            GL20.glUniform1i(getUniformLocation("samplerTex"), 0);
            GL20.glUniform1i(getUniformLocation("samplerIcon"), 4);
            GL20.glUseProgram(0);
        }
        
    }
    
    ShaderCPBar shaderCPBar;
    ShaderOverloaded shaderOverloaded;
    
}
