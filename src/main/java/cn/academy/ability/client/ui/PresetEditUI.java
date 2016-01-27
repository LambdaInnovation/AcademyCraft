/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.ui;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.ability.api.data.PresetData.PresetEditor;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.CGui;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.Component;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.Tint;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.IGuiEventHandler;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
@Registrant
public class PresetEditUI extends GuiScreen {
    
    static final Color 
        CRL_BACK = new Color().setColor4i(49, 49, 49, 200),
        CRL_WHITE = new Color(1, 1, 1, 0.6),
        CRL_GLOW = new Color(1, 1, 1, 0.2);
    
    static WidgetContainer loaded;
    static Widget template;
    
    static final double STEP = 125;
    static final long TRANSIT_TIME = 350;
    static final double MAX_ALPHA = 1, MIN_ALPHA = 0.3;
    static final double MAX_SCALE = 1, MIN_SCALE = 0.8;
    
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
    
    static {
        loaded = CGUIDocument.panicRead(new ResourceLocation("academy:guis/preset_edit.xml"));
        template = loaded.getWidget("template");
    }
    
    final EntityPlayer player;
    final PresetData data;

    final IFont font = Resources.font();

    // lastActive is the preset ID before the transition.
    int lastActive, active;
    /**
     * The preset editor of the current active selection. Always not null.
     */
    PresetEditor editor;
    
    boolean transiting;
    long transitStartTime;
    long deltaTime; //Time that has passed since last transition
    double transitProgress;
    
    Widget selector;
    
    public PresetEditUI() {
        player = Minecraft.getMinecraft().thePlayer;
        data = PresetData.get(player);
        if(!data.isActive()) {
            throw new RuntimeException("Cannot open preset edit gui when data is dirty");
        }
        
        init();
    }
    
    @Override
    public void onGuiClosed() {
        editor.save();
    }
    
    private String local(String key) {
        return StatCollector.translateToLocal("ac.gui.preset_edit." + key);
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
        
        updateEditor();
    }
    
    private void updateEditor() {
        if(editor != null)
            editor.save();
        editor = data.createEditor(active);
    }
    
