package cn.academy.item;

import cn.academy.AcademyCraft;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.misc.media.Media;
import cn.academy.misc.media.MediaAcquireData;
import cn.academy.misc.media.MediaApp$;
import cn.academy.misc.media.MediaManager;
import cn.academy.terminal.TerminalData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class MediaItem extends Item
{
    public MediaItem()
    {
        setMaxStackSize(1);
        hasSubtypes = true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote)
        {
            MediaAcquireData acquireData = MediaAcquireData.apply(player);
            TerminalData tData = TerminalData.get(player);

            Media media = getMedia(stack.getItemDamage());

            if (!tData.isInstalled(MediaApp$.MODULE$))
            {
                player.sendMessage(new TextComponentTranslation("ac.media.notinstalled"));
            }
            else if (acquireData.isInstalled(media))
            {
                player.sendMessage(new TextComponentTranslation("ac.media.haveone", media.name()));
            }
            else
            {
                acquireData.install(media);
                if (!player.capabilities.isCreativeMode)
                {
                    stack.shrink(1);
                }
                player.sendMessage(new TextComponentTranslation("ac.media.acquired", media.name()));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack)
    {
        return getMedia(stack.getItemDamage()).name();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag)
    {
        tooltip.add(getMedia(stack.getItemDamage()).desc());
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (tab == AcademyCraft.cct) {
            List<Media> medias = scala.collection.JavaConversions.seqAsJavaList(MediaManager.internalMedias());
            for(int i=0;i<medias.size();i++)
            {
                items.add(new ItemStack(this, 1, i));
            }
        }
    }

    private Media getMedia(int damage)
    {
        return MediaManager.allMedias().apply(damage);
    }
}
