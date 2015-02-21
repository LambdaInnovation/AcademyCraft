/**
 * 
 */
package cn.academy.energy.client.gui;

import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.energy.block.tile.impl.ContainerNode;
import cn.academy.energy.block.tile.impl.TileNode;
import cn.academy.energy.client.gui.Dialogues.DiagState;
import cn.academy.energy.client.gui.Dialogues.InputPassword;
import cn.academy.energy.client.gui.Dialogues.StateDiag;
import cn.academy.energy.msg.node.MsgInitNode;
import cn.academy.energy.msg.node.MsgNodeGuiLoad;
import cn.academy.energy.msg.node.MsgNodeLoadList;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.gui.LIGuiContainer;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.DragBar;
import cn.liutils.api.gui.widget.ListVertical;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * @author WeathFolD
 *
 */
public class GuiNode extends LIGuiContainer {
	
	ResourceLocation 
		TEX = new ResourceLocation("academy:textures/guis/wireless_node.png"),
		TEX_SELECT = new ResourceLocation("academy:textures/guis/node_choose_net.png");
	
	static final int[] COLOR = { 133, 240, 240 };
	
	//Parents/Associations
	final ContainerNode node;
	final TileNode tile;
	
	//Sync flags
	public boolean synced; //flag set by Messages indicating the packet was sent
	public boolean listSynced; //whether channel list was synced.
	
	//Sync data
	public String curChannel;
	public List<String> channels;
	public int nNodes;
	public int nGens;

	//pages
	Page mainPage;
	StateDiag stateDiag;
	Choose choosePage;
	
	public GuiNode(ContainerNode c) {
		super(c);
		node = c;
		tile = c.node;
		AcademyCraft.netHandler.sendToServer(new MsgNodeGuiLoad.Request(tile));
		AcademyCraft.netHandler.sendToServer(new MsgNodeLoadList.Request(tile));
		reinit();
	}
	
	private void reinit() {
		gui = new LIGui();
		mainPage = null;
		choosePage = null;
		gui.addWidget(mainPage = new Page());
	}
	
	@Override
	public boolean isSlotActive() {
		return mainPage.doesListenKey;
	}
	
	//---Main page
	private class Page extends Widget {
		public Page() {
			this.alignStyle = AlignStyle.CENTER;
			this.setSize(207.5, 205.333333);
			this.initTexDraw(TEX, 0, 0, 313, 308);
			this.setTexResolution(384, 384);
		}
		
		@Override
		public void draw(double mx, double my, boolean hov) {
			GL11.glPushMatrix();
			GL11.glTranslated(-.2, 0.5, 0);
			super.draw(mx, my, hov);
			
			//44.7 40 35 308 120.67 8.67 181 13
			GL11.glColor4d(1, 1, 1, 1);
			double prog = tile.getEnergy() / tile.getMaxEnergy();
			HudUtils.drawRect(45.8, 54.8, 35, 308, 120.67 * prog, 8.67, 181 * prog, 13);
			GL11.glPopMatrix();
			
			RenderUtils.bindColor(COLOR);
			String cn = synced ? (curChannel == null ? ACLangs.notConnected() : curChannel) : ACLangs.loading();
			drawText(cn, 81, 14.5, 7, Align.LEFT);
			
			RenderUtils.bindIdentity();
		}
		
		@Override
		public void onAdded() {
			addWidget(new Indicator());
		}
	}
	
	private class Indicator extends Widget {
		public Indicator() {
			setPos(170.5, 11);
			setSize(18 / 1.5, 24 / 1.5);
		}
		
		@Override
		public void draw(double mx, double my, boolean hov) {
			final double tw = 18, th = 24;
			RenderUtils.loadTexture(TEX);
			if(isConnected()) {
				HudUtils.drawRect(0, 0, 315, 62, tw / 1.5, th / 1.5, tw, th);
			} else {
				HudUtils.drawRect(0, 0, 315, 22, tw / 1.5, th / 1.5, tw, th);
			}
		}
		
