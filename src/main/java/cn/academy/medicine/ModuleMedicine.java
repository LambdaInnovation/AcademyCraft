package cn.academy.medicine;

import cn.academy.core.item.ACItem;
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

    @RegGuiHandler
    static final GuiHandlerBase guiHandlerMatExtractor = new GuiHandlerBase() {
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            return new GuiMatExtractor();
        }

        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            return new ContainerMatExtractor();
        }
    };

    @RegGuiHandler
    static final GuiHandlerBase guiHandlerMedSynth = MedSynthesizer.guiHandler();

    @RegBlock
    private static final BlockMatExtractor$ matExtractor = BlockMatExtractor$.MODULE$;

    @RegBlock
    private static final BLockMedSynthesizer$ medSynthesizer = BLockMedSynthesizer$.MODULE$;

    @RegItem
    private static final ItemMedicineBottle$ medicineBottle = ItemMedicineBottle$.MODULE$;

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
                RenderMedicineBottle.apply(itemStack -> medicineBottle.getInfo(itemStack).displayColor()));

        MinecraftForgeClient.registerItemRenderer(emptyBottle,
                RenderMedicineBottle.apply(itemStack -> new Color(0x000000)));
    }

}
