package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.client.sound.ACSounds;
import cn.academy.terminal.DonatorList;
import cn.academy.util.RegACKeyHandler;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.TerminalData;
import cn.academy.event.AppInstalledEvent;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.*;
import com.google.common.base.Preconditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class TerminalUI extends AuxGui {

    private static final String OVERRIDE_GROUP = "AC_Terminal";

    private static AuxGui current = null;

    private static final double BALANCE_SPEED = 3000; //pixel/s
    public static final int MAX_MX = 605, MAX_MY = 740;
    
    static final ResourceLocation APP_BACK = tex("app_back"), APP_BACK_HDR = tex("app_back_highlight"), CURSOR = tex("cursor");
    
    final double SENSITIVITY = 0.7;

    private static WidgetContainer loaded;

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        loaded = CGUIDocument.read(new ResourceLocation("academy:guis/terminal.xml"));
    }
    
    CGui gui;
    
    Widget root;

    TerminalMouseHelper helper;
    MouseHelper oldHelper;
    LeftClickHandler clickHandler;
    
    float mouseX, mouseY;
    float buffX, buffY; //Used for rotation judging. Will balance to mouseX and mouseY at the rate of BALANCE_SPEED.
    
    double createTime;
    double lastFrameTime;
    
    int selection = 0;
    int scroll = 0;
    List<Widget> apps = new ArrayList<>();
    
    public TerminalUI() {
        gui = new CGui();
        gui.addWidget(root = loaded.getWidget("back").copy());
        
        buffX = buffY = mouseX = mouseY = 150;

        consistent = false;

        MinecraftForge.EVENT_BUS.register(this);

        initGui();
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        oldHelper = mc.mouseHelper;
        mc.mouseHelper = helper = new TerminalMouseHelper();

        KeyManager.dynamic.addKeyHandler("terminal_click",KeyManager.MOUSE_LEFT, clickHandler = new LeftClickHandler());
        ControlOverrider.override(OVERRIDE_GROUP, KeyManager.MOUSE_LEFT);

        // There is a chance that About App will be opened
        //   to improve user experience we request that ahead of time
        DonatorList.Instance.tryRequest();
    }
    
    @Override
    public void onDisposed() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.mouseHelper = oldHelper;
        KeyManager.dynamic.removeKeyHandler("terminal_click");
        ControlOverrider.endOverride(OVERRIDE_GROUP);
    }

    @Override
    public void draw(ScaledResolution sr) {
        //Frame update
        selection = (int)((mouseY - 0.01) / MAX_MY * 3) * 3 + (int)((mouseX - 0.01) / MAX_MX * 3);
        
        if(mouseY == 0) {
            mouseY = 1;
            if(scroll > 0) scroll--;
        }
        if(mouseY == MAX_MY) {
            mouseY -= 1;
            if(scroll < getMaxScroll()) scroll++;
        }
        
        //Draw
        Minecraft mc = Minecraft.getMinecraft();
        double time = GameTimer.getTime();
        if(lastFrameTime == 0) lastFrameTime = time;
        double dt = time - lastFrameTime;
        
        mouseX += helper.dx * SENSITIVITY;
        mouseY -= helper.dy * SENSITIVITY;
        mouseX = Math.max(0, Math.min(MAX_MX, mouseX));
        mouseY = Math.max(0, Math.min(MAX_MY, mouseY));
        
        buffX = balance(dt, buffX, mouseX);
        buffY = balance(dt, buffY, mouseY);
        
        helper.dx = helper.dy = 0;
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        float aspect = (float)mc.displayWidth / mc.displayHeight;
        GLU.gluPerspective(50, 
             aspect, 
             1f, 100);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4d(1, 1, 1, 1);
        
        double scale = 1.0 / 310;
        GL11.glTranslated(.35 * aspect, 1.2, -4);
        
        GL11.glTranslated(1, -1.8, 0);
        
        GL11.glRotated(-1.6, 0, 0, 1);
        GL11.glRotated(-18 - 4 * (buffX / MAX_MX - 0.5) + 1 * Math.sin(time / 1000.0), 0, 1, 0);
        GL11.glRotated(7 + 4 * (buffY / MAX_MY - 0.5), 1, 0, 0);
        
        //DEBUG CODE
//        GL11.glPointSize(20);
//        GL11.glColor4d(1, 1, 1, 1);
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        GL11.glBegin(GL11.GL_POINTS);
//        GL11.glVertex3f(0, 0, 0);
//        GL11.glEnd();
//        GL11.glEnable(GL11.GL_TEXTURE_2D);
        
        GL11.glTranslated(-1, 1.8, 0);
        
        GL11.glScaled(scale, -scale, scale);
        
        gui.draw(mouseX, mouseY);
        
        {
            GL11.glPushMatrix();
            double csize = (getSelectedApp() == null ? 1 : 1.3) * (20 + Math.sin(time / 300.0) * 2);
            RenderUtils.loadTexture(CURSOR);
            
            GL11.glColor4d(1, 1, 1, .4);
            GL11.glTranslated(0, 0, -2);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            HudUtils.rect(-csize/2 + buffX, -csize/2 + buffY + 120, csize, csize);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glPopMatrix();
        }
        
        GL11.glPopMatrix();
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glCullFace(GL11.GL_BACK);
    }

    @SubscribeEvent
    public void _onAppInstalled(AppInstalledEvent evt) {
        if (SideUtils.isClient()) {
            updateAppList(TerminalData.get(Minecraft.getMinecraft().player));
        }
    }

    private float balance(double dt, float from, float to) {
        double d = to - from;
        return (float) (from + Math.min(BALANCE_SPEED * dt, Math.abs(d)) * Math.signum(d));
    }
    
    private void initGui() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        
        final TerminalData data = TerminalData.get(player);
        
        createTime = GameTimer.getTime();

        {
            Widget widget = root.getWidget("text_appcount");
            TextBox textBox = widget.getComponent(TextBox.class);
            widget.listen(FrameEvent.class, (w, e) -> {
                int currentTime = (int) (player.world.getWorldTime() % 24000);
                int hour = currentTime / 1000;
                int minutes = (currentTime % 1000) * 60 / 1000;

                String countText = I18n.translateToLocalFormatted("ac.gui.terminal.appcount", apps.size());
                String timeText = wrapTime(hour) + ":" + wrapTime(minutes);
                textBox.content = countText + ", " + timeText;
            });
        }

        root.getWidget("text_username").getComponent(TextBox.class)
            .setContent(player.getName());

        // Obsolete stuff
        root.removeWidget("text_loading");
        root.removeWidget("icon_loading");

        updateAppList(data);

        createTime = GameTimer.getTime();
        
        root.getWidget("arrow_up").listen(FrameEvent.class, 
        (w, e) -> {
            w.getComponent(DrawTexture.class).enabled = scroll > 0;
        });
        
        root.getWidget("arrow_up").listen(FrameEvent.class,
        (w, e) -> {
            w.getComponent(DrawTexture.class).enabled = scroll > 0;
        });
        
        root.getWidget("arrow_down").listen(FrameEvent.class,
        (w, e) -> {
            w.getComponent(DrawTexture.class).enabled = scroll < getMaxScroll();
        });
        
        root.getWidget("icon_loading").listen(FrameEvent.class,
        (w, e) -> {
            w.getComponent(DrawTexture.class).color.setAlpha(
                Colors.f2i(0.1f + 0.45f * (1 + MathHelper.sin((float) GameTimer.getTime() * 5)))
            );
        });
        
        root.getWidget("text_loading").listen(FrameEvent.class,
        (w, e) -> {
            w.getComponent(TextBox.class).option.color.setAlpha(
                Colors.f2i(
                    0.1f + ( 0.45f * (1 + MathHelper.sin( (float) GameTimer.getTime() * 5 ) ) )
                )
            );
        });
    }

    private String wrapTime(int val) {
        assert val >= 0 && val < 100;
        return val < 10 ? ("0" + val) : (String.valueOf(val));
    }
    
    private void updateAppList(TerminalData data) {
        for(Widget w : apps)
            w.dispose();
        apps.clear();
        for(App app : data.getInstalledApps()) {
            Widget w = createAppWidget(apps.size(), app);
            root.addWidget(w);
            apps.add(w);
        }


        root.getWidget("text_appcount").getComponent(TextBox.class).content =
            I18n.translateToLocalFormatted("ac.gui.terminal.appcount", apps.size());
        updatePosition();
    }
    
    private void updatePosition() {
        final float START_X = 65, START_Y = 155, STEP_X = 180, STEP_Y = 180;
        
        // Check if scroll is viable
        int max = getMaxScroll();
        if(scroll > max) scroll = max;
        
        for(Widget w : apps) {
            w.transform.doesDraw = false;
        }
        
        for(int i = scroll * 3; i < scroll * 3 + 9 && i < apps.size(); ++i) {
            int order = i - scroll * 3;
            Widget app = apps.get(i);
            app.transform.doesDraw = true;
            app.transform.x = START_X + STEP_X * (order % 3);
            app.transform.y = START_Y + STEP_Y * (order / 3);
            app.dirty = true;
        }
    }
    
    private int getMaxScroll() {
        int r;
        if(apps.size() % 3 == 0)
            r = apps.size() / 3;
        else r = apps.size() / 3 + 1;
        return Math.max(0, r - 3);
    }
    
    private Widget getSelectedApp() {
        int lookup = scroll + selection;
        return apps.size() <= lookup ? null : apps.get(lookup);
    }
    
    private double getLifetime() {
        return GameTimer.getTime() - createTime;
    }
    
    private Widget createAppWidget(int id, App app) {
        Widget ret = root.getWidget("app_template").copy();
        ret.transform.doesDraw = true;

        DrawTexture.get(ret.getWidget("icon")).texture = app.getIcon();
        TextBox.get(ret.getWidget("text")).content = app.getDisplayName();
        
        ret.addComponent(new AppHandler(id, app));
        
        return ret;
    }

    private static ResourceLocation tex(String name) {
        return Resources.getTexture("guis/data_terminal/" + name);
    }

    public static void passOn(AuxGui newGui) {
        Preconditions.checkNotNull(current);
        current.dispose();
        current = newGui;
        AuxGuiHandler.register(current);
    }
    
    @RegACKeyHandler(name = "open_data_terminal", keyID = Keyboard.KEY_LMENU)
    public static KeyHandler keyHandler = new KeyHandler() {
        
        @Override
        public void onKeyUp() {
            EntityPlayer player = getPlayer();
            TerminalData tData = TerminalData.get(player);
            
            if(tData.isTerminalInstalled()) {
                if(current == null || current.disposed) {
                    current = new TerminalUI();
                    AuxGuiHandler.register(current);
                } else if (current instanceof TerminalUI) {
                    current.dispose();
                    current = null;
                }
            } else {
                player.sendMessage(new TextComponentTranslation("ac.terminal.notinstalled"));
            }
        }
        
    };
    
    private class AppHandler extends Component {
        
        final int id;
        final App app;

        DrawTexture drawer;
        TextBox text;
        DrawTexture icon;
        
        boolean lastSelected = true;
        
        public AppHandler(int _id, App _app) {
            super("AppHandler");
            id = _id;
            app = _app;

            listen(FrameEvent.class, (w, e) -> {
                float mAlpha = MathUtils.clampf(0.0f, 1.0f, (float) (getLifetime() - ((id + 1) * 0.1f)) / 0.40f);
                boolean selected = getSelectedApp() == w;
                
                if(selected) {
                    if(!lastSelected) {
                        ACSounds.playClient(Minecraft.getMinecraft().player, "terminal.select", SoundCategory.MASTER,0.2f);
                    }
                    drawer.texture = APP_BACK_HDR;

                    icon.zLevel = 40;
                    drawer.zLevel = text.zLevel = (float) icon.zLevel;
                    
                    drawer.color.setAlpha(Colors.f2i(mAlpha));
                    icon.color.setAlpha(Colors.f2i(0.8f * mAlpha));
                    text.option.color.setAlpha(Colors.f2i(0.1f + 0.72f * mAlpha));
                } else {
                    drawer.texture = APP_BACK;

                    icon.zLevel = 10;
                    drawer.zLevel = text.zLevel = (float) icon.zLevel;
                    
                    drawer.color.setAlpha(Colors.f2i(mAlpha));
                    icon.color.setAlpha(Colors.f2i(0.6f * mAlpha));
                    text.option.color.setAlpha(Colors.f2i(0.10f + 0.1f * mAlpha));
                }
                
                lastSelected = selected;
            });

        }
        
        @Override
        public void onAdded() {
            super.onAdded();

            drawer = DrawTexture.get(widget);
            text = TextBox.get(widget.getWidget("text"));
            icon = DrawTexture.get(widget.getWidget("icon"));
            icon.color.setAlpha(0);
            drawer.color.setAlpha(0);
            text.option.color.setAlpha(Colors.f2i(0.1f));
        }
    }
    
    static AppHandler getHandler(Widget w) {
        return w.getComponent(AppHandler.class);
    }
    
    private class LeftClickHandler extends KeyHandler {
        
        @Override
        public void onKeyUp() {
            Widget app = getSelectedApp();
            if(app != null) {
                AppHandler handler = getHandler(app);
                AppEnvironment env = handler.app.createEnvironment();
                TerminalData data = TerminalData.get(getPlayer());
                
                env.app = handler.app;
                env.terminal = TerminalUI.this;
                
                //ACSounds.playClient(getPlayer(), "terminal.confirm", 0.5f);
                env.onStart();
            }
        }
        
    }

    public class TerminalMouseHelper extends MouseHelper {

        public int dx, dy;

        @Override
        public void mouseXYChange() {
            this.dx = Mouse.getDX();
            this.dy = Mouse.getDY();
        }
    }

}