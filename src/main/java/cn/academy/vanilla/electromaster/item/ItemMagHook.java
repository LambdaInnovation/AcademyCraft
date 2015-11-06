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
package cn.academy.vanilla.electromaster.item;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.academy.vanilla.electromaster.entity.EntityMagHook;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.template.client.render.item.RenderModelItem;
import cn.lambdalib.util.deprecated.ItemModelCustom;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Elec Move Support Hook
 * @author WeathFolD
 */
public class ItemMagHook extends Item {
	
	@SideOnly(Side.CLIENT)
	@RegItem.Render
	public static HookRender render;

	public ItemMagHook() {
		setCreativeTab(AcademyCraft.cct);
		setUnlocalizedName("ac_maghook");
		setTextureName("academy:maghook");
	}
	
    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
    	if(!world.isRemote) {
    		world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
    		world.spawnEntityInWorld(new EntityMagHook(player));
    		if(!player.capabilities.isCreativeMode)
    			--stack.stackSize;
    	}
        return stack;
    }
    
    @SideOnly(Side.CLIENT)
    public static class HookRender extends RenderModelItem {
    	public HookRender() {
    		super(new ItemModelCustom(Resources.getModel("maghook")), 
    			Resources.getTexture("models/maghook"));
    		renderInventory = false;
    		this.setScale(0.15d);
    		this.setStdRotation(0, -90, 90);
    		this.setOffset(0, 0.0, -3);
    		this.setEquipOffset(1, 0, 0);
    	}
    	
    	@Override
		protected void renderAtStdPosition(float i) {
    		this.setOffset(0, 0, 1);
    		this.setEquipOffset(0.5, 0.1, 0);
    		super.renderAtStdPosition(i);
    	}
    }


}
