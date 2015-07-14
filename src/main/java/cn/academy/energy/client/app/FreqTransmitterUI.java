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

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatAllowedCharacters;
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
import cn.academy.energy.api.block.IWirelessUser;
import cn.annoreg.mc.network.Future;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.core.event.eventhandler.LIFMLGameEventDispatcher;
import cn.liutils.core.event.eventhandler.LIHandler;
import cn.liutils.template.block.BlockMulti;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.GameTimer;
import cn.liutils.util.helper.KeyManager;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent;

/**
 * @author WeAthFolD
 */
public class FreqTransmitterUI extends AuxGui {
	
	private abstract class State {
		
		boolean handlesKey;
		final long createTime;
		
		public State(boolean _handlesKey) {
			handlesKey = _handlesKey;
			createTime = GameTimer.getTime();
		}
		
		final boolean handlesKeyInput() {
			return handlesKey;
		}
		
		abstract void handleDraw(double w, double h);
		abstract void handleClicking(MovingObjectPosition result);
		
		void handleKeyInput(char ch, int kid) {}
		
		final long getDeltaTime() {
			return GameTimer.getTime() - createTime;
		}
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
		
		ControlOverrider.override(KeyManager.MOUSE_LEFT);
		ControlOverrider.override(KeyManager.MOUSE_RIGHT);
	}
	
	@Override
	public boolean isConsistent() {
		return false;
	}
	