    private Widget createCopy() {
        Widget ret = template.copy();
        for(Widget w : ret.getDrawList()) {
            for(Widget w2 : w.getDrawList())
                w2.listen(FrameEvent.class, new AlphaAssign(), -1);
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
    }
    
    private double getXFor(int i, int active) {
        if(i == active) {
            return 0;
        }
        return STEP * (i - active);
    }
    
    private double getXFor(int i) {
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
        updateEditor();
    }
    
    // Foreground page
    private void onEdit(int keyID, int cid) {
        editor.edit(keyID, cid);
        getPage(get(foreground, active)).updateInfo();
    }
    
    private void saveEdit() {
        editor.save();
    }
    
    // Transition page
    private void updateTransit() {
        deltaTime = GameTimer.getAbsTime() - transitStartTime;
        
        transitProgress = (double)deltaTime / TRANSIT_TIME;
        if(transitProgress > 1) {
            transitProgress = 1;
        }
        
        for(int i = 0; i < 4; ++i) {
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
        for(int i = 0; i < 4; ++i) {
            Widget page = get(gui, i);
            getPage(page).updateInfo();
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
        protected double alpha;
        
        final int id;
        
        public Page(int _id) {
            super("Page");
            id = _id;
        }
        
        public void updateInfo() {
            
            Preset p = data.getPreset(id);
            byte[] pdata = (id == active && editor != null) ? editor.display : p.getData();
            
            for(int i = 0; i < 4; ++i) {
                Category cat = AbilityData.get(player).getCategory();
                Controllable c = cat.getControllable(pdata[i]);
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
            DrawTexture.get(widget).color.a = alpha;
        }
        
        
    }
    
    static Page getPage(Widget w) {
        return w.getComponent("Page");
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
                dt.color.a = page.alpha;
            });
            
            listen(LeftClickEvent.class, (w, e) -> {
                Page page = getPage(w.getWidgetParent());
                if(selector != null && !selector.disposed) {
                    selector.dispose();
                    selector = null;
                } else if(page.id == active) {
                    // Open the selector
                    selector = new Selector(keyid);
                    selector.transform.setPos(foreground.mouseX, foreground.mouseY);
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
                DrawTexture.get(w).color.a = alpha;
            });
        }
        
        @Override
        public void updatePosition() {
            double x0 = getXFor(id, lastActive), x1 = getXFor(id, active);
            double dx = MathUtils.lerp(x0, x1, transitProgress);
            double scale;
            
            if(isFrom()) {
                alpha = MathUtils.lerp(MAX_ALPHA, MIN_ALPHA, transitProgress);
                scale = MathUtils.lerp(MAX_SCALE, MIN_SCALE, transitProgress);
            } else if(isTo()) {
                alpha = MathUtils.lerp(MIN_ALPHA, MAX_ALPHA, transitProgress);
                scale = MathUtils.lerp(MIN_SCALE, MAX_SCALE, transitProgress);
            } else {
                alpha = MIN_ALPHA;
                scale = MIN_SCALE;
            }
            
            widget.transform.x = dx;
            widget.transform.scale = scale;
            
            DrawTexture.get(widget).color.a = alpha;
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
            double masterAlpha = getPage(w.getWidgetParent().getWidgetParent()).alpha;
            DrawTexture dt = DrawTexture.get(w);
            if(dt != null) {
                dt.color.a = masterAlpha;
            } else {
                TextBox.get(w).option.color.a = masterAlpha;
            }
        }
        
    }
    
    private class Selector extends Widget {
        final int MAX_PER_ROW = 4;
        final double MARGIN = 2.5, SIZE = 15, STEP = SIZE + 3;
        
        List<Skill> available = new ArrayList();
        final int keyid;
        
        double width, height;
        
        public Selector(int _keyid) {
            keyid = _keyid;
            
            AbilityData aData = AbilityData.get(player);
            Category c = aData.getCategory();
            
            for(Skill s : aData.getControllableSkillList()) {
                int cid = s.getControlID();
                if(!editor.hasMapping(cid)) {
                    available.add(s);
                }
            }
            
            List<SelectionProvider> providers = new ArrayList();
            providers.add(new SelectionProvider(-1, Resources.getTexture("guis/preset_settings/cancel"), local("skill_remove")));
            for(Skill s : available)
                providers.add(new SelectionProvider(s.getControlID(), s.getHintIcon(), s.getDisplayName()));
            
            height = MARGIN * 2 + SIZE + STEP * (ldiv(providers.size(), MAX_PER_ROW) - 1);
            width = available.size() < MAX_PER_ROW ? 
                MARGIN * 2 + SIZE + STEP * (providers.size() - 1) : 
                MARGIN * 2 + SIZE + STEP * (MAX_PER_ROW - 1);
                
            transform.setSize(width, height);
            
            // Build the window and the widget
            listen(FrameEvent.class, (w, e) -> {
                CRL_WHITE.bind();
                ACRenderingHelper.drawGlow(0, 0, width, height, 1, CRL_WHITE);
                
                CRL_BACK.bind();
                HudUtils.colorRect(0, 0, width, height);
                
                String str; 
                Widget hovering = foreground.getHoveringWidget();
                if(hovering != null && hovering.getName().contains("_sel")) {
                    SelHandler sh = hovering.getComponent("_sel");
                    str = sh.selection.hint;
                } else {
                    str = local("skill_select");
                }

                FontOption opt = new FontOption(9, new Color(0xffbbbbbb));
                double     len = font.getTextWidth(str, opt);

                CRL_BACK.bind();
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
                single.addComponent(new Tint(Color.monoBlend(1, 0), Color.monoBlend(1, 0.2), false));
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
                    onEdit(keyid, selection.id);
                    Selector.this.dispose();
                });
            }
            
        }
    }
    
    private int ldiv(int a, int b) {
        return a % b == 0 ? a / b : a / b + 1;
    }
    
}
