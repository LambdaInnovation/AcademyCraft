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
package cn.academy.ability;

import cn.academy.ability.block.AbilityInterferer;
import cn.academy.ability.block.BlockDeveloper;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.item.ItemDeveloper;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegBlock;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;
import cn.lambdalib.multiblock.ItemBlockMulti;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

/**
 * The ability module init class.
 * 
 * @author WeAthFolD
 */
@Registrant
@RegInit
@RegACRecipeNames
@RegEventHandler(Bus.Forge)
public class ModuleAbility {

	@RegItem
	@RegItem.HasRender
	@RecipeName("dev_portable")
	public static ItemDeveloper developerPortable;

	@RegBlock(item = ItemBlockMulti.class)
	@RecipeName("dev_normal")
	public static BlockDeveloper developerNormal = new BlockDeveloper(DeveloperType.NORMAL);

	@RegBlock(item = ItemBlockMulti.class)
	@RecipeName("dev_advanced")
	public static BlockDeveloper developerAdvanced = new BlockDeveloper(DeveloperType.ADVANCED);

    @RegBlock
    public static AbilityInterferer abilityInterferer;

	public static void init() {
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
		if (event.target != null && event.target.typeOfHit == MovingObjectType.BLOCK) {
			if (event.player.worldObj.getBlock(event.target.blockX, event.target.blockY,
					event.target.blockZ) instanceof BlockDeveloper)
				event.setCanceled(true);
		}
	}

}
