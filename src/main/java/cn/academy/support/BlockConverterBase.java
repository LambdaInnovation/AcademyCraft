/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support;

import cn.academy.core.block.ACBlockContainer;
import cn.academy.energy.api.block.IWirelessUser;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

/**
 * @author WeAthFolD
 */
public abstract class BlockConverterBase extends ACBlockContainer {

    public static class Item extends ItemBlock {

        BlockConverterBase converter;

        public Item(Block block) {
            super(block);
            converter = (BlockConverterBase) block;
        }

        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean idk) {
            list.add(StatCollector.translateToLocalFormatted("ac.converter.desc_template", converter.from,
                    converter.to));
        }

    }

    public final Class<? extends TileEntity> tileType;
    public final String from, to;

    public BlockConverterBase(String name, String _from, String _to, Class<? extends TileEntity> _tileType) {
        super(name, Material.rock);
        from = _from;
        to = _to;
        tileType = _tileType;
        setHarvestLevel("pickaxe", 0);
        setHardness(2.5f);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float tx, float ty,
            float tz) {
        TileEntity te = WorldUtils.getTileEntity(world, x, y, z, tileType);
        if (te != null && te instanceof IWirelessUser && !player.isSneaking()) {
            if (world.isRemote) {
                displayGui((IWirelessUser) te);
            }
            return true;
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    private void displayGui(IWirelessUser te) {
        // Minecraft.getMinecraft().displayGuiScreen(new GuiLinkToNode(te));
    }

}
