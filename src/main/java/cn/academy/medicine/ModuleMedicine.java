package cn.academy.medicine;

import cn.academy.core.item.ACItem;
import cn.academy.medicine.blocks.BlockMatExtractor;
import cn.academy.medicine.blocks.BlockMedSynthesizer;
import cn.academy.medicine.blocks.ContainerMatExtractor;
import cn.academy.medicine.blocks.GuiMatExtractor;
import cn.academy.medicine.items.ItemMedicineBottle;
import cn.academy.medicine.items.ItemPowder;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegBlock;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.annoreg.mc.RegPreInitCallback;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;


/**
 * Initialization for medicine module.
 */
@Registrant
public class ModuleMedicine {


    @RegBlock
    private static final BlockMatExtractor matExtractor = new BlockMatExtractor();

    @RegBlock
    private static final BlockMedSynthesizer medSynthesizer = new BlockMedSynthesizer();

    @RegItem
    public static final ItemMedicineBottle medicineBottle = new ItemMedicineBottle();

    @RegItem
    public static Item emptyBottle = new ACItem("empty_med_bottle") {
        @Override
        public void registerIcons(IIconRegister iicon) {}
    };

    @RegPreInitCallback
    private static void init() {
        ItemPowder._init();
    }

    @SideOnly(Side.CLIENT)
    @RegInitCallback
    private static void initClient() {
        MinecraftForgeClient.registerItemRenderer(medicineBottle,
                new ItemMedicineBottle.RenderMedicineBottle(itemStack -> medicineBottle.getInfo(itemStack).displayColor));

        MinecraftForgeClient.registerItemRenderer(emptyBottle,
                new ItemMedicineBottle.RenderMedicineBottle(itemStack -> new Color(0x000000)));
    }

}
