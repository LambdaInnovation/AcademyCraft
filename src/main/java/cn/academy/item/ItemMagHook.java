package cn.academy.item;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.academy.entity.EntityMagHook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
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
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(!world.isRemote) {
            world.playSound(
                player,
                player.posX, player.posY, player.posZ,
                Resources.sound("random.bow"),
                SoundCategory.PLAYERS,
                0.5F,
                0.4F / (itemRand.nextFloat() * 0.4F + 0.8F)
            );
            world.spawnEntity(new EntityMagHook(player));
            if(!player.capabilities.isCreativeMode)
                stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
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