package cn.academy.core.register;

import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;
import cn.academy.core.AcademyCraftMod;
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
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.core.ctor.Arg;
import cn.annoreg.core.ctor.Ctor;
import cn.annoreg.mc.RegItem;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class ACItems {
	
	@RegItem
	@RegItem.UTName
	public static Item logo;
	
	@RegItem
	@Ctor(@Arg(Int = 0))
	public static ACRecord record0;
	@RegItem
	@Ctor(@Arg(Int = 1))
	public static ACRecord record1;
	@RegItem
	@Ctor(@Arg(Int = 2))
	public static ACRecord record2;
	
	@RegItem(renderName = "renderCoin")
	public static ItemCoin coin;
	
	@SideOnly(Side.CLIENT)
	public static RendererCoin.ItemRender renderCoin;

	@RegItem
	@Ctor(@Arg(Int = 1))
	public static ItemCapsule capsuleH;
	@RegItem
	@Ctor(@Arg(Int = 2))
	public static ItemCapsule capsuleM;
	@RegItem
	@Ctor(@Arg(Int = 3))
	public static ItemCapsule capsuleL;

	@RegItem
	@Ctor(@Arg(Int = 1))
	public static ItemTablet tabletH;
	@RegItem
	@Ctor(@Arg(Int = 2))
	public static ItemTablet tabletM;
	@RegItem
	@Ctor(@Arg(Int = 3))
	public static ItemTablet tabletL;
	
	@RegItem
	public static ItemNeedle needle;

	@RegItem
	public static ItemEnergyCrystal EnergyCrystal;
	
	@RegItem
	public static ItemModuleAttached adEnergyCard;
	
	@RegItem
	@RegItem.UTName("aluminumingot")
	@RegItem.OreDict("ingotAluminum")
	public static Item AluminumIngot;
	
	@RegItem
	@RegItem.UTName("copperingot")
	@RegItem.OreDict("ingotCopper")
	public static Item CopperIngot;
	
	@RegItem
	@RegItem.UTName("steelingot")
	@RegItem.OreDict("ingotRefinedIron")
	public static Item SteelIngot;
	
	@RegItem
	@RegItem.UTName("tiningot")
	@RegItem.OreDict("ingotTin")
	public static Item TinIngot;
	
	@RegItem
	@RegItem.UTName("siliconrod")
	public static Item SiliconRod;
	
	@RegItem
	@RegItem.UTName("coppercoil")
	public static Item CopperCoil;
	
	@RegItem
	@RegItem.UTName("corebearing")
	public static Item CoreBearing;
	
	@RegItem(name = "void", renderName = "renderVoid")
	public static ItemVoid ivoid;
	
	@SideOnly(Side.CLIENT)
	public static RenderVoid renderVoid;
	
}
