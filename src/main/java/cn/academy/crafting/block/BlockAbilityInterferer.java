package cn.academy.crafting.block;

import cn.academy.ability.block.TileAbilityInterferer;
import cn.academy.core.block.ACBlockContainer;
import cn.academy.crafting.client.ui.GuiAbilityInterferer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Created by Paindar on 2017/3/31.
 */
@Registrant
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
            TileEntity tile = world.getTileEntity(x, y, z);
            if(tile instanceof TileAbilityInterferer)
                return new ContainAbilityInterferer((TileAbilityInterferer)tile, player);
            return null;
        }
    };

    private IIcon iconOn;
    private IIcon iconOff;

    public BlockAbilityInterferer()
    {
        super("ability_interferer", Material.rock, guiHandler);
    }


    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        return new TileAbilityInterferer();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir){
        iconOn = ricon(ir, "ability_interf_on");
        iconOff = ricon(ir, "ability_interf_off");
    }

    @Override
    public void onBlockPlacedBy(World world,int x,int y,int z,EntityLivingBase placer,ItemStack stack)
    {
        TileEntity tile = world.getTileEntity(x,y,z);
        if(placer instanceof EntityPlayer && tile instanceof  TileAbilityInterferer)
        {
            ((TileAbilityInterferer)tile).setPlacer((EntityPlayer)placer);
        }
    }


    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileAbilityInterferer)
        {
            if (((TileAbilityInterferer) tile).enabled())
                return iconOn;
            else
                return iconOff;
        }
        return iconOn;
    }

    @Override
    public IIcon getIcon(int side,int meta){return iconOff;}

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderBlockPass(){return -1;}

    @Override
    public boolean isOpaqueCube(){return false;}
}
