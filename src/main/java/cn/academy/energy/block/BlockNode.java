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

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.client.gui.node.GuiNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Wireless Node block.
 * @author WeathFolD
 */
@RegistrationClass
public class BlockNode extends BlockContainer {

    public enum NodeType {
        BASIC("basic", 10000, 20, 9), 
        STANDARD("standard", 50000, 40, 12), 
        ADVANCED("advanced", 300000, 100, 19);
        
        final int maxEnergy, latency, range;
        final String name;
        NodeType(String _name, int _maxEnergy, int _latency, int _range) {
            name = _name;
            maxEnergy = _maxEnergy;
            latency = _latency;
            range = _range;
        }
    }
    
    final NodeType type;
    IIcon iconTop_disabled, iconTop_enabled;
    IIcon sideIcon[];
    
    public BlockNode(NodeType _type) {
        super(Material.rock);
        setCreativeTab(AcademyCraft.cct);
        setBlockName("node_" + _type.name);
        
        type = _type;
    }
    
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
        iconTop_disabled = ir.registerIcon("academy:node_top_0");
        iconTop_enabled = ir.registerIcon("academy:node_top_1");
        sideIcon = new IIcon[5];
        for(int i = 0; i < 4; ++i) {
            sideIcon[i] = ir.registerIcon("academy:node_" + type.name + "_side_" + i);
        }
    }
    
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return (side == 0 || side == 1) ? iconTop_enabled : sideIcon[1];
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
            pct = Math.min(4, (int)(node.getEnergy() / node.getMaxEnergy()));
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
    
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        if(!player.isSneaking()) {
            guiHandler.openGuiContainer(player, world, x, y, z);
            return true;
        }
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileNode();
    }
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            ContainerNode c = (ContainerNode) getServerContainer(player, world, x, y, z);
            return c == null ? null : new GuiNode(c);
        }
        
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
