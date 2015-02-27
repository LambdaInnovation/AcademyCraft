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
package cn.academy.core.register;

import net.minecraft.item.Item;
import cn.academy.core.client.render.RenderVoid;
import cn.academy.core.item.ItemVoid;
import cn.academy.energy.item.ItemEnergyCrystal;
import cn.academy.energy.item.ItemFreqRegulator;
import cn.academy.misc.client.render.RendererCoin;
import cn.academy.misc.item.ACRecord;
import cn.academy.misc.item.ACSimpleItem;
import cn.academy.misc.item.ItemCoin;
import cn.academy.misc.item.ItemMagHook;
import cn.academy.misc.item.ItemModuleAttached;
import cn.academy.misc.item.ItemNeedle;
import cn.academy.misc.item.ItemSilbarn;
import cn.annoreg.core.RegWithName;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.RegItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * AC Item Registration Class
 * @author WeathFold, KSkun
 */
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
	
	//TODO: These items isn't in used in AC beta.
/*	@RegItem
	public static ItemModuleAttached adEnergyCard;*/
	
	@RegItem
	@RegItem.UTName("alingot")
	@RegItem.OreDict("ingotAluminum")
	public static ACSimpleItem ingotAl;
	
	@RegItem
	@RegItem.UTName("cuingot")
	@RegItem.OreDict("ingotCopper")
	public static ACSimpleItem ingotCu;
	
	//TODO: These items isn't in used in AC beta.
/*	@RegItem
	@RegItem.UTName("steelingot")
	@RegItem.OreDict("ingotRefinedIron")
	public static ACSimpleItem ingotSteel;
	
	@RegItem
	@RegItem.UTName("tiningot")
	@RegItem.OreDict("ingotTin")
	public static ACSimpleItem ingotTin;*/
	
	@RegItem
	@RegItem.UTName("mgingot")
	@RegItem.OreDict("ingotMagnesium")
	public static ACSimpleItem ingotMg;
	
	@RegItem
	@RegItem.UTName("niingot")
	@RegItem.OreDict("ingotNickel")
	public static ACSimpleItem ingotNi;
	
	@RegItem
	@RegItem.UTName("crystal")
	@RegItem.OreDict("crystal")
	public static ACSimpleItem crystal;
	
	@RegItem
	@RegItem.UTName("shadowingot")
	@RegItem.OreDict("ingotShadow")
	public static ACSimpleItem ingotShadow;
	
	@RegItem
	@RegItem.UTName("mgplate")
	public static ACSimpleItem mgPlate;
	
	@RegItem
	@RegItem.UTName("alplate")
	public static ACSimpleItem alPlate;
	
	@RegItem
	@RegItem.UTName("aplate1")
	public static ACSimpleItem aplate1;
	
	@RegItem
	@RegItem.UTName("siliconrod")
	public static ACSimpleItem siliconRod;
	
	@RegItem
	@RegItem.UTName("smallsi")
	public static ACSimpleItem smallSi;
	
	//TODO: This is not available in the beta version, remove the annotation when in a release version.
//	@RegItem
//	@RegItem.UTName("irondust")
//	public static ACSimpleItem ironDust;

	@RegItem
	@RegItem.UTName("coppercoil")
	public static ACSimpleItem copperCoil;
	
	//TODO: If these items is unnecessary, delete them.
//	@RegItem
//	@RegItem.UTName("corebearing")
//	public static ACSimpleItem coreBearing;
	
	@RegItem
	@RegItem.UTName("bodydet")
	public static ACSimpleItem bodyDet;
	
	@RegItem
	@RegItem.UTName("io")
	public static ACSimpleItem ioPort;
	
	//TODO: This is not available in the beta version, remove the annotation when in a release version.
//	@RegItem
//	@RegItem.UTName("brainalpha")
//	public static ACSimpleItem brainAlpha;
	
	@RegItem
	@RegItem.UTName("brainbeta")
	public static ACSimpleItem brainBeta;
	
	@RegItem
	@RegItem.UTName("pcbn")
	@RegItem.OreDict("circuitBase")
	public static ACSimpleItem pcb;
	
	@RegItem
	@RegItem.UTName("cplank")
	public static ACSimpleItem compPlank;
	
	@RegItem()
	@RegItem.HasRender
	@RegEventHandler(Bus.FML)
	@RegWithName("void")
	public static ItemVoid ivoid = new ItemVoid() {
		//吐槽：这到底什么微妙的写法
		@SideOnly(Side.CLIENT)
		@RegItem.Render
		public RenderVoid renderVoid;
	};
	
	@RegItem
	public static ItemFreqRegulator freqReg;
	
	@RegItem
	@RegItem.UTName("silbarn")
	public static ItemSilbarn sibarn;
	
	@RegItem
	public static ItemMagHook magHook;
	
}