		@Override
		public void onMouseDown(double mx, double my) {
			mainPage.doesListenKey = false;
			gui.addWidget(choosePage = new Choose());
		}
	}
	
	//---Choose page
	private class Choose extends Widget {
		public Choose() {
			this.setSize(150, 216.5);
			this.initTexDraw(TEX_SELECT, 0, 0, 300, 433);
			this.setTexResolution(512, 512);
			
			choosePage = this;
			this.alignStyle = AlignStyle.CENTER;
		}
		
		@Override
		public void onAdded() {
			ChannelList list;
			addWidget(list = new ChannelList());
			ChooseDB db = new ChooseDB();
			addWidget(db);
			list.setDragBar(db);
		}
		
		@Override
		public void draw(double mx, double my, boolean hover) {
			GL11.glPushMatrix();
			GL11.glTranslated(-this.getNode().x, -this.getNode().y, 0);
			GL11.glColor4d(0, 0, 0, .7);
			HudUtils.drawModalRect(0, 0, GuiNode.this.width, GuiNode.this.height);
			GL11.glPopMatrix();
			
			GL11.glColor4d(1, 1, 1, 1);
			super.draw(mx, my, hover);
		}
		
		public void onClose() {
			mainPage.doesListenKey = true;
			dispose();
		}
	}
	
	private class ChooseDB extends DragBar {

		public ChooseDB() {
			super(135, 38, 16, 172.5, 16);
			this.initTexDraw(TEX_SELECT, 304, 0, 32, 32);
		}
		
	}
	
	private class ChannelList extends ListVertical {
		
		boolean loaded;

		public ChannelList() {
			super("channel", 3.5, 38, 128.5, 172.5);
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			if(!loaded && listSynced) {
				loaded = true;
				init();
			}
		}
		
		@Override
		public void onAdded() {
			if(listSynced) {
				loaded = true;
				init();
			}
		}
		
		private void init() {
			for(String s : channels) {
				this.addWidget(new OneChannel(s));
			}
		}
		
	}
	
	private class OneChannel extends Widget {
		private final int 
			ACTIVE_COLOR[] = { 57, 150, 150, 180 },
			FONT_COLOR[] = { 142, 255, 255, 233 };
		final String channel;
		
		public OneChannel(String st) {
			setSize(129.5, 16);

			channel = st;
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			if(h) {
				RenderUtils.bindColor(ACTIVE_COLOR);
				HudUtils.drawModalRect(0, 0, width, height);
			}
			RenderUtils.bindColor(100, 255, 255);
			drawText(channel, 5, 4, 7);
		}
		
		@Override
		public void onMouseDown(double mx, double my) {
			gui.addWidget(new NInputPassword(channel));
			choosePage.dispose();
		}
	}
	
	private class NInputPassword extends InputPassword {

		public NInputPassword(String _cn) {
			super(_cn);
		}

		@Override
		public void performAction(String pwd) {
			gui.addWidget(stateDiag = new StateDiag());
			AcademyCraft.netHandler.sendToServer(new MsgInitNode(tile, cn, pwd));
			dispose();
		}
		
	}
	
	public void finishInit(boolean suc) {
		if(stateDiag != null) {
			stateDiag.state = suc ? DiagState.SUCCESS : DiagState.FAIL;
			stateDiag.initCancel();
		} else {
			AcademyCraft.log.error("WTF");
		}
	}
	
	private boolean isConnected() {
		return curChannel != null;
	}
	
	private static void drawText(String str, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(str, x, y, size);
	}
	
	private static void drawText(String str, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(str, x, y, size, align);
	}
	
	private static void drawAdjusted(String str, double x, double y, double size, Align align, double cst) {
		ACClientProps.FONT_YAHEI_32.drawAdjusted(str, x, y, size, align, cst);
	}
	
}
