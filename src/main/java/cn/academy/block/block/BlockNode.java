package cn.academy.block.block;

import cn.academy.AcademyCraft;
import cn.academy.block.container.ContainerNode;
import cn.academy.block.tileentity.TileNode;
import cn.academy.energy.client.ui.GuiNode;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Wireless Node block.
 * @author WeathFolD
 */
public class BlockNode extends ACBlockContainer {

    public static PropertyBool CONNECTED = PropertyBool.create("connected");

    public static PropertyInteger ENERGY = PropertyInteger.create("energy", 0, 4);

    public enum NodeType implements IStringSerializable {
        BASIC("basic", 15000, 150, 9, 5),
        STANDARD("standard", 50000, 300, 12, 10), 
        ADVANCED("advanced", 200000, 900, 19, 20);
        
        public final int maxEnergy, bandwidth, range, capacity;
        public final String name;
        NodeType(String _name, int _maxEnergy, int _bandwidth, int _range, int _capacity) {
            name = _name;
            maxEnergy = _maxEnergy;
            bandwidth = _bandwidth;
            range = _range;
            capacity = _capacity;
        }

        @Override
        public String getName() {
            return name;
        }
    }
    
    final NodeType type;

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        boolean enabled;
        int pct;
        if(te instanceof TileNode) {
            TileNode node = (TileNode) te;
            enabled = node.enabled;
            pct = (int) Math.min(4, Math.round((4 * node.getEnergy() / node.getMaxEnergy())));
        } else {
            enabled = false;
            pct = 0;
        }

        return state
            .withProperty(CONNECTED, enabled)
            .withProperty(ENERGY, pct);
    }

    public BlockNode(NodeType _type) {
        super(Material.ROCK, guiHandler);
        setCreativeTab(AcademyCraft.cct);
        setHardness(2.5f);
        setHarvestLevel("pickaxe", 1);

        type = _type;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CONNECTED, ENERGY);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (placer instanceof EntityPlayer) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileNode) {
                ((TileNode) tile).setPlacer((EntityPlayer) placer);
            }
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return type==null?0:type.ordinal();
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileNode();
    }

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Override
        @SideOnly(Side.CLIENT)
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            ContainerNode c = (ContainerNode) getServerContainer(player, world, x, y, z);
            return c == null ? null : GuiNode.apply(c);
        }
        
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileNode te = check(world, x, y, z);
            return te == null ? null : new ContainerNode(te, player);
        }
        
        private TileNode check(World world, int x, int y, int z) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            return (TileNode) (te instanceof TileNode ? te : null);
        }
    };

}