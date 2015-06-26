package cn.academy.vanilla;

import net.minecraft.potion.Potion;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.academy.core.registry.RegACKeyHandler;
import cn.academy.vanilla.electromaster.CatElectroMaster;
import cn.academy.vanilla.electromaster.item.ItemCoin;
import cn.academy.vanilla.meltdowner.CatMeltdowner;
import cn.academy.vanilla.meltdowner.item.ItemSilbarn;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;
import cn.liutils.util.helper.KeyHandler;

@Registrant
@RegInit
public class ModuleVanilla {
	
	@RegItem
	@RegItem.HasRender
	public static ItemCoin coin;
	
	@RegItem
	@RegItem.HasRender
	public static ItemSilbarn silbarn;
	
	@RegCategory
	public static CatElectroMaster electroMaster;
	
	@RegCategory
	public static CatMeltdowner meltdowner;

	public static void init() {}
	
}