	private void setState(State next) {
		if(next == null) {
			this.dispose();
			
			if(current.handlesKeyInput()) {
				ControlOverrider.endCompleteOverride();
			}
		} else {
			if(current != null && current.handlesKeyInput()) {
				ControlOverrider.endCompleteOverride();
			}
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
		
		ControlOverrider.removeOverride(KeyManager.MOUSE_LEFT);
		ControlOverrider.removeOverride(KeyManager.MOUSE_RIGHT);
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
	 
	private static void drawTextBox(String str, double x, double y) {
		final double trimLength = 120, size = 10;
		Vector2d vec = Font.font.simDrawWrapped(str, size, trimLength);
		final double X0 = x, Y0 = y, MARGIN = 5;
		
		drawBox(X0, Y0, MARGIN * 2 + vec.x + 25, MARGIN * 2 + vec.y);
		Font.font.drawWrapped(str, X0 + MARGIN, Y0 + MARGIN, size, 0xffffff, trimLength);
	}
	
	private class KeyEventDispatcher extends LIHandler<InputEvent> {

		@Override
		protected boolean onEvent(InputEvent event) {
			if(current != null) {
				if(event instanceof MouseInputEvent) {
					int mid = Mouse.getEventButton();
					if(mid == 1 && Mouse.getEventButtonState()) {
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
	
	// STATES
	
	private class StateNotify extends State {
		
		final String key;
		
		public StateNotify(String _key) {
			super(false);
			key = _key;
		}
		
		@Override
		public void handleDraw(double w, double h) {
			drawTextBox(local(key), w / 2 + 10, h / 2 + 10);
		}

		@Override
		void handleClicking(MovingObjectPosition result) {}
		
	}
	
	private class StateNotifyAndQuit extends StateNotify {
		
		public StateNotifyAndQuit(String _key) {
			super(_key);
		}
		
		@Override
		public void handleDraw(double w, double h) {
			super.handleDraw(w, h);
			if(this.getDeltaTime() > 2500L) {
				dispose();
			}
		}
		
	}
	
	private class StateNotifyAndReturn extends StateNotify {
		final State toSwitch;
		
		public StateNotifyAndReturn(String _key, State _toSwitch) {
			super(_key);
			toSwitch = _toSwitch;
		}
		
		@Override
		public void handleDraw(double w, double h) {
			super.handleDraw(w, h);
			if(this.getDeltaTime() > 2500L) {
				setState(toSwitch);
			}
		}
	}
	
	// S0
	private class StateStart extends State {
		
		boolean started = false;
		
		public StateStart() {
			super(false);
		}

		@Override
		public void handleDraw(double w, double h) {
			drawTextBox(local("s0_0"), w / 2 + 10, h / 2 + 10);
		}

		@Override
		public void handleClicking(MovingObjectPosition result) {
			if(result == null) {
				setState(null);
				return;
			}
			if(started)
				return;
			int hx = result.blockX,
					hy = result.blockY,
					hz = result.blockZ;
			TileEntity te = world.getTileEntity(hx, hy, hz);
			if(te instanceof IWirelessNode) {
				
				setState(new StateDoNodeLink((IWirelessNode) te));
				
			} else if(te instanceof IWirelessMatrix) {
				
				started = true;
				IWirelessMatrix mat = (IWirelessMatrix) te;
				// BAD CODING STYLE: Hard coded BlockMulti processing
				Block block = world.getBlock(hx, hy, hz);
				if(block instanceof BlockMulti) {
					mat = (IWirelessMatrix) ((BlockMulti)block).getOriginTile(te);
				}
				
				final IWirelessMatrix mat2 = mat;
				Syncs.querySSID(mat, Future.create((Object o) -> {
					if(current == StateStart.this) {
						if(o == null) {
							setState(new StateNotifyAndQuit("e0"));
						} else {
							setState(new StateAuthorize(mat2, (String) o));
						}
					}
				}));
			} else {
				setState(new StateNotifyAndQuit("e4"));
			}
		}
		
	}
	
	// S1
	private class StateAuthorize extends State {
		
		final IWirelessMatrix matrix;
		final String ssid;
		String pass = "";

		public StateAuthorize(IWirelessMatrix _matrix, String _ssid) {
			super(true);
			matrix = _matrix;
			ssid = _ssid;
		}

		@Override
		void handleDraw(double w, double h) {
			GL11.glPushMatrix();
			GL11.glTranslated(w / 2 + 10, h / 2 - 10, 0);
			
			drawBox(0, 0, 140, 40);
			
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < pass.length(); ++i)
				sb.append('*');
			
			Font.font.draw(String.format("SSID: %s", "test"), 10, 5, 10, 0xbfbfbf);
			Font.font.draw(String.format("PASS: %s", sb.toString()), 10, 15, 10, 0xffffff);
			Font.font.draw(local("s1_0"), 10, 25, 10, 0x30ffff);
			GL11.glPopMatrix();
		}

		@Override
		void handleClicking(MovingObjectPosition result) {
			// NO-OP
		}
		
		@Override
		void handleKeyInput(char ch, int kid) {
			if(ChatAllowedCharacters.isAllowedCharacter(ch)) {
				pass = pass + ch;
			} else if(kid == Keyboard.KEY_RETURN) {
				State state = new StateNotify("s1_1");
				setState(state);
				Syncs.authorizeMatrix(matrix, pass, Future.create((Object o) -> {
					if(state == FreqTransmitterUI.this.current) {
						boolean result = (boolean) o;
						if(result) {
							setState(new StateDoMatrixLink(matrix, pass));
						} else {
							setState(new StateNotifyAndQuit("e1"));
						}
					}
				}));
			} else if(kid == Keyboard.KEY_BACK) {
				if(pass.length() > 0)
					pass = pass.substring(0, pass.length() - 1);
			}
		}
		
	}
	
	//S2
	private class StateDoMatrixLink extends State {

		final IWirelessMatrix matrix;
		final String pass;
		
		public StateDoMatrixLink(IWirelessMatrix _matrix, String _pass) {
			super(false);
			matrix = _matrix;
			pass = _pass;
		}

		@Override
		void handleDraw(double w, double h) {
			drawTextBox(local("s2_0"), w / 2 + 10, h / 2 + 10);
		}

		@Override
		void handleClicking(MovingObjectPosition result) {
			TileEntity tile;
			
			if(result == null || 
				!((tile = world.getTileEntity(result.blockX, result.blockY, result.blockZ)) instanceof IWirelessNode)) {
				setState(new StateNotifyAndQuit("e4"));
			} else {
				IWirelessNode node = (IWirelessNode) tile;
				State state = new StateNotify("e5");
				setState(state);
				Syncs.linkNodeToMatrix(node, matrix, pass, Future.create((Object o) -> {
					if(FreqTransmitterUI.this.current == state) {
						boolean res = (boolean) o;
						if(res) {
							System.out.println("Link successful!");
							setState(new StateNotifyAndReturn("e6", StateDoMatrixLink.this));
						} else {
							setState(new StateNotifyAndQuit("e2"));
						}
					}
				}));
			}
		}
		
	}
	
	//S3
	private class StateDoNodeLink extends State {
		
		IWirelessNode node;

		public StateDoNodeLink(IWirelessNode _node) {
			super(false);
			node = _node;
		}

		@Override
		void handleDraw(double w, double h) {
			drawTextBox(local("s3_0"), w / 2 + 10, h / 2 + 10);
		}

		@Override
		void handleClicking(MovingObjectPosition r) {
			if(r == null) {
				setState(new StateNotifyAndQuit("e4"));
				return;
			}
			
			TileEntity tile = world.getTileEntity(r.blockX, r.blockY, r.blockZ);
			
			if(tile instanceof IWirelessUser) {
				State state = new StateNotify("e5");
				setState(state);
				Syncs.linkUserToNode((IWirelessUser) tile, node, Future.create((Object o) -> {
					if(FreqTransmitterUI.this.current == state) {
						boolean res = (boolean) o;
						if(res) {
							setState(new StateNotifyAndReturn("e6", StateDoNodeLink.this));
						} else {
							setState(new StateNotifyAndQuit("e3"));
						}
					}
				}));
			} else {
				setState(new StateNotifyAndQuit("e4"));
			}
		}
		
	}

}
