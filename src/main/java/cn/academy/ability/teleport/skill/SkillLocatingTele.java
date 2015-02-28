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
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.event.ControlStateEvent;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class SkillLocatingTele extends SkillBase {
	
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
			double scale = 0.5;
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
		
		public GuiCreate() {
			bs.createTime = Minecraft.getSystemTime();
		}
		
		@Override
	    public void drawScreen(int mx, int my, float w) {
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			bs.draw(width, height);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
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
