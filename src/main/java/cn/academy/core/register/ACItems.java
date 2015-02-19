package cn.academy.core.register;

import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.render.RenderVoid;
import cn.academy.core.item.ItemVoid;
import cn.academy.energy.item.ItemEnergyCrystal;
import cn.academy.energy.item.ItemFreqRegulator;
import cn.academy.misc.client.render.RendererCoin;
import cn.academy.misc.item.ACSimpleItem;
import cn.academy.misc.item.ACRecord;
import cn.academy.misc.item.ItemCapsule;
import cn.academy.misc.item.ItemCoin;
import cn.academy.misc.item.ItemExpNail;
import cn.academy.misc.item.ItemModuleAttached;
import cn.academy.misc.item.ItemNeedle;
import cn.academy.misc.item.ItemTablet;
import cn.annoreg.core.RegWithName;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class ACItems {
	
	@RegItem
	@RegItem.UTName("logo")
	public static Item logo;
	
	@RegItem
	public static ACRecord record0 = new ACRecord(0);
	@RegItem
	public static ACRecord record1 = new ACRecord(1);
	@RegItem
	public static ACRecord record2 = new ACRecord(2);
	
	@RegItem
	@RegItem.HasRender
	public static ItemCoin coin = new ItemCoin() {
		@RegItem.Render
		@SideOnly(Side.CLIENT)
		public RendererCoin.ItemRender renderCoin;
	};

	//TODO: Reconfigure the medicine system.
/*	@RegItem
	public static ItemCapsule capsuleA = new ItemCapsule(1);
	@RegItem
	public static ItemCapsule capsuleM = new ItemCapsule(2);
	@RegItem
	public static ItemCapsule capsuleL = new ItemCapsule(3);

	@RegItem
	public static ItemTablet tabletA = new ItemTablet(1);
	@RegItem
	public static ItemTablet tabletM = new ItemTablet(2);
	@RegItem
	public static ItemTablet tabletL = new ItemTablet(3);*/
	
	@RegItem
	public static ItemNeedle needle;

	@RegItem
	public static ItemEnergyCrystal energyCrystal;
	
	@RegItem
	public static ItemModuleAttached adEnergyCard;
	
	@RegItem
	@RegItem.UTName("al_ingot")
	@RegItem.OreDict("ingotAl")
	public static ACSimpleItem ingotAl;
	
	@RegItem
	@RegItem.UTName("cu_ingot")
	@RegItem.OreDict("ingotCu")
	public static ACSimpleItem ingotCu;
	
	@RegItem
	@RegItem.UTName("steelingot")
	@RegItem.OreDict("ingotRefinedIron")
	public static ACSimpleItem ingotSteel;
	
	@RegItem
	@RegItem.UTName("tiningot")
	@RegItem.OreDict("ingotTin")
	public static ACSimpleItem ingotTin;
	
	@RegItem
	@RegItem.UTName("mg_ingot")
	@RegItem.OreDict("ingotMg")
	public static ACSimpleItem ingotMg;
	
	@RegItem
	@RegItem.UTName("ni_ingot")
	@RegItem.OreDict("ingotNi")
	public static ACSimpleItem ingotNi;
	
	@RegItem
	@RegItem.UTName("ac_crystal")
	@RegItem.OreDict("crystal")
	public static ACSimpleItem crystal;
	
	@RegItem
	@RegItem.UTName("shadow_ingot")
	@RegItem.OreDict("ingotShadow")
	public static ACSimpleItem ingotShadow;
	
	@RegItem
	@RegItem.UTName("mg_plate")
	public static ACSimpleItem mgPlate;
	
	@RegItem
	@RegItem.UTName("al_plate")
	public static ACSimpleItem alPlate;
	
	@RegItem
	@RegItem.UTName("almg_plate")
	public static ACSimpleItem almgPlate;
	
	@RegItem
	@RegItem.UTName("ac_siliconrod")
	public static ACSimpleItem siliconRod;
	
	@RegItem
	@RegItem.UTName("ac_smallsi")
	public static ACSimpleItem smallSi;
	
	//TODO: This is not available in the beta version, remove the annotation when in a release version.
//	@RegItem
//	@RegItem.UTName("iron_dust")
//	public static ACSimpleItem ironDust;

	@RegItem
	@RegItem.UTName("ac_coppercoil")
	public static ACSimpleItem copperCoil;
	
	//TODO: If these items is unnecessary, delete them.
//	@RegItem
//	@RegItem.UTName("corebearing")
//	public static ACSimpleItem coreBearing;
	
	@RegItem
	@RegItem.UTName("ac_bodydet")
	public static ACSimpleItem bodyDet;
	
	@RegItem
	@RegItem.UTName("ac_io")
	public static ACSimpleItem ioPort;
	
	//TODO: This is not available in the beta version, remove the annotation when in a release version.
//	@RegItem
//	@RegItem.UTName("brainalpha")
//	public static ACSimpleItem brainAlpha;
	
	@RegItem
	@RegItem.UTName("ac_brainbeta")
	public static ACSimpleItem brainBeta;
	
	@RegItem
	@RegItem.UTName("ac_pcbn")
	public static ACSimpleItem pcb;
	
	@RegItem
	@RegItem.UTName("ac_cplank")
	public static ACSimpleItem compPlank;
	
	@RegItem
	@RegItem.UTName("ac_exp_nail")
	public static ItemExpNail expNail;
	
	@RegItem()
	@RegItem.HasRender
	@RegWithName("ac_void")
	public static ItemVoid ivoid = new ItemVoid() {
		@SideOnly(Side.CLIENT)
		@RegItem.Render
		public RenderVoid renderVoid;
	};
	
	@RegItem
	public static ItemFreqRegulator freqReg;
	
}
