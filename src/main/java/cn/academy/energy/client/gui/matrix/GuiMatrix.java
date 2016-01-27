/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.gui.matrix;

import cn.academy.energy.block.ContainerMatrix;
import cn.academy.energy.block.TileMatrix;
import cn.academy.energy.client.gui.matrix.GuiMatrixSync.ActionResult;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.CGui;
import cn.lambdalib.cgui.gui.CGuiScreenContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

/**
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
public class GuiMatrix extends CGuiScreenContainer {
    
    static WidgetContainer document = CGUIDocument.panicRead(new ResourceLocation("academy:guis/matrix.xml"));
    
    //--------
    
    //Synced states
    boolean receivedSync;
    boolean isLoaded;
    
    String ssid;
    int nodes;
    
    //Action
    boolean waitingForResult;
    long resultReceivedTime; //Used for anim display time ctrl.
    ActionResult result;
    
    //Meta objects
    final TileMatrix tile;
    
    final ContainerMatrix container;
    
    final EntityPlayer player;
    
    Widget pageMain, pageSSID, pageCheck;

    Widget check_info, check_markDrawer;
    
    long syncedTime = -1;

    public GuiMatrix(ContainerMatrix c) {
        super(c);
        tile = c.tile;
        container = c;
        player = Minecraft.getMinecraft().thePlayer;
        
        load();
    }
    
    public void receiveSync(NBTTagCompound tag) {
        receivedSync = true;
        
        isLoaded = tag.getBoolean("loaded");
        nodes = tag.getInteger("nodes");
        
        TextBox box = TextBox.get(pageMain.getWidget("text_ssid2"));
        if(isLoaded) {
            ssid = tag.getString("ssid");
            box.content = ssid;
        } else {
            box.content = "Not Loaded";
        }
    }
    
    private static String local(String s) {
        return StatCollector.translateToLocal("ac.gui.matrix." + s);
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        GL11.glPushMatrix();
        GL11.glTranslated(-guiLeft, -guiTop, 0);
        
        Widget w = gui.getTopWidget(x, y);
        if(w != null) {
            String text = null;
            switch(w.getName()) {
            case "progress_cap":
                text = local("capacity") + ": " + nodes + "/" + tile.getCapacity();
                break;
            case "progress_lat":
                text = local("bandwidth") + String.format(": %.1f IF/t", tile.getBandwidth());
                break;
            case "progress_ran":
                text = local("range") + String.format(": %.2fm", tile.getRange());
                break;
            default:
                break;
            }
            
            if(text != null) {
                this.drawHoveringText(Arrays.asList(new String[] { text }), x, y, this.fontRendererObj);
            }
        }
        
        GL11.glPopMatrix();
        
        if(GameTimer.getTime() - syncedTime > 1200)
            GuiMatrixSync.sendSyncRequest(this);
    }
    
    
    private void startWaiting() {
        waitingForResult = true;
        result = ActionResult.WAITING;
        
        updateCheckState();
        pageCheck.transform.doesDraw = true;
        pageMain.transform.doesListenKey = false;
    }
    
    /**
     * May called by sync method or gui itself, to update state animation.
     */
    public void receiveActionResult(ActionResult result, boolean needSync) {
        if(waitingForResult) {
            waitingForResult = false;
            this.result = result;
            resultReceivedTime = GameTimer.getAbsTime();
            updateCheckState();
            
            if(needSync) {
                GuiMatrixSync.sendSyncRequest(this);
            }
        }
    }
    
    private void load() {
        GuiMatrixSync.sendSyncRequest(this);
        
        CGui gui = getGui();
        pageMain = document.getWidget("window_main").copy();
        pageSSID = document.getWidget("window_init").copy();
        pageCheck = document.getWidget("window_check").copy();

        check_markDrawer = pageCheck.getWidget("mark_check2");
        check_info = pageCheck.getWidget("test_info");
        
        gui.addWidget(pageMain);
        gui.addWidget(pageSSID);
        gui.addWidget(pageCheck);
        
        pageSSID.transform.doesDraw = false;
        pageCheck.transform.doesDraw = false;
        
        wrapButton(pageMain.getWidget("button_config"));
        
        wrapButton(pageSSID.getWidget("button_yes"));
        wrapButton(pageSSID.getWidget("button_no"));
        
        wrapButton(pageCheck.getWidget("button_close"));

        /* Main Events */ {
            pageMain.getWidget("button_config").listen(LeftClickEvent.class, (w, e) -> {
                if (receivedSync) {
                    openInitWindow();
                }
            });

            pageMain.listen(FrameEvent.class, (w, e) -> {
                ProgressBar.get(pageMain.getWidget("progress_cap")).progress = ((double) nodes / tile.getCapacity());
                ProgressBar.get(pageMain.getWidget("progress_lat")).progress = (tile.getBandwidth() / TileMatrix.MAX_BANDWIDTH);
                ProgressBar.get(pageMain.getWidget("progress_ran")).progress = (tile.getRange() / TileMatrix.MAX_RANGE);
            });
        }

        /* SSID input events */ {
            pageSSID.listen(FrameEvent.class, (w, event) ->
            {
                if(!pageMain.transform.doesListenKey) {
                    RenderUtils.drawBlackout();
                }
            });

            pageSSID.getWidget("button_yes").listen(LeftClickEvent.class, (w, e) -> {
                startWaiting();

                if(!isLoaded) {
                    //Do init
                    String ssid = ssidContent(1), pw1 = ssidContent(2), pw2 = ssidContent(3);
                    if(pw1.equals(pw2) && !ssid.isEmpty()) {
                        GuiMatrixSync.fullInit(player, tile, ssid, pw1);
                    } else {
                        receiveActionResult(ActionResult.INVALID_INPUT, false);
                    }
                } else {
                    //Update pass
                    String oldpw = ssidContent(1), pw1 = ssidContent(2), pw2 = ssidContent(3);
                    if(pw1.equals(pw2)) {
                        GuiMatrixSync.passwordUpdate(player, tile, oldpw, pw1);
                    } else {
                        receiveActionResult(ActionResult.INVALID_INPUT, false);
                    }
                }

                pageSSID.transform.doesDraw = false;
            });

            pageSSID.getWidget("button_no").listen(LeftClickEvent.class, (w, e) -> {
                //Close without doing anything
                pageSSID.transform.doesDraw = false;
                pageMain.transform.doesListenKey = true;
            });
        }

        /* Check page events */ {
            Widget markBorder = pageCheck.getWidget("mark_check1");

            pageCheck.listen(FrameEvent.class, (w, event) ->
            {
                if(!pageMain.transform.doesListenKey) {
                    RenderUtils.drawBlackout();
                }
            });

            pageCheck.getWidget("mark_check1").listen(FrameEvent.class, (w, e) -> {
                double alpha = 0.7 * 0.5 * (1 + Math.sin(GameTimer.getAbsTime() / 600.0)) + 0.3;
                DrawTexture.get(markBorder).color.a = alpha;
            });

            pageCheck.getWidget("button_close").listen(LeftClickEvent.class, (w, e) -> {
                pageCheck.transform.doesDraw = false;
                pageMain.transform.doesListenKey = true;
            });
        }
    }

    /**
     * Gets the content of three tex boxes in pageSSID
     */
    private String ssidContent(int iid) {
        return TextBox.get(pageSSID.getWidget("text_" + iid)).content;
    }
    
    @Override
    public boolean isSlotActive() {
        return pageMain.transform.doesListenKey;
    }
    
    @Override
    protected boolean containerAcceptsKey(int key) {
        return false;
    }

    private void updateCheckState() {
        DrawTexture.get(check_markDrawer).texture = result.markSrc;
        TextBox.get(check_info).content = result.getDescription();
    }
    
    private void wrapButton(Widget w) {
        DrawTexture drawer = w.getComponent("DrawTexture");
        final Color hoverColor = new Color(1, 1, 1, 1), idleColor = new Color(1, 1, 1, 0.3);
        drawer.color = idleColor;
        
        w.listen(FrameEvent.class, (ww, event) -> {
            drawer.color = event.hovering ? hoverColor : idleColor;
        });
    }
    
    private void openInitWindow() {
        TextBox box = pageSSID.getWidget("text_1").getComponent("TextBox");
        DrawTexture 
            drawSSID = DrawTexture.get(pageSSID.getWidget("input_ssid")),
            drawOldPW = DrawTexture.get(pageSSID.getWidget("input_oldpw"));
        box.content = "";
        box.allowEdit = true;
        
        if(isLoaded) {
            drawSSID.enabled = false;
            drawOldPW.enabled = true;
        } else {
            drawSSID.enabled = true;
            drawOldPW.enabled = false;
        }
        
        TextBox.get(pageSSID.getWidget("text_2")).content = "";
        TextBox.get(pageSSID.getWidget("text_3")).content = "";
        
        pageMain.transform.doesListenKey = false;
        pageSSID.transform.doesDraw = true;
    }
    
}
