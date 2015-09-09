/**
 * 
 */
package cn.academy.vanilla.teleporter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.vanilla.teleporter.data.LocTeleData;
import cn.academy.vanilla.teleporter.data.LocTeleData.Location;
import cn.academy.vanilla.teleporter.skills.LocationTeleport;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiScreen;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.ElementList;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.component.Tint;
import cn.liutils.cgui.gui.component.VerticalDragBar;
import cn.liutils.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.liutils.cgui.gui.component.VerticalDragBar.DraggedHandler;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class LocTeleportUI extends LIGuiScreen {
	
	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/loctele.xml"));
	}
	
	Widget pageInspect, pageAdd, pageAction;
	
	public Location selection;
	
	final LocTeleData data;
	final EntityPlayer player;
	
	public LocTeleportUI() {
		data = LocTeleData.get(Minecraft.getMinecraft().thePlayer);
		player = data.getPlayer();
		drawBack = false;
		
		init();
	}
	
	private void init() {
		pageInspect = loaded.getWidget("inspect").copy();
		pageAdd = loaded.getWidget("add").copy();
		pageAction = loaded.getWidget("action").copy();
		
		pageAction.transform.doesDraw = false;
		
		/* Inspect */ {
			Widget area = pageInspect.getWidget("area");
			
			rebuildInspectPanel();
			
			Widget dragbar = pageInspect.getWidget("dragbar");
			dragbar.regEventHandler(new DraggedHandler() {

				@Override
				public void handleEvent(Widget w, DraggedEvent event) {
					ElementList list = ElementList.get(area);
					list.setProgress((int) 
						(VerticalDragBar.get(w).getProgress() * list.getMaxProgress()));
				}
				
			});
		}
		
		/* Add */ {
			
			Widget area = pageAdd.getWidget("back");
			area.regEventHandler(new FrameEventHandler() {
				
				int wait = 0;

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					if(wait == 0) {
						wait = 100;
						w.getWidget("text_disabled").transform.doesDraw = !canRecordLocation();
					} else {
						wait--;
					}
				}
				
			});
			
			area.getWidget("button").regEventHandler(new MouseDownHandler() {

				@Override
				public void handleEvent(Widget w, MouseDownEvent event) {
					if(canRecordLocation()) {
						data.add(LocationTeleport.toLocation(player, TextBox.get(pageAdd.getWidget("back/text_input")).content));
						player.closeScreen();
					}
				}
				
			});
			
			TextBox.get(area.getWidget("text_coord")).setContent(local("location") + LocationTeleport.toLocation(player, "def").formatCoords());
		}
		
		/* Action */ {
			pageAction.regEventHandler(new FrameEventHandler() {
				
				int wait = 0;

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					if(wait == 0) {
						wait = 100;
						w.getWidget("text_nocp").transform.doesDraw = !LocationTeleport.canPerform(player, selection);
					} else {
						wait--;
					}
				}
				
			});
			
			pageAction.getWidget("button").regEventHandler(new MouseDownHandler() {

				@Override
				public void handleEvent(Widget w, MouseDownEvent event) {
					LocationTeleport.performAction(player, selection);
					player.closeScreen();
				}
				
			});
			
		}
		
		gui.addWidget(pageInspect);
		gui.addWidget(pageAdd);
		gui.addWidget(pageAction);
	}
	
	private void rebuildInspectPanel() {
		Widget area = pageInspect.getWidget("area");
		area.removeComponent("ElementList");
		
		ElementList list = new ElementList();
		
		Widget template = area.getWidget("t_loc");
		for(int i = 0; i < data.getLocCount(); ++i) {
			final int id = i;
			
			Location l = data.get(i);
			Widget single = template.copy();
			single.transform.doesDraw = true;
			
			TextBox.get(single.getWidget("text")).setContent(l.name);
			
			single.getWidget("cancel").regEventHandler(MouseDownEvent.class,
				(Widget w, MouseDownEvent e) -> {
					data.removeAt(id);
					rebuildInspectPanel();
				});
			
			single.regEventHandler(new MouseDownHandler() {

				@Override
				public void handleEvent(Widget w, MouseDownEvent event) {
					updateSelection(l);
				}
				
			});
			
			list.addWidget(single);
			
			Tint tint = single.getComponent("Tint");
		}
		
		area.addComponent(list);
	}
	
	private void updateSelection(Location loc) {
		selection = loc;
		pageAction.transform.doesDraw = true;
		boolean diffdim = player.worldObj.provider.dimensionId != selection.dimension;
		
		TextBox.get(pageAction.getWidget("text_loc")).setContent(local("location") + selection.formatCoords());
		TextBox.get(pageAction.getWidget("text_dist")).setContent(
			local("distance") + 
			(diffdim ? "N/A" : String.format("%.1f", player.getDistance(selection.x, selection.y, selection.z))));
		TextBox.get(pageAction.getWidget("text_dim")).setContent(local("dim") + loc.dimension);
	}
	
	private boolean canRecordLocation() {
		if(TextBox.get(pageAdd.getWidget("back/text_input")).equals(""))
			return false;
		return LocationTeleport.canRecord(player);
	}
	
	private String local(String key) {
		return StatCollector.translateToLocal("ac.gui.loctele." + key);
	}
	
	@Override
    public boolean doesGuiPauseGame() {
        return false;
    }
	
}
