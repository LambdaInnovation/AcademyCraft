package cn.academy.block.block;

import cn.academy.block.TileAbilityInterferer;
import cn.academy.block.container.ContainAbilityInterferer;
import cn.academy.crafting.client.ui.GuiAbilityInterferer;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by Paindar on 2017/3/31.
 */
public class BlockAbilityInterferer extends ACBlockContainer
{
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int  x, int y, int z){
            ContainAbilityInterferer container = (ContainAbilityInterferer)getServerContainer(player, world, x, y, z);
            if (container == null)
                return null;
            return GuiAbilityInterferer.apply(container);
        }

        @Override
        protected Object getServerContainer(EntityPlayer player,World world,int x,int y,int z){
            TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if(tile instanceof TileAbilityInterferer)
                return new ContainAbilityInterferer((TileAbilityInterferer)tile, player);
            return null;
        }
    };

//    private IIcon iconOn;
//    private IIcon iconOff;

    public BlockAbilityInterferer() {
        super(Material.ROCK, guiHandler);
    }


    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        return new TileAbilityInterferer();
    }

//    @Override
//    @SideOnly(Side.CLIENT)
//    public void registerBlockIcons(IIconRegister ir){
//        iconOn = ricon(ir, "ability_interf_on");
//        iconOff = ricon(ir, "ability_interf_off");
//    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if(placer instanceof EntityPlayer && tile instanceof  TileAbilityInterferer) {
            ((TileAbilityInterferer)tile).setPlacer((EntityPlayer)placer);
        }
    }


//    @Override
//    @SideOnly(Side.CLIENT)
//    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side)
//    {
//        TileEntity tile = world.getTileEntity(x, y, z);
//        if (tile instanceof TileAbilityInterferer)
//        {
//            if (((TileAbilityInterferer) tile).enabled())
//                return iconOn;
//            else
//                return iconOff;
//        }
//        return iconOn;
//    }
//
//    @Override
//    public IIcon getIcon(int side,int meta){return iconOff;}
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }
}