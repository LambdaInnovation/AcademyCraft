package cn.academy.block.tileentity;

import cn.academy.block.WindGeneratorConsts;
import cn.academy.energy.ModuleEnergy;
import cn.academy.client.render.block.RenderWindGenMain;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.BlockMulti.SubBlockPos;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkMessage.NullablePar;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
@RegTileEntity
@RegTileEntity.HasRender
public class TileWindGenMain extends TileInventory implements IMultiTile {
    
    static List<SubBlockPos>[] checkAreas = new ArrayList[6];
    static {
        List<SubBlockPos> checkArea = new ArrayList();
        for(int i = -6; i <= 6; ++i) {
            for(int j = -6; j <= 6; ++j) {
                if(i != 0 || j != 0)
                    checkArea.add(new SubBlockPos(i, j, -1));
            }
        }
        
        for(int i = 2; i < 6; ++i) {
            List list = (checkAreas[i] = new ArrayList());
            for(SubBlockPos pos : checkArea) {
                list.add(BlockMulti.rotate(pos, ForgeDirection.values()[i]));
            }
        }
    }
    
    // State for render
    @SideOnly(Side.CLIENT)
    @RegTileEntity.Render
    public static RenderWindGenMain renderer;
    
    public long lastFrame = -1;
    public float lastRotation;
    
    public boolean complete;
    public boolean noObstacle;
    
    int updateWait, updateWait2;
    
    public TileWindGenMain() {
        super("windgen_main", 1);
    }

    // Spin logic
    public boolean isFanInstalled() {
        ItemStack stack = this.getStackInSlot(0);
        return stack != null && stack.getItem() == ModuleEnergy.windgenFan;
    }
    
    /**
     * Unit: Degree per second
     */
    @SideOnly(Side.CLIENT)
    public double getSpinSpeed() {
        return complete ? 60.0 : 0;
    }

    // InfoBlockMulti delegates
    InfoBlockMulti info = new InfoBlockMulti(this);
    
    @Override
    public void updateEntity() {
        super.updateEntity();
        info.update();
        
        if(info.getSubID() == 0) {
            if(++updateWait == 10) {
                updateWait = 0;
                complete = isCompleteStructure();
                noObstacle = complete && isNoObstacle();
            }
            
            if(!getWorld().isRemote) {
                if(++updateWait2 == 20) {
                    updateWait2 = 0;
                    NetworkMessage.sendToAllAround(
                            TargetPoints.convert(this, 50),
                            this, "sync", inventory[0]
                    );
                }
            }
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
    
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != 0 || (stack != null && stack.getItem() == ModuleEnergy.windgenFan);
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
    
    public boolean isCompleteStructure() {
        int[] origin = ModuleEnergy.windgenMain.getOrigin(this);
        if(origin == null)
            return false;
        
        int x = origin[0], y = origin[1] - 1, z = origin[2];
        int state = 1;
        int pillars = 0;
        
        for(; state < 2; --y) {
            Block block = world.getBlock(x, y, z);
            if(state == 1) {
                if(block == ModuleEnergy.windgenPillar) {
                    ++pillars;
                    if(pillars > WindGeneratorConsts.MAX_PILLARS)
                        break;
                } else if(block == ModuleEnergy.windgenBase){
                    state = 2;
                } else {
                    state = 3;
                }
            }
        }
        return state == 2 && pillars >= WindGeneratorConsts.MIN_PILLARS;
    }
    
    public boolean isNoObstacle() {
        int x = x, y = y, z = z;
        InfoBlockMulti info = getBlockInfo();
        World world = getWorld();
        List<SubBlockPos> arr = checkAreas[info.getDir().ordinal()];
        for(SubBlockPos sbp : arr) {
            if(world.getBlock(x + sbp.dx, y + sbp.dy, z + sbp.dz).getMaterial() != Material.air)
                return false;
        }
        return true;
    }

    @Listener(channel="sync", side=Side.CLIENT)
    private void syncStack(@NullablePar ItemStack stack) {
        setInventorySlotContents(0, stack);
    }
    
}