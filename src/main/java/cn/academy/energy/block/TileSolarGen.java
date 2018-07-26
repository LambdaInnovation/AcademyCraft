package cn.academy.energy.block;

import cn.academy.core.block.TileGeneratorBase;
import cn.academy.energy.IFConstants;
import cn.academy.energy.client.render.block.RenderSolarGen;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@RegTileEntity
@RegTileEntity.HasRender
public class TileSolarGen extends TileGeneratorBase implements IMultiTile {

    public static final int SLOT_BATTERY = 0;

    public enum SolarStatus {
        STOPPED, WEAK, STRONG
    }
    
    @SideOnly(Side.CLIENT)
    @RegTileEntity.Render
    public static RenderSolarGen renderer;

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
        return isDay && world.canBlockSeeTheSky(x, y + 1, z);
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
            return ((BlockMulti) block).getRenderBB(x, y, z, info.getDir());
        } else {
            return super.getRenderBoundingBox();
        }
    }

}