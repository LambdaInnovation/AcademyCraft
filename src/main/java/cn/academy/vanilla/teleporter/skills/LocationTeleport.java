/**
 * 
 */
package cn.academy.vanilla.teleporter.skills;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.client.ui.CPBar;
import cn.academy.ability.client.ui.CPBar.IConsumptionHintProvider;
import cn.academy.misc.achievements.ModuleAchievements;
import cn.academy.vanilla.teleporter.client.LocTeleportUI;
import cn.academy.vanilla.teleporter.data.LocTeleData.Location;
import cn.academy.vanilla.teleporter.util.TPAttackHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Location teleport. This is the skill plus the synchronization&logic needed
 * for the UI.
 * 
 * @author WeAthFolD
 */
@Registrant
public class LocationTeleport extends Skill {

	public static final LocationTeleport instance = new LocationTeleport();
	static IEntitySelector basicSelector = EntitySelectors.and(EntitySelectors.living, new IEntitySelector() {

		@Override
		public boolean isEntityApplicable(Entity entity) {
			return entity.width * entity.width * entity.height < 80f;
		}

	});

	private LocationTeleport() {
		super("location_teleport", 3);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant() {
			@Override
			@SideOnly(Side.CLIENT)
			public void execute() {
				Minecraft.getMinecraft().displayGuiScreen(new LocTeleportUI());

				CPBar.setHintProvider(new IConsumptionHintProvider() {

					@Override
					public boolean alive() {
						return locate() != null;
					}

					@Override
					public float getConsumption() {
						LocTeleportUI ui = locate();
						if (ui == null || ui.selection == null)
							return 0;
						return LocationTeleport.getConsumption(player, ui.selection);
					}

					private LocTeleportUI locate() {
						GuiScreen screen = Minecraft.getMinecraft().currentScreen;
						return screen instanceof LocTeleportUI ? ((LocTeleportUI) screen) : null;
					}

				});
			}
		};
	}

	/**
	 * Determine whether player can record the current location.
	 */
	public static boolean canRecord(EntityPlayer player) {
		AbilityData aData = AbilityData.get(player);
		return player.worldObj.provider.dimensionId == 0 || aData.getSkillExp(instance) >= 0.3f;
	}

	public static Location toLocation(EntityPlayer player, String name) {
		return new Location(name, player.worldObj.provider.dimensionId, (float) player.posX, (float) player.posY,
				(float) player.posZ);
	}

	public static float getConsumption(EntityPlayer player, Location dest) {
		AbilityData data = AbilityData.get(player);
		double distance = player.getDistance(dest.x, dest.y, dest.z);
		double dimPenalty = player.worldObj.provider.dimensionId != dest.dimension ? 2 : 1;
		return instance.getFunc("consumption").callFloat(distance, dimPenalty, data.getSkillExp(instance));
	}

	public static float getOverload(EntityPlayer player) {
		return instance.getOverload(AbilityData.get(player));
	}

	public static boolean canPerform(EntityPlayer player, Location dest) {
		CPData cpData = CPData.get(player);
		return cpData.getCP() >= getConsumption(player, dest);
	}

	private static float getRange() {
		return instance.getFloat("range");
	}

	@SideOnly(Side.CLIENT)
	public static void performAction(EntityPlayer player, Location dest) {
		performAtServer(player, dest);
	}

	@RegNetworkCall(side = Side.SERVER)
	private static void performAtServer(@Instance EntityPlayer player, @Data Location dest) {
		AbilityData aData = AbilityData.get(player);
		CPData cpData = CPData.get(player);

		if (cpData.perform(getOverload(player), getConsumption(player, dest))) {
			List<Entity> entitiesToTeleport = WorldUtils.getEntities(player, 5,
					EntitySelectors.and(EntitySelectors.excludeOf(player), basicSelector));
			entitiesToTeleport = entitiesToTeleport.subList(0, Math.min(4, entitiesToTeleport.size()));

			if (player.worldObj.provider.dimensionId != dest.dimension) {
				player.travelToDimension(dest.dimension);
				for (Entity e : entitiesToTeleport) {
					e.travelToDimension(dest.dimension);
				}
			}

			for (Entity e : entitiesToTeleport) {
				double dx = e.posX - player.posX, dy = e.posY - player.posY, dz = e.posZ - player.posZ;
				e.setPosition(dest.x + dx, dest.y + dy, dest.z + dz);
			}
			player.setPositionAndUpdate(dest.x, dest.y, dest.z);

			double dist = player.getDistance(dest.x, dest.y, dest.z);
			aData.addSkillExp(instance, instance.getFunc("expincr").callFloat(dist));
			ModuleAchievements.trigger(player, "teleporter.ignore_barrier");
			TPAttackHelper.incrTPCount(player);
		}
	}

}
