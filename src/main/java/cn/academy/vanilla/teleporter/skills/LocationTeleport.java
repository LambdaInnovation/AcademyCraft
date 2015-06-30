/**
 * 
 */
package cn.academy.vanilla.teleporter.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.client.ui.CPBar;
import cn.academy.ability.client.ui.CPBar.IConsumptionHintProvider;
import cn.academy.vanilla.teleporter.client.LocTeleportUI;
import cn.academy.vanilla.teleporter.data.LocTeleData.Location;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Location teleport. This is the skill plus the synchronization&logic needed for the UI.
 * @author WeAthFolD
 */
@Registrant
public class LocationTeleport extends Skill {
	
	static LocationTeleport instance;

	public LocationTeleport() {
		super("location_teleport", 3);
		instance = this;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant() {
			@Override
			public void execute() {
				LocTeleportUI.handler.openClientGui();
				
				CPBar.setHintProvider(new IConsumptionHintProvider() {

					@Override
					public boolean alive() {
						return locate() != null;
					}

					@Override
					public float getConsumption() {
						LocTeleportUI ui = locate();
						if(ui == null || ui.selection == null) return 0;
						return LocationTeleport.getConsumption(player, ui.selection);
					}
					
					private LocTeleportUI locate() {
						GuiScreen screen = Minecraft.getMinecraft().currentScreen;
						return screen instanceof LocTeleportUI ? ((LocTeleportUI)screen) : null;
					}
					
				});
			}
		};
	}
	
	//NOTE: Currently all of the methods doesn't check player category.
	// Might this cause issues? 
	
	/**
	 * Determine whether player can record the current location.
	 */
	public static boolean canRecord(EntityPlayer player) {
		AbilityData aData = AbilityData.get(player);
		return player.worldObj.provider.dimensionId == 0 || aData.getSkillExp(instance) >= 0.3f;
	}
	
	public static Location toLocation(EntityPlayer player, String name) {
		return new Location(name, player.worldObj.provider.dimensionId, 
				(float)player.posX, (float)player.posY, (float)player.posZ);
	}
	
	public static float getConsumption(EntityPlayer player, Location dest) {
		AbilityData data = AbilityData.get(player);
		double distance = player.getDistance(dest.x, dest.y, dest.z);
		double dimPenalty = player.worldObj.provider.dimensionId != dest.dimension ? 2 : 1;
		return instance.getFunc("consumption").callFloat(distance, dimPenalty, data.getSkillExp(instance));
	}
	
	public static float getOverload(EntityPlayer player) {
		return 20f;
	}
	
	public static boolean canPerform(EntityPlayer player, Location dest) {
		CPData cpData = CPData.get(player);
		return cpData.getCP() >= getConsumption(player, dest);
	}
	
	@SideOnly(Side.CLIENT)
	public static void performAction(EntityPlayer player, Location dest) {
		performAtServer(player, dest);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	private static void performAtServer(@Instance EntityPlayer player, @Data Location dest) {
		CPData cpData = CPData.get(player);
		if(cpData.perform(getOverload(player), getConsumption(player, dest))) {
			if(player.worldObj.provider.dimensionId != dest.dimension) {
				player.travelToDimension(dest.dimension);
			}
			
			player.setPositionAndUpdate(dest.x, dest.y, dest.z);
		}
	}

}
