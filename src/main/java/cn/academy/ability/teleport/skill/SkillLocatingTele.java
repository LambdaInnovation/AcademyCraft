/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.teleport.skill;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.teleport.CatTeleport;
import cn.academy.ability.teleport.client.gui.LocatingGuiBase;
import cn.academy.ability.teleport.client.gui.LocatingGuiBase.GList;
import cn.academy.ability.teleport.client.gui.LocatingGuiBase.GList.ListElem;
import cn.academy.ability.teleport.data.LocationData.Location;
import cn.academy.ability.teleport.msg.LocTeleMsg;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACLangs;
import cn.academy.core.event.ClientEvents;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.InputBox;
import cn.liutils.util.ClientUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * TODO:This skill needs localizing.
 * @author WeathFolD
 */
@RegistrationClass
public class SkillLocatingTele extends SkillBase {
	
	public static final double scale = 0.4;
	
	public final static ResourceLocation 
		tex = new ResourceLocation("academy:textures/guis/tp_locating_ui.png"),
		tex2 = new ResourceLocation("academy:textures/guis/tp_locating_ui2.png");
		
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
			
		}.setCooldown(0));
	}
	
	public static final float getConsumption(int slv, int lv, double dist, boolean difdimm) {
		dist = Math.min(800, dist);
		return (float) ((difdimm ? 2 : 1) * 
				Math.max(8, Math.sqrt(Math.min(dist, 800))) * 
				(200 - slv *10 + 5 * (lv * lv)));
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
	
	public static class GuiCreate extends LocatingGuiBase {
		
		InputBox box;
		
		int targDim;
		float targX, targY, targZ;
		
		public GuiCreate() {
			super(ACLangs.tpLocatingCreate(), tex, new int[] { 220, 220, 220, 200 });
			init();
		}
		
		protected void init() {
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
		public String getHint() {
			StringBuilder sb = new StringBuilder();
			if(isCreating()) {
				 sb.append(String.format("x %.1f\ny %.1f\nz %.1f\n", targX, targY, targZ));
				 sb.append(data.getLocCount() == 5 ? ACLangs.tpLocatingMax() + "\n" : 
					 "ENTER: " + ACLangs.tpLocatingAdd() + "\n");
			}
			if(isRemoving()) {
				GList.ListElem le = (GList.ListElem) getFocus();
				sb.append(String.format("x %.1f\ny %.1f\nz %.1f\n", le.data.x, le.data.y, le.data.z));
				sb.append("DEL: " + ACLangs.tpLocatingRemove() + "\n");
			}
			sb.append("ESC: " + ACLangs.tpLocatingQuit() + "\n");
			return sb.toString();
		}
	}
	
	public static class GuiSelect extends LocatingGuiBase {
		
		public GuiSelect() {
			super(ACLangs.tpLocatingSelect(), tex2, new int[] { 40, 40, 40, 200 });
			init();
		}
		
		@Override
		protected void init() {
			super.init();
			
		}

		@Override
		public String getHint() {
			StringBuilder sb = new StringBuilder();
			if(getFocus() != null) {
				sb.append("ENTER: " + ACLangs.tpLocatingTeleport() + "\n");
			}
			sb.append("ESC: " + ACLangs.tpLocatingQuit());
			return sb.toString();
		}
		
		@Override
		public void keyTyped(char ch, int kid) {
			super.keyTyped(ch, kid);
			if(data.getLocCount() < 5 && kid == Keyboard.KEY_RETURN) {
				Widget focus;
				if((focus = getFocus()) != null) {
					GList.ListElem le = (ListElem) focus;
					//Do the teleportation.
					//Validate in client. ignore the small CP variation. Maybe further changed to sync in server.
					double dist = Math.sqrt(player.getDistance(le.data.x, le.data.y, le.data.z));
					boolean diffdimm = player.worldObj.provider.dimensionId != le.data.dimension;
					AbilityData data = AbilityDataMain.getData(player);
					int slv = data.getSkillLevel(CatTeleport.skillLocatingTele), lv = data.getLevelID() + 1;
					float cp = getConsumption(slv, lv, dist, diffdimm);
					if(!data.decreaseCP(cp, CatTeleport.skillLocatingTele)) {
						//failed.
						ClientUtils.playSound(ClientEvents.abortSound, 1);
						player.closeScreen();
						return;
					}
					
					AcademyCraft.netHandler.sendToServer(
						new LocTeleMsg(le.data.dimension, le.data.x, le.data.y, le.data.z, cp));
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

}
