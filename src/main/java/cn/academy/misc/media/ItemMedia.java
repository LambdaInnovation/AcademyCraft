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
package cn.academy.misc.media;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import cn.academy.core.item.ACItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
			MediaData data = MediaData.get(player);
			int mID = stack.getItemDamage();
			
			if(data.isMediaInstalled(mID)) {
				player.addChatMessage(new ChatComponentTranslation("ac.media.haveone", MediaPlayer.getMedia(mID).getName()));
			} else {
				data.installMedia(mID);
				player.addChatMessage(new ChatComponentTranslation("ac.media.acquired", MediaPlayer.getMedia(mID).getName()));
			}
		}
        return stack;
    }
	
    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister ir) {
    	icons = new IIcon[MediaPlayer.getMediaCount()];
    	for(int i = 0; i < MediaPlayer.getMediaCount(); ++i) {
    		icons[i] = ir.registerIcon("academy:media_" + MediaPlayer.getMedia(i).name);
    	}
    }
    
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
    	return StatCollector.translateToLocal(MediaPlayer.getMedia(stack.getItemDamage()).getDisplayName());
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
    	list.add(MediaPlayer.getMedia(stack.getItemDamage()).getDesc());
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for(int i = 0; i < MediaPlayer.getMediaCount(); ++i) {
        	list.add(new ItemStack(this, 1, i));
        }
    }

}
