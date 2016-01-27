/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block;

import cn.academy.core.AcademyCraft;
import cn.academy.core.block.ACBlockContainer;
import cn.academy.energy.client.gui.node.GuiNode;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Wireless Node block.
 * @author WeathFolD
 */
@Registrant
public class BlockNode extends ACBlockContainer {

    public enum NodeType {
        BASIC("basic", 15000, 150, 9, 5),
        STANDARD("standard", 50000, 300, 12, 10), 
        ADVANCED("advanced", 200000, 900, 19, 20);
        
        final int maxEnergy, bandwidth, range, capacity;
        final String name;
        NodeType(String _name, int _maxEnergy, int _bandwidth, int _range, int _capacity) {
            name = _name;
            maxEnergy = _maxEnergy;
            bandwidth = _bandwidth;
            range = _range;
            capacity = _capacity;
        }
    }
    
    final NodeType type;
    IIcon iconTop_disabled, iconTop_enabled;
    IIcon sideIcon[];
    
    public BlockNode(NodeType _type) {
        super("node", Material.rock, guiHandler);
        setCreativeTab(AcademyCraft.cct);
        setBlockName("ac_node_" + _type.name);
        setHardness(2.5f);
        setHarvestLevel("pickaxe", 1);
        
        type = _type;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
        iconTop_disabled = ir.registerIcon("academy:node_top_0");
        iconTop_enabled = ir.registerIcon("academy:node_top_1");
        sideIcon = new IIcon[5];
        for(int i = 0; i < 5; ++i) {
            sideIcon[i] = ir.registerIcon("academy:node_" + type.name + "_side_" + i);
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return (side == 0 || side == 1) ? iconTop_enabled : sideIcon[1];
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderBlockPass() {
        return -1;
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity te = world.getTileEntity(x, y, z);
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
        if(side == 0 || side == 1) {
            return enabled ? iconTop_enabled : iconTop_disabled;
        }
        
        
        return sideIcon[pct];
    }
    
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, 
            float tx, float ty, float tz, int meta) {
        return type.ordinal();
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
            return c == null ? null : new GuiNode(c);
        }
        
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileNode te = check(world, x, y, z);
            return te == null ? null : new ContainerNode(te, player);
        }
        
        private TileNode check(World world, int x, int y, int z) {
            TileEntity te = world.getTileEntity(x, y, z);
            return (TileNode) (te instanceof TileNode ? te : null);
        }
    };

}
