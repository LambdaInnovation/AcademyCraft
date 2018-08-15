package cn.academy.item;

import cn.academy.Resources;
import cn.academy.entity.EntitySilbarn;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSilbarn extends Item {
    
    @SideOnly(Side.CLIENT)
    public static RenderSilbarn render;
    
    public ItemSilbarn() {
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(!world.isRemote) {
            world.playSound(player, player.posX,player.posY,player.posZ,
                    new SoundEvent(new ResourceLocation( "random.bow"))
                    , SoundCategory.AMBIENT, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            world.spawnEntity(new EntitySilbarn(player));
        }
        if(!player.capabilities.isCreativeMode)
            stack.setCount(stack.getCount()-1);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    public static class RenderSilbarn extends RenderModelItem {

        public RenderSilbarn() {
            super(new ItemModelCustom(Resources.getModel("silbarn")), Resources.getTexture("models/silbarn"));
            this.renderInventory = false;
            this.setStdRotation(90, 0, 0);
            this.setEquipRotation(0, 90, 0);
            this.setEquipOffset(.5, 0.1, -.2);
        }
        
    }
    
}