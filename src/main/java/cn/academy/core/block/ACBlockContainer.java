package cn.academy.core.block;

import cn.academy.core.AcademyCraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * BaseClass for typical block containers. will automatically try to open the container gui.
 * @author WeAthFolD
 */
public abstract class ACBlockContainer extends BlockContainer {
    
    final GuiHandlerBase guiHandler;

    public ACBlockContainer(String name, Material mat) {
        this(name, mat, null);
    }
    
    public ACBlockContainer(String name, Material mat, GuiHandlerBase _guiHandler) {
        super(mat);
        guiHandler = _guiHandler;
        setCreativeTab(AcademyCraft.cct);
        setBlockName("ac_" + name);
        setBlockTextureName("academy:" + name);
    }
    
    protected IIcon ricon(IIconRegister ir, String name) {
        return ir.registerIcon("academy:" + name);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        if(guiHandler != null && !player.isSneaking()) {
            if(!world.isRemote)
                guiHandler.openGuiContainer(player, world, x, y, z);
            return true;
        }
        return false;
    }
    
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int wtf) {
        if(!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if(te instanceof IInventory) {
                StackUtils.dropItems(world, x, y, z, (IInventory) te);
            }
        }
        super.breakBlock(world, x, y, z, block, wtf);
    }

}
