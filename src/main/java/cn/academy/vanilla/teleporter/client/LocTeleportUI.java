/**
 * 
 */
package cn.academy.vanilla.teleporter.client;

import cn.academy.vanilla.teleporter.data.LocTeleData;
import cn.academy.vanilla.teleporter.data.LocTeleData.Location;
import cn.academy.vanilla.teleporter.skills.LocationTeleport;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.LIGui;
import cn.lambdalib.cgui.gui.LIGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.ElementList;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.Tint;
import cn.lambdalib.cgui.gui.component.VerticalDragBar;
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

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
		player = data.getEntity();
		drawBack = false;

		init();
	}

	int wait1, wait2;

	private void init() {
		pageInspect = loaded.getWidget("inspect").copy();
		pageAdd = loaded.getWidget("add").copy();
		pageAction = loaded.getWidget("action").copy();

		pageAction.transform.doesDraw = false;

		/* Inspect */ {
			Widget area = pageInspect.getWidget("area");

			rebuildInspectPanel();

			Widget dragbar = pageInspect.getWidget("dragbar");
			dragbar.listen(DraggedEvent.class, (w, e) -> {
				ElementList list = ElementList.get(area);
				list.setProgress((int) (VerticalDragBar.get(w).getProgress() * list.getMaxProgress()));
			});
		}

		/* Add */ {

			Widget area = pageAdd.getWidget("back");
			area.listen(FrameEvent.class, (w, event) -> {
				if (wait1 == 0) {
					wait1 = 100;
					w.getWidget("text_disabled").transform.doesDraw = !canRecordLocation();
				} else {
					wait1--;
				}
			});

			area.getWidget("button").listen(LeftClickEvent.class, (w, event) -> {
				if (canRecordLocation()) {
					data.add(LocationTeleport.toLocation(player,
							TextBox.get(pageAdd.getWidget("back/text_input")).content));
					player.closeScreen();
				}
			});

			TextBox.get(area.getWidget("text_coord"))
					.setContent(local("location") + LocationTeleport.toLocation(player, "def").formatCoords());
		}

		/* Action */ {
			pageAction.listen(FrameEvent.class, (w, event) -> {
				if (wait2 == 0) {
					wait2 = 100;
					w.getWidget("text_nocp").transform.doesDraw = !LocationTeleport.canPerform(player, selection);
				} else {
					wait2--;
				}
			});

			pageAction.getWidget("button").listen(LeftClickEvent.class, (w, e) -> {
				LocationTeleport.performAction(player, selection);
				player.closeScreen();
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
		for (int i = 0; i < data.getLocCount(); ++i) {
			final int id = i;

			Location l = data.get(i);
			Widget single = template.copy();
			single.transform.doesDraw = true;

			TextBox.get(single.getWidget("text")).setContent(l.name);

			single.getWidget("cancel").listen(LeftClickEvent.class, (Widget w, LeftClickEvent e) -> {
				data.removeAt(id);
				rebuildInspectPanel();
			});

			single.listen(LeftClickEvent.class, (w, e) -> {
				updateSelection(l);
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
		TextBox.get(pageAction.getWidget("text_dist")).setContent(local("distance")
				+ (diffdim ? "N/A" : String.format("%.1f", player.getDistance(selection.x, selection.y, selection.z))));
		TextBox.get(pageAction.getWidget("text_dim")).setContent(local("dim") + loc.dimension);
	}

	private boolean canRecordLocation() {
		if (TextBox.get(pageAdd.getWidget("back/text_input")).equals(""))
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
