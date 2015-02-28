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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.academy.ability.teleport.data.LocationData;
import cn.academy.ability.teleport.data.LocationData.Location;
import cn.academy.ability.teleport.skill.SkillLocatingTele.GuiCreate.GList.ListElem;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.event.ControlStateEvent;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.util.ACUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.Widget.AlignStyle;
import cn.liutils.api.gui.widget.FocusedVList;
import cn.liutils.api.gui.widget.InputBox;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class SkillLocatingTele extends SkillBase {
	
	static final double scale = 0.5;
	
	private final static ResourceLocation tex = new ResourceLocation("academy:textures/guis/tp_locating_ui.png");
	
	@RegAuxGui
	public static GuiSelect guiSelect = new GuiSelect();
	
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
		public void onFinish() {
			if(isOpeningGui()) { //Opening gui, don't do anything.
				return;
			}
			
			if(isRemote()) {
				if(this.getTickTime() < 10) {
					//Open teleport gui
					guiSelect.open();
				} else {
					//Open create gui
					guiCreateHandler.openClientGui();
				}
			}
		}

		@Override
		public void onHold() {}
		
	}
	
	private static boolean isOpeningGui() {
		return guiSelect.isOpen() || false;
	}

	/**
	 * Common subroutine of both GUIs.
	 */
	private static class BaseScreen {
		public long createTime;
		
		double[][] shadowOffsets = {
				{ 51, -32 },
				{ -60, -60 },
				{ 66, 30 },
				{ -30, 60 }
		};
		
		public void draw(double w, double h) {
			long dt = Minecraft.getSystemTime() - createTime;
			double mAlpha = Math.min(1.0, dt / 800.0);
			
			HudUtils.setTextureResolution(512, 512);
			RenderUtils.loadTexture(tex);
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glPushMatrix(); {
				GL11.glTranslated(w / 2, h / 2, 0);
				GL11.glScaled(scale, scale, 1);
				GL11.glTranslated(-413 / 2, -219 / 2, 0);
				
				GL11.glColor4d(1, 1, 1, mAlpha);
				double ratio = Math.min(1.0, dt / 600.0);
				for(double[] off : shadowOffsets) {
					drawOneShadow(ratio * off[0], ratio * off[1]);
				}
				drawMainWindow();
			} GL11.glPopMatrix();
			GL11.glDisable(GL11.GL_BLEND);
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
	
	public static class GuiSelect extends AuxGui {
		
		BaseScreen bs = new BaseScreen();
		
		public boolean open;

		@Override
		public boolean isOpen() {
			return open;
		}
		
		public void open() {
			bs.createTime = Minecraft.getSystemTime();
			open = true; 
		}
		
		public void close() {
			open = false;
		}

		@Override
		public void draw(ScaledResolution sr) {
			bs.draw(sr.getScaledWidth_double(), sr.getScaledHeight_double());
		}
		
	}
	
	public static class GuiCreate extends LIGuiScreen {
		
		BaseScreen bs = new BaseScreen();
		Widget mainScreen;
		
		InputBox box;
		static final int[] color = { 178, 178, 178, 180 };
		
		int targDim;
		float targX, targY, targZ;
		
		EntityPlayer player;
		LocationData data;
		
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
					if(getFocusedElement() == this) {
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
		
		public GuiCreate() {
			init();
		}
		
		void init() {
			player = Minecraft.getMinecraft().thePlayer;
			targX = (float) player.posX;
			targY = (float) player.posY;
			targZ = (float) player.posZ;
			targDim = player.worldObj.provider.dimensionId;
			data = LocationData.get(player);
			
			bs.createTime = Minecraft.getSystemTime();
			this.drawBack = false;
			
			mainScreen = new Widget() {
				@Override
				public void draw(double mx, double my, boolean hov) {
					StringBuilder sb = new StringBuilder();
					if(isCreating()) {
						sb.append(String.format("x %.1f\ny %.1f\nz %.1f\n", targX, targY, targZ));
						sb.append("ENTER: add\n");
					}
					if(isRemoving()) {
						ListElem le = (ListElem) gui.getFocus();
						sb.append(String.format("x %.1f\ny %.1f\nz %.1f\n", le.data.x, le.data.y, le.data.z));
						sb.append("DEL: remove\n");
					}
					sb.append("ESC: quit\n");
					
					RenderUtils.bindColor(220, 220, 220, 200);
					ACUtils.drawText(sb.toString(), 45, 48, 15);
				}
			};
			mainScreen.setSize(413, 219);
			mainScreen.alignStyle = AlignStyle.CENTER;
			mainScreen.scale = scale;
			gui.addWidget(mainScreen);
			
			//sub widgets
			mainScreen.addWidget(new GList());
			mainScreen.addWidget(box = new InputBox(42, 148, 127, 25, 20, 1, 15));
			box.setTextColor(255, 255, 255, 180);
			box.setFont(ACClientProps.FONT_YAHEI_32);
		}
		
		@Override
	    public void drawScreen(int mx, int my, float w) {
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			bs.draw(width, height);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			super.drawScreen(mx, my, w);
		}
		
		private boolean isCreating() {
			return box.isFocused();
		}
		
		private boolean isRemoving() {
			Widget focus = this.gui.getFocus();
			return focus != null && focus != box;
		}
		
		@Override
		public void keyTyped(char ch, int kid) {
			super.keyTyped(ch, kid);
			if(isRemoving()) {
				if(Keyboard.KEY_DELETE == kid) {
					//confirm the del
					data.clientRemove(((ListElem)gui.getFocus()).n);
					
					//Re-construct elements brutely.
					gui = new LIGui();
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
	}
	
	@RegGuiHandler
	public static GuiHandlerBase guiCreateHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		protected GuiScreen getClientGui() {
			return new GuiCreate();
		}
	};
	
	@SubscribeEvent
	public void onStateChange(ControlStateEvent event) {
		if(guiSelect.isOpen()) {
			guiSelect.close();
		}
	}

}
