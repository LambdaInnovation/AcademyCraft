package cn.academy.item;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.academy.entity.EntityMagHook;
import cn.lambdalib2.template.client.render.item.RenderModelItem;
import cn.lambdalib2.util.ItemModelCustom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Elec Move Support Hook
 * @author WeathFolD
 */
public class ItemMagHook extends Item {
    
    @SideOnly(Side.CLIENT)
    public static HookRender render;

    public ItemMagHook() {
        setCreativeTab(AcademyCraft.cct);
        setUnlocalizedName("ac_maghook");
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if(!world.isRemote) {
            world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            world.spawnEntity(new EntityMagHook(player));
            if(!player.capabilities.isCreativeMode)
                stack.setCount(stack.getCount()-1);
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