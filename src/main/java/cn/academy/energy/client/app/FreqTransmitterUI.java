/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.client.app;

import javax.vecmath.Vector2d;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.util.ControlOverrider;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.core.event.eventhandler.LIFMLGameEventDispatcher;
import cn.liutils.core.event.eventhandler.LIHandler;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.KeyManager;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent;

/**
 * @author WeAthFolD
 */
public class FreqTransmitterUI extends AuxGui {
	
	private interface State {
		void handleDraw(double w, double h);
		void handleClicking(MovingObjectPosition result);
		boolean handlesKeyInput();
		void handleKeyInput(char ch, int kid);
	}
	
	private static final Color
		BG_COLOR = new Color().setColor4i(58, 77, 83, 100),
		GLOW_COLOR = new Color().setColor4i(0, 255, 251, 130);
	
	private static final double GLOW_SIZE = 2;
	
	EntityPlayer player;
	World world;
	
	State current;
	
	KeyEventDispatcher keyDispatcher;
	
	public FreqTransmitterUI() {
		player = Minecraft.getMinecraft().thePlayer;
		world = player.worldObj;
		
		LIFMLGameEventDispatcher.INSTANCE.registerKeyInput(keyDispatcher = new KeyEventDispatcher());
		LIFMLGameEventDispatcher.INSTANCE.registerMouseInput(keyDispatcher);
		
		setState(new StateStart());
	}
	
	private void setState(State next) {
		if(next == null) {
			this.dispose();
			
			if(current.handlesKeyInput()) {
				ControlOverrider.endCompleteOverride();
			}
		} else {
			if(next.handlesKeyInput()) {
				ControlOverrider.startCompleteOverride();
			}
		}
		current = next;
	}
	
	private String local(String key) {
		return StatCollector.translateToLocal("ac.app.freq_transmitter." + key);
	}
	
	@Override
	public void onDisposed() {
		keyDispatcher.setDead();
	}

	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		double width = sr.getScaledWidth_double(), height = sr.getScaledHeight_double();
		
		AppFreqTransmitter app = AppFreqTransmitter.instance;
		GL11.glPushMatrix(); {
			
			GL11.glTranslated(15, 15, 0);
			
			final float isize = 18;
			final float fsize = 10;
			String str = app.getDisplayName();
			double len = Font.font.strLen(str, fsize);
			
			drawBox(0, 0, 30 + len, 18);
			
			ResourceLocation icon = app.getIcon();
			RenderUtils.loadTexture(icon);
			GL11.glColor4d(1, 1, 1, 1);
			HudUtils.rect(2, 0, isize, isize);
			
			Font.font.draw(str, isize + 6, 4, fsize, 0xffffff);
		
		} GL11.glPopMatrix();
		
		current.handleDraw(width, height);
		
		GL11.glColor4d(1, 1, 1, 1);
	}
	
	private static void drawBox(double x, double y, double width, double height) {
		BG_COLOR.bind();
		HudUtils.colorRect(x, y, width, height);
		
		ACRenderingHelper.drawGlow(x, y, width, height, GLOW_SIZE, GLOW_COLOR);
	}
	
	private class KeyEventDispatcher extends LIHandler<InputEvent> {

		@Override
		protected boolean onEvent(InputEvent event) {
			if(current != null) {
				if(event instanceof MouseInputEvent) {
					int mid = Mouse.getEventButton();
					if(mid == 1 && Mouse.getEventButtonState()) {
						System.out.println("Handle clicking");
						current.handleClicking(Raytrace.traceLiving(player, 4, EntitySelectors.nothing));
					}
				} else {
					if(Keyboard.getEventKeyState()) {
						if(current.handlesKeyInput())
							current.handleKeyInput(Keyboard.getEventCharacter(), Keyboard.getEventKey());
					}
				}
			} else {
				System.err.println("Human is dead. Mismatch.");
				this.setDead();
			}
			return true;
		}
		
	}
	
	// S0
	private class StateStart implements State {

		@Override
		public void handleDraw(double w, double h) {
			String str = local("s0_0");
			final double trimLength = 120, size = 10;
			Vector2d vec = Font.font.simDrawWrapped(str, size, trimLength);
			final double X0 = w / 2 + 10, Y0 = h / 2 + 10, MARGIN = 5;
			
			drawBox(X0, Y0, MARGIN * 2 + vec.x + 15, MARGIN * 2 + vec.y);
			Font.font.drawWrapped(str, X0 + MARGIN, Y0 + MARGIN, size, 0xffffff, trimLength);
		}

		@Override
		public void handleClicking(MovingObjectPosition result) {
			if(result == null) {
				setState(null);
				return;
			}
			int hx = result.blockX,
					hy = result.blockY,
					hz = result.blockZ;
			TileEntity te = world.getTileEntity(hx, hy, hz);
			if(te instanceof IWirelessNode) {
				System.out.println("Linking node");
			} else if(te instanceof IWirelessMatrix) {
				System.out.println("Linking matrix");
			} else {
				setState(null);
			}
		}

		@Override
		public void handleKeyInput(char ch, int kid) {
			// TODO Auto-generated method stub
			System.out.println("KeyInput " + KeyManager.getKeyName(kid));
		}

		@Override
		public boolean handlesKeyInput() {
			return true;
		}
		
	}

}
