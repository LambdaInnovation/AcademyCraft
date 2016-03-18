/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.item.ACItem;
import cn.academy.terminal.TerminalData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

/**
 * @author WeAthFolD
 */
public class ItemMedia extends ACItem {
    
    IIcon[] icons;

    public ItemMedia() {
        super("media");
        setMaxStackSize(1);
        hasSubtypes = true;
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)  {
        if(!player.worldObj.isRemote) {
            MediaData data = MediaData.of(player);
            TerminalData tData = TerminalData.get(player);
            
            ACMedia media = getInternal(stack.getItemDamage());
            
            if(!tData.isInstalled(AppMediaPlayer.instance)) {
                player.addChatMessage(new ChatComponentTranslation("ac.media.notinstalled"));
            } else if(data.isInstalled(media)) {
                player.addChatMessage(new ChatComponentTranslation("ac.media.haveone", media.getName()));
            } else {
                data.install(media);
                if(!player.capabilities.isCreativeMode)
                    stack.stackSize--;
                player.addChatMessage(new ChatComponentTranslation("ac.media.acquired", media.getName()));
            }
        }
        return stack;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister ir) {
        List<ACMedia> internal = MediaManager.internalMedias();
        icons = new IIcon[internal.size()];
        for(int i = 0; i < internal.size(); ++i) {
            icons[i] = ir.registerIcon("academy:media_" + internal.get(i).getID());
        }
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int meta) {
        return icons[meta];
    }
    
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return getInternal(stack.getItemDamage()).getName();
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
        list.add(getInternal(stack.getItemDamage()).getDesc());
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for(int i = 0; i < MediaManager.internalMedias().size(); ++i) {
            list.add(new ItemStack(this, 1, i));
        }
    }

    private ACMedia getInternal(int idx) {
        return MediaManager.internalMedias().get(idx);
    }

}
