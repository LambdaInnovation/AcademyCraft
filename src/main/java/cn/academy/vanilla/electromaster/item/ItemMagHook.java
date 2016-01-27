/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
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
