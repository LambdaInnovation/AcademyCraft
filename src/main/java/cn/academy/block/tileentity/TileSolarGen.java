package cn.academy.block.tileentity;

import cn.academy.energy.IFConstants;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntity;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//import cn.academy.client.render.block.RenderSolarGen;

/**
 * @author WeAthFolD
 */
@RegTileEntity
public class TileSolarGen extends TileGeneratorBase implements IMultiTile {

    public static final int SLOT_BATTERY = 0;

    public enum SolarStatus {
        STOPPED, WEAK, STRONG
    }

    public TileSolarGen() {
        super("solar_generator", 1, 1000, IFConstants.LATENCY_MK2);
    }

    @Override
    public double getGeneration(double required) {
        World world = this.getWorld();
        double brightLev =  canGenerate() ? 1.0 : 0.0;
        brightLev *= world.isRaining() ? 0.2 : 1.0;

        return Math.min(required, brightLev * 3.0);
    }

    public SolarStatus getStatus() {
        World world = getWorld();
        if (canGenerate()) {
            return world.isRaining() ? SolarStatus.WEAK : SolarStatus.STRONG;
        } else {
            return SolarStatus.STOPPED;
        }
    }

    private boolean canGenerate() {
        World world = getWorld();
        long time = world.getWorldTime() % 24000;
        boolean isDay = time >= 0 && time <= 12500;
        return isDay && world.canSeeSky(getPos().add(0, 1, 0));
    }
    
    // InfoBlockMulti delegates
    InfoBlockMulti info = new InfoBlockMulti(this);
    
    @Override
    public void update() {
        super.update();
        info.update();

        ItemStack battery = getStackInSlot(SLOT_BATTERY);
        if (battery != null) {
            tryChargeStack(battery);
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        info = new InfoBlockMulti(this, tag);
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        info.save(tag);
        return tag;
    }

    @Override
    public InfoBlockMulti getBlockInfo() {
        return info;
    }

    @Override
    public void setBlockInfo(InfoBlockMulti i) {
        info = i;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        Block block = getBlockType();
        if(block instanceof BlockMulti) {
            return ((BlockMulti) block).getRenderBB(getPos(), info.getDir());
        } else {
            return super.getRenderBoundingBox();
        }
    }

}