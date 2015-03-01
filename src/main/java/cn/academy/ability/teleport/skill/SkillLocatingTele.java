/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.teleport.skill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.academy.ability.teleport.data.LocationData;
import cn.academy.ability.teleport.data.LocationData.Location;
import cn.academy.ability.teleport.skill.SkillLocatingTele.GuiBase.GList.ListElem;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.event.ControlStateEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.msg.TeleportMsg;
import cn.academy.misc.util.ACUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.FocusedVList;
import cn.liutils.api.gui.widget.InputBox;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class SkillLocatingTele extends SkillBase {
	
	static final double scale = 0.5;
	
	private final static ResourceLocation tex = new ResourceLocation("academy:textures/guis/tp_locating_ui.png");
		
	public SkillLocatingTele() {
		setName("tp_loc");
		setLogo("tp/locating.png");
		setMaxLevel(10);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(40) { //TODO: Time settings invalid, wtf?

			@Override
			public State createSkill(EntityPlayer player) {
				return new LocState(player);
			}
			
		});
	}
	
	public static class LocState extends State {

		public LocState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {}

		@Override
		@SideOnly(Side.CLIENT) //Client-Only override.
		public boolean onFinish(boolean res) {
			if(isOpeningGui()) { //Opening gui, don't do anything.
				return false;
			}
			if(!res) return false;
			
			if(isRemote()) {
				if(this.getTickTime() < 10) {
					//Open teleport gui
					guiSelectHandler.openClientGui();
				} else {
					//Open create gui
					guiCreateHandler.openClientGui();
				}
			}
			return true;
		}

		@Override
		public void onHold() {}
		
	}
	
	@SideOnly(Side.CLIENT)
	private static boolean isOpeningGui() {
		return !ClientUtils.isPlayerInGame();
	}
	
	public static abstract class GuiBase extends LIGui {
		EntityPlayer player;
		LocationData data;
		BaseScreen mainScreen;
		
		static final int[] color = { 178, 178, 178, 180 };
		
		public static class GList extends FocusedVList {
			public GList() {
				super("list", 175, 40, 198, 138);
			}
			
			@Override
			public void onAdded() {
				LocationData data = LocationData.get(Minecraft.getMinecraft().thePlayer);
				for(int i = 0; i < data.getLocCount(); ++i) {
					addWidget(new ListElem(data.getLocation(i), i));
				}
			}
			
			public class ListElem extends Widget {
				final LocationData.Location data;
				final int n;
				
				public ListElem(LocationData.Location _data, int _n) {
					data = _data;
					n = _n;
					setSize(198, 25);
				}
				
				public void draw(double mx, double my, boolean hov) {
					if(getGui().getFocus() == this) {
						RenderUtils.bindColor(color);
						HudUtils.drawModalRect(0, 0, width, height);
					} else if(hov) {
						RenderUtils.bindColor(178, 178, 178, 60);
						HudUtils.drawModalRect(0, 0, width, height);
					}
					RenderUtils.bindColor(200, 200, 200, 200);
					ACUtils.drawText(data.name, 5, 5, 14, 180);
				}
				
				@Override
				public boolean doesNeedFocus() {
					return true;
				}
				
				@Override
				public void onMouseDown(double mx, double my) {
					GList.this.setFocus(n);
				}
			}
		}
		
		public GuiBase() {}
		
		void init() {
			boolean first = mainScreen == null;
			player = Minecraft.getMinecraft().thePlayer;
			data = LocationData.get(player);
			
			if(!first)
				this.clear();
			long time = first ? Minecraft.getSystemTime() : mainScreen.createTime;
			addWidget(mainScreen = new BaseScreen(time));
			mainScreen.addWidget(new GList());
		}
		
		abstract String getHint();
		
		private class BaseScreen extends Widget {
			 long createTime;
			
			public BaseScreen(long time) {
				super(413, 219);
				this.alignStyle = AlignStyle.CENTER;
				createTime = time;
				
				this.scale = 0.6;
			}
			 
			double[][] shadowOffsets = {
					{ 51, -32 },
					{ -60, -60 },
					{ 66, 30 },
					{ -30, 60 }
			};
			
			@Override
			public void draw(double mx, double my, boolean hov) {
				long dt = Minecraft.getSystemTime() - createTime;
				double mAlpha = Math.min(1.0, dt / 800.0);
				
				HudUtils.setTextureResolution(512, 512);
				RenderUtils.loadTexture(tex);
				
				GL11.glColor4d(1, 1, 1, mAlpha);
				double ratio = Math.min(1.0, dt / 600.0);
				for(double[] off : shadowOffsets) {
					drawOneShadow(ratio * off[0], ratio * off[1]);
				}
				drawMainWindow();
				
				RenderUtils.bindColor(220, 220, 220, 200);
				ACUtils.drawText(getHint(), 45, 48, 15);
			}
			
			private void drawOneShadow(double ox, double oy) {
				GL11.glPushMatrix();
				rect(ox, oy, 0, 219, 413, 219);
				GL11.glPopMatrix();
			}
			
			private void drawMainWindow() {
				rect(0, 0, 0, 0, 413, 219);
			}
			
			protected void rect(double x, double y, double u, double v, double w2, double h2) {
				HudUtils.drawRect(x, y, u, v, w2, h2, w2, h2);
			}
		}
	}
	
	public static class GuiCreate extends GuiBase {
		
		InputBox box;
		
		int targDim;
		float targX, targY, targZ;
		
		public GuiCreate() {
			init();
		}
		
		void init() {
			super.init();
			targX = (float) player.posX;
			targY = (float) player.posY;
			targZ = (float) player.posZ;
			targDim = player.worldObj.provider.dimensionId;
			
			//sub widgets
			mainScreen.addWidget(box = new InputBox(42, 148, 127, 25, 20, 1, 15));
			box.setTextColor(255, 255, 255, 180);
			box.setFont(ACClientProps.FONT_YAHEI_32);
		}
		
		private boolean isCreating() {
			return box.isFocused();
		}
		
		private boolean isRemoving() {
			Widget focus = getFocus();
			return focus != null && focus != box;
		}
		
		@Override
		public void keyTyped(char ch, int kid) {
			super.keyTyped(ch, kid);
			if(isRemoving()) {
				if(Keyboard.KEY_DELETE == kid) {
					//confirm the del
					data.clientRemove(((GList.ListElem)getFocus()).n);
					
					//Re-construct elements brutely.
					init();
				}
			}
			if(isCreating()) {
				if(Keyboard.KEY_RETURN == kid) {
					//confirm the sel
					data.clientAdd(new Location(box.getContent(), targDim, targX, targY, targZ));
					player.closeScreen();
				}
			}
		}

		@Override
		String getHint() {
			StringBuilder sb = new StringBuilder();
			if(isCreating()) {
				 sb.append(String.format("x %.1f\ny %.1f\nz %.1f\n", targX, targY, targZ));
				 sb.append("ENTER: add\n");
			}
			if(isRemoving()) {
				GList.ListElem le = (GList.ListElem) getFocus();
				sb.append(String.format("x %.1f\ny %.1f\nz %.1f\n", le.data.x, le.data.y, le.data.z));
				sb.append("DEL: remove\n");
			}
			sb.append("ESC: quit\n");
			return sb.toString();
		}
	}
	
	public static class GuiSelect extends GuiBase {
		
		public GuiSelect() {
			init();
		}
		
		@Override
		protected void init() {
			super.init();
			
		}

		@Override
		String getHint() {
			StringBuilder sb = new StringBuilder();
			if(getFocus() != null) {
				sb.append("ENTER: Teleport\n");
			}
			sb.append("ESC: Quit");
			return sb.toString();
		}
		
		@Override
		public void keyTyped(char ch, int kid) {
			super.keyTyped(ch, kid);
			if(kid == Keyboard.KEY_RETURN) {
				Widget focus;
				if((focus = getFocus()) != null) {
					GList.ListElem le = (ListElem) focus;
					//Do the teleportation.
					AcademyCraft.netHandler.sendToServer(
						new TeleportMsg(le.data.dimension, le.data.x, le.data.y, le.data.z));
					player.closeScreen();
				}
			}
		}
	}
	
	@RegGuiHandler
	public static GuiHandlerBase guiCreateHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		protected GuiScreen getClientGui() {
			return new LIGuiScreen(new GuiCreate()).setDrawBack(false);
		}
	};
	
	@RegGuiHandler
	public static GuiHandlerBase guiSelectHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		protected GuiScreen getClientGui() {
			return new LIGuiScreen(new GuiSelect()).setDrawBack(false);
		}
	};
	
	@SubscribeEvent
	public void onStateChange(ControlStateEvent event) {
		
	}

}
