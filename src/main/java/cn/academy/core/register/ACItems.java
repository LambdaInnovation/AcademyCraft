package cn.academy.core.register;

import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.item.ItemVoid;
import cn.academy.misc.item.ACRecord;
import cn.academy.misc.item.ItemCapsule;
import cn.academy.misc.item.ItemCoin;
import cn.academy.misc.item.ItemEnergyCrystal;
import cn.academy.misc.item.ItemModuleAttached;
import cn.academy.misc.item.ItemNeedle;
import cn.academy.misc.item.ItemTablet;
import cn.liutils.api.util.RegUtils;
import cpw.mods.fml.common.registry.GameRegistry;

public class ACItems {
	
	//Misc
	public static Item 
		logo,
		record0,
		record1,
		record2,
		capsuleH,
		capsuleM,
		capsuleL,
		tabletH,
		tabletM,
		tabletL,
		needle,
		EnergyCrystal,
		adEnergyCard,
		enchItem,
		AluminumIngot,
		CopperIngot,
		SteelIngot,
		TinIngot,
		SiliconRod,
		CopperCoil,
		CoreBearing,
		ivoid;
	
	public static ItemCoin coin;
	
	public static void init(Configuration conf) {
		
		ivoid = RegUtils.reg(ItemVoid.class, "ac_void");
		
		//TODO: (Suggestion) Switch all simple registration to RegUtils.reg() with template arg
		logo = new Item().setUnlocalizedName("ac_logo").setTextureName("academy:logo");
		record0 = new ACRecord("ac1", 0).setUnlocalizedName("ac_record1");
		record1 = new ACRecord("ac2", 1).setUnlocalizedName("ac_record2");
		record2 = new ACRecord("ac3", 2).setUnlocalizedName("ac_record3");
		coin = new ItemCoin();
		capsuleH = new ItemCapsule(1).setUnlocalizedName("ability_capsule1");
		capsuleM = new ItemCapsule(2).setUnlocalizedName("ability_capsule2");
		capsuleL = new ItemCapsule(3).setUnlocalizedName("ability_capsule3");
		tabletH = new ItemTablet(1).setUnlocalizedName("ability_tablet1");
		tabletM = new ItemTablet(2).setUnlocalizedName("ability_tablet2");
		tabletL = new ItemTablet(3).setUnlocalizedName("ability_tablet3");
		needle = new ItemNeedle();
		EnergyCrystal = new ItemEnergyCrystal();
		adEnergyCard = new ItemModuleAttached();
//		enchItem = new EnchedItem();
		AluminumIngot = setItemBothUandT("aluminumingot");
		CopperIngot = setItemBothUandT("copperingot");
		SteelIngot = setItemBothUandT("steelingot");
		TinIngot = setItemBothUandT("tiningot");
		SiliconRod = setItemBothUandT("siliconrod");
		CopperCoil = setItemBothUandT("coppercoil");
		CoreBearing = setItemBothUandT("corebearing");
		
		GameRegistry.registerItem(logo, "ac_logo");
		GameRegistry.registerItem(record0, "ac_record1");
		GameRegistry.registerItem(record1, "ac_record2");
		GameRegistry.registerItem(record2, "ac_record3");
		GameRegistry.registerItem(coin, "ac_coin");
		GameRegistry.registerItem(capsuleH, "ability_capsule1");
		GameRegistry.registerItem(capsuleM, "ability_capsule2");
		GameRegistry.registerItem(capsuleL, "ability_capsule3");
		GameRegistry.registerItem(tabletH, "ability_tablet1");
		GameRegistry.registerItem(tabletM, "ability_tablet2");
		GameRegistry.registerItem(tabletL, "ability_tablet3");
		GameRegistry.registerItem(needle, "ac_needle");
		GameRegistry.registerItem(EnergyCrystal, "ac_energycrystal");
		GameRegistry.registerItem(adEnergyCard, "ac_card");
//		GameRegistry.registerItem(enchItem, "ac_enchitem");
		
		GameRegistry.registerItem(AluminumIngot,"ac_aluminumingot");
		GameRegistry.registerItem(CopperIngot,"ac_copperingot");
		GameRegistry.registerItem(SteelIngot,"ac_steelingot");
		GameRegistry.registerItem(TinIngot,"ac_tiningot");
		
		GameRegistry.registerItem(SiliconRod,"ac_siliconrod");
		GameRegistry.registerItem(CopperCoil,"ac_coppercoil");
		GameRegistry.registerItem(CoreBearing,"ac_corebearing");
		
		//矿物词典
		OreDictionary.registerOre("ingotCopper", CopperIngot);
		OreDictionary.registerOre("ingotAluminum", AluminumIngot);
		OreDictionary.registerOre("ingotRefinedIron", SteelIngot);
		OreDictionary.registerOre("ingotTin", TinIngot);
		
		//熔炉配方
/*		GameRegistry.addSmelting(ACBlocks.copperore, new ItemStack(ACItems.CopperIngot), 0.1f);
		GameRegistry.addSmelting(ACBlocks.tinore,new ItemStack(ACItems.TinIngot), 0.1f);
		GameRegistry.addSmelting(ACBlocks.aluminumore,new ItemStack(ACItems.AluminumIngot),0.1f);*/
	}
	
	private static Item setItemBothUandT(String ut){
		return new Item().setUnlocalizedName("ac_" + ut)
				.setTextureName("academy:" + ut)
				.setCreativeTab(AcademyCraftMod.cct);
	}
}
