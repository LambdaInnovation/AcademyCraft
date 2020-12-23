package cn.academy.client.auxgui;

import cn.academy.ability.Controllable;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.PresetData;
import cn.academy.datapart.PresetData.Preset;
import cn.academy.Resources;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.IGuiEventHandler;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.*;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
//import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class PresetEditUI extends GuiScreen {
    
    static final Color
        CRL_BACK = new Color(49, 49, 49, 200),
        CRL_WHITE = Colors.fromFloat(1, 1, 1, 0.6f),
        CRL_GLOW = Colors.fromFloat(1, 1, 1, 0.2f);
    
    static WidgetContainer loaded;
    static Widget template;
    
    static final float STEP = 125;
    static final double TRANSIT_TIME = 0.35;
    static final int MAX_ALPHA = Colors.f2i(1f), MIN_ALPHA = Colors.f2i(0.3f);
    static final float MAX_SCALE = 1, MIN_SCALE = 0.8f;
    
    static class SelectionProvider {
        public final int id;
        public final ResourceLocation texture;
        public final String hint;
        
        public SelectionProvider(int _id, ResourceLocation _texture, String _hint) {
            id = _id;
            texture = _texture;
            hint = _hint;
        }
    }
    
    /**
     * Drawer when nothing happened
     */
    CGui foreground = new CGui();
    
    /**
     * Drawer of transition
     */
    CGui transitor = new CGui();

    @StateEventCallback
    private static void __init(FMLInitializationEvent ev) {
        loaded = CGUIDocument.read(new ResourceLocation("academy:guis/preset_edit.xml"));
        template = loaded.getWidget("template");
    }
    
    final EntityPlayer player;
    final PresetData data;
    final AbilityData aData;

    final IFont font = Resources.font();

    // lastActive is the preset ID before the transition.
    int lastActive, active;
    
    boolean transiting;
    double transitStartTime;
    double deltaTime; //Time that has passed since last transition
    double transitProgress;
    
    Widget selector;
    
    public PresetEditUI() {
        player = Minecraft.getMinecraft().player;
        data = PresetData.get(player);
        aData = AbilityData.get(player);
        
        init();
    }
    
    @Override
    public void onGuiClosed() { }

    @SideOnly(Side.CLIENT)
    private String local(String key) {
        return I18n.format("ac.gui.preset_edit." + key);
    }
    
    private void init() {
        foreground.addWidget(loaded.getWidget("background").copy());
        transitor.addWidget(loaded.getWidget("background").copy());
        
        // Build the pages
        for(int i = 0; i < 4; ++i) {
            Widget normal = createCopy();
            TextBox.get(normal.getWidget("title")).setContent(local("tag") + (i + 1));
            
            for(int j = 0; j < 4; ++j) {
                Widget ww = normal.getWidget(String.valueOf(j));
                normal.getWidget(String.valueOf(j)).addComponent(new HintHandler(j));
            }
            normal.addComponent(new ForegroundPage(i));
            add(i, foreground, normal);
        }
        
        for(int i = 0; i < 4; ++i) {
            Widget back = createCopy();
            TextBox.get(back.getWidget("title")).setContent(local("tag") + (i + 1));
            
            back.addComponent(new TransitPage(i));
            add(i, transitor, back);
        }
        
        resetAll();
    }
    
    private void resetAll() {
        updateInfo(foreground);
        updateInfo(transitor);
        
        updatePosForeground();
    }

    private Widget createCopy() {
        Widget ret = template.copy();
        for(Widget w : ret.getDrawList()) {
            for(Widget w2 : w.getDrawList())
                w2.listen(FrameEvent.class, 1, new AlphaAssign());
        }
        return ret;
    }
    
    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if(button == 0) {
            if(!transiting) {
                foreground.mouseClicked(mx, my, button);
            }
        }
    }
    
    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        GL11.glEnable(GL11.GL_BLEND);
        RenderUtils.drawBlackout();

        if(transiting) {
            updateTransit();
            transitor.resize(width, height);
            transitor.draw(-1, -1);
        } else {
            updatePosForeground();
            foreground.resize(width, height);
            foreground.draw(mx, my);
        }
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    private float getXFor(int i, int active) {
        if(i == active) {
            return 0;
        }
        return STEP * (i - active);
    }
    
    private float getXFor(int i) {
        return getXFor(i, active);
    }
    
    private void add(int i, CGui gui, Widget w) {
        gui.addWidget("" + i, w);
    }
    
    private Widget get(CGui gui, int i) {
        return gui.getWidget("" + i);
    }
    
    // Major control
    private void startTransit(int to) {
        updateInfo(transitor);
        
        lastActive = active;
        active = to;
        transiting = true;
        transitStartTime = GameTimer.getAbsTime();
    }
    
    private void finishTransit() {
        updatePosForeground();
    }
    
    // Foreground page
    private void onEdit(int keyID, Controllable controllable) {
        Preset last = data.getPreset(active);
        Controllable[] arr = last.copyData();
        arr[keyID] = controllable;

        Preset newPreset = new Preset(arr);
        data.setPresetFromClient(active, newPreset);
        getPage(get(foreground, active)).updateInfo(newPreset);
    }
    
    // Transition page
    private void updateTransit() {
        deltaTime = GameTimer.getAbsTime() - transitStartTime;
        
        transitProgress = deltaTime / TRANSIT_TIME;
        if(transitProgress > 1) {
            transitProgress = 1;
        }
        
        for(int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            Widget page = get(transitor, i);
            getPage(page).updatePosition();
        }
        
        if(transitProgress == 1) {
            transiting = false;
            finishTransit();
        }
    }

    // Utils
    private void updateInfo(CGui gui) {
        for(int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            Widget page = get(gui, i);
            getPage(page).updateInfo(data.getPreset(i));
        }
    }
    
    private void updatePosForeground() {
        for(int i = 0; i < 4; ++i) {
            Widget page = get(foreground, i);
            getPage(page).updatePosition();
        }
    }
    
    private abstract class Page extends Component {
        
        /**
         * Master alpha visited by all sub widgets
         */
        protected int alpha;
        
        final int id;
        
        public Page(int _id) {
            super("Page");
            id = _id;
        }
        
        public void updateInfo(Preset preset) {
            for(int i = 0; i < PresetData.MAX_PRESETS; ++i) {
                Controllable c = preset.getControllable(i);
                Widget main = widget.getWidget("" + i);
                DrawTexture.get(main.getWidget("icon")).texture = c == null ? Resources.TEX_EMPTY : c.getHintIcon();
                TextBox.get(main.getWidget("text")).content = c == null ? "" : c.getHintText();
            }
        }
        
        public void updatePosition() {
            widget.transform.x = getXFor(id);
            widget.dirty = true;
            
            alpha = id == active ? MAX_ALPHA : MIN_ALPHA;
            widget.transform.scale = id == active ? MAX_SCALE : MIN_SCALE;
            DrawTexture.get(widget).color.setAlpha(alpha);
        }
        
        
    }
    
    static Page getPage(Widget w) {
        return w.getComponent(Page.class);
    }
    
    private class HintHandler extends Component {

        final int keyid;
        
        public HintHandler(int _keyid) {
            super("Hint");
            keyid = _keyid;
            
            listen(FrameEvent.class, (w, event) -> 
            {
                Page page = getPage(w.getWidgetParent());
                DrawTexture dt = DrawTexture.get(w);
                dt.enabled = page.id == active && event.hovering;
                dt.color.setAlpha(page.alpha);
            });
            
            listen(LeftClickEvent.class, (w, e) -> {
                Page page = getPage(w.getWidgetParent());
                if(selector != null && !selector.disposed) {
                    selector.dispose();
                    selector = null;
                } else if(page.id == active) {
                    // Open the selector
                    selector = new Selector(keyid);
                    selector.transform.setPos(foreground.getMouseX(), foreground.getMouseY());
                    foreground.addWidget(selector);
                } else {
                    startTransit(page.id);
                }
            });
        }
        
    }
    
    private class ForegroundPage extends Page {
        
        public ForegroundPage(int _id) {
            super(_id);
        }
        
    }
    
    private class TransitPage extends Page {
        
        public TransitPage(int _id) {
            super(_id);
            
            listen(FrameEvent.class, (w, e) -> 
            {
                DrawTexture.get(w).color.setAlpha(alpha);
            });
        }
        
        @Override
        public void updatePosition() {
            double x0 = getXFor(id, lastActive), x1 = getXFor(id, active);
            float dx = (float) MathUtils.lerp(x0, x1, transitProgress);
            float scale;
            
            if(isFrom()) {
                alpha = (int)MathUtils.lerp(MAX_ALPHA, MIN_ALPHA, transitProgress);
                scale = (float)MathUtils.lerp(MAX_SCALE, MIN_SCALE, transitProgress);
            } else if(isTo()) {
                alpha = (int)MathUtils.lerp(MIN_ALPHA, MAX_ALPHA, transitProgress);
                scale = (float)MathUtils.lerp(MIN_SCALE, MAX_SCALE, transitProgress);
            } else {
                alpha = MIN_ALPHA;
                scale = MIN_SCALE;
            }
            
            widget.transform.x = dx;
            widget.transform.scale = scale;
            
            DrawTexture.get(widget).color.setAlpha(alpha);
            widget.dirty = true;
        }
        
        private boolean isFrom() {
            return id == lastActive;
        }
        
        private boolean isTo() {
            return id == active;
        }
        
    }
    
    private class AlphaAssign implements IGuiEventHandler<FrameEvent> {

        @Override
        public void handleEvent(Widget w, FrameEvent event) {
            int masterAlpha = getPage(w.getWidgetParent().getWidgetParent()).alpha;
            DrawTexture dt = DrawTexture.get(w);
            if(dt != null) {
                dt.color.setAlpha(masterAlpha);
            } else {
                TextBox.get(w).option.color.setAlpha(masterAlpha);
            }
        }
        
    }
    
    private class Selector extends Widget {
        final int MAX_PER_ROW = 4;
        final float MARGIN = 2.5f, SIZE = 15, STEP = SIZE + 3;
        
        List<Skill> available = new ArrayList();
        final int keyid;
        
        float width, height;
        
        public Selector(int _keyid) {
            keyid = _keyid;
            
            AbilityData aData = AbilityData.get(player);
            
            for(Skill s : aData.getControllableSkillList()) {
                if(!data.getPreset(active).hasControllable(s)) {
                    available.add(s);
                }
            }
            
            List<SelectionProvider> providers = new ArrayList<>();
            providers.add(new SelectionProvider(-1, Resources.getTexture("guis/preset_settings/cancel"), local("skill_remove")));
            for(Skill s : available) {
                providers.add(new SelectionProvider(s.getControlID(), s.getHintIcon(), s.getDisplayName()));
            }
            
            height = MARGIN * 2 + SIZE + STEP * (ldiv(providers.size(), MAX_PER_ROW) - 1);
            width = available.size() < MAX_PER_ROW ? 
                MARGIN * 2 + SIZE + STEP * (providers.size() - 1) : 
                MARGIN * 2 + SIZE + STEP * (MAX_PER_ROW - 1);
                
            transform.setSize(width, height);
            
            // Build the window and the widget
            listen(FrameEvent.class, (w, e) -> {
                Colors.bindToGL(CRL_WHITE);
                ACRenderingHelper.drawGlow(0, 0, width, height, 1, CRL_WHITE);
                
                Colors.bindToGL(CRL_BACK);
                HudUtils.colorRect(0, 0, width, height);
                
                String str; 
                Widget hovering = foreground.getHoveringWidget();
                if(hovering != null && hovering.getName().contains("_sel")) {
                    SelHandler sh = hovering.getComponent(SelHandler.class);
                    str = sh.selection.hint;
                } else {
                    str = local("skill_select");
                }

                FontOption opt = new FontOption(9, Colors.fromHexColor(0xffbbbbbb));
                double     len = font.getTextWidth(str, opt);

                Colors.bindToGL(CRL_BACK);
                HudUtils.colorRect(0, -13.5, len + 6, 11.5);

                ACRenderingHelper.drawGlow(0, -13.5, len + 6, 11.5, 1, CRL_GLOW);
                
                font.draw(str, 3, -12, opt);
                
                GL11.glColor4d(1, 1, 1, 1);
            });
            
            // Build all the skills that can be set
            for(int i = 0; i < providers.size(); ++i) {
                int row = i / MAX_PER_ROW, col = i % MAX_PER_ROW;
                SelectionProvider selection = providers.get(i);
                Widget single = new Widget();
                single.transform.setPos(MARGIN + col * STEP, MARGIN + row * STEP);
                single.transform.setSize(SIZE, SIZE);
                
                DrawTexture tex = new DrawTexture().setTex(selection.texture);
                single.addComponent(tex);
                single.addComponent(new Tint(Colors.monoBlend(1, 0), Colors.monoBlend(1, 0.2f), false));
                single.addComponent(new SelHandler(selection));
                addWidget("_sel" + i, single);
            }
        }
        
        private class SelHandler extends Component {
            
            final SelectionProvider selection;

            public SelHandler(SelectionProvider _selection) {
                super("_sel");
                selection = _selection;
                listen(LeftClickEvent.class, (w, e) -> {
                    onEdit(keyid, aData.getCategory().getControllable(selection.id));
                    Selector.this.dispose();
                });
            }
            
        }
    }
    
    private int ldiv(int a, int b) {
        return a % b == 0 ? a / b : a / b + 1;
    }
    
}