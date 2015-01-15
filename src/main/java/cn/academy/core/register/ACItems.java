package cn.academy.core.register;

import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.render.RenderVoid;
import cn.academy.core.item.ItemVoid;
import cn.academy.misc.client.render.RendererCoin;
import cn.academy.misc.item.ACRecord;
import cn.academy.misc.item.ItemCapsule;
import cn.academy.misc.item.ItemCoin;
import cn.academy.misc.item.ItemEnergyCrystal;
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

	@RegItem
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
	public static ItemTablet tabletL = new ItemTablet(3);
	
	@RegItem
	public static ItemNeedle needle;

	@RegItem
	public static ItemEnergyCrystal energyCrystal;
	
	@RegItem
	public static ItemModuleAttached adEnergyCard;
	
	@RegItem
	@RegItem.UTName("aluminumingot")
	@RegItem.OreDict("ingotAluminum")
	public static Item ingotAl;
	
	@RegItem
	@RegItem.UTName("copperingot")
	@RegItem.OreDict("ingotCopper")
	public static Item ingotCu;
	
	@RegItem
	@RegItem.UTName("steelingot")
	@RegItem.OreDict("ingotRefinedIron")
	public static Item ingotSteel;
	
	@RegItem
	@RegItem.UTName("tiningot")
	@RegItem.OreDict("ingotTin")
	public static Item ingotTin;
	
	@RegItem
	@RegItem.UTName("siliconrod")
	public static Item siliconRod;
	
	@RegItem
	@RegItem.UTName("coppercoil")
	public static Item copperCoil;
	
	@RegItem
	@RegItem.UTName("corebearing")
	public static Item coreBearing;
	
	@RegItem()
	@RegItem.HasRender
	@RegWithName("void")
	public static ItemVoid ivoid = new ItemVoid() {
		@SideOnly(Side.CLIENT)
		@RegItem.Render
		public RenderVoid renderVoid;
	};
	
	
}
