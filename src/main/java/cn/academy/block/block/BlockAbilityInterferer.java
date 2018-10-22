package cn.academy.block.block;

import cn.academy.block.TileAbilityInterferer;
import cn.academy.block.container.ContainAbilityInterferer;
import cn.academy.crafting.client.ui.GuiAbilityInterferer;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
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

    public static final PropertyBool PROP_ON = PropertyBool.create("on");

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

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PROP_ON);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);

        boolean on = false;
        if (tile instanceof TileAbilityInterferer)
        {
            if (((TileAbilityInterferer) tile).enabled())
                on = true;
        }
        return state.withProperty(PROP_ON, on);
    }

    public BlockAbilityInterferer() {
        super(Material.ROCK, guiHandler);
    }


    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        return new TileAbilityInterferer();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if(placer instanceof EntityPlayer && tile instanceof  TileAbilityInterferer) {
            ((TileAbilityInterferer)tile).setPlacer((EntityPlayer)placer);
        }
    }

}