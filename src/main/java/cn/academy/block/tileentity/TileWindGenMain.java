package cn.academy.block.tileentity;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.block.WindGeneratorConsts;
import cn.academy.client.render.block.RenderWindGenMain;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.BlockMulti.SubBlockPos;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkMessage.NullablePar;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
@RegTileEntity
public class TileWindGenMain extends TileInventory implements IMultiTile, ITickable  {
    
    static List<SubBlockPos>[] checkAreas = new ArrayList[6];
    static {
        List<SubBlockPos> checkArea = new ArrayList();
        for(int i = -7; i <= 7; ++i) {
            for(int j = -7; j <= 7; ++j) {
                if(i != 0 || j != 0)
                    checkArea.add(new SubBlockPos(i, j, -1));
            }
        }
        
        for(int i = 2; i < 6; ++i) {
            List list = (checkAreas[i] = new ArrayList());
            for(SubBlockPos pos : checkArea) {
                list.add(BlockMulti.rotate(pos, EnumFacing.values()[i]));
            }
        }
    }

    public double lastFrame = -1;
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
        return stack != null && stack.getItem() == ACItems.windgen_fan;
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
    public void update() {
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
        return slot != 0 || (stack.getItem() == ACItems.windgen_fan);
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
    
    public boolean isCompleteStructure() {
        BlockPos origin = ACBlocks.windgen_main.getOrigin(this);
        if(origin == null)
            return false;
        
        int x = origin.getX(), y = origin.getY() - 1, z = origin.getZ();
        int state = 1;
        int pillars = 0;
        
        for(; state < 2; --y) {
            Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
            if(state == 1) {
                if(block == ACBlocks.windgen_pillar) {
                    ++pillars;
                    if(pillars > WindGeneratorConsts.MAX_PILLARS)
                        break;
                } else if(block == ACBlocks.windgen_base){
                    state = 2;
                } else {
                    state = 3;
                }
            }
        }
        return state == 2 && pillars >= WindGeneratorConsts.MIN_PILLARS;
    }
    
    public boolean isNoObstacle() {
        InfoBlockMulti info = getBlockInfo();
        World world = getWorld();
        List<SubBlockPos> arr = checkAreas[info.getDir().ordinal()];
        for(SubBlockPos sbp : arr) {
            Material material = world.getBlockState(getPos().add(sbp.dx, sbp.dy, sbp.dz)).getMaterial();
            if(material != Material.AIR)
                return false;
        }
        return true;
    }

    @Listener(channel="sync", side=Side.CLIENT)
    private void syncStack(@NullablePar ItemStack stack) {
        setInventorySlotContents(0, stack);
    }
    
}