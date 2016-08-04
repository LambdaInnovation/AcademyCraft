package cn.academy.medicine;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegBlock;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.annoreg.mc.RegPreInitCallback;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

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

    @RegPreInitCallback
    private static void init() {
        ItemPowder._init();
    }

}
