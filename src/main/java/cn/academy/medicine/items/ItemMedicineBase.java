package cn.academy.medicine.items;

import cn.academy.core.item.ACItem;
import cn.academy.medicine.MedSynth;
import cn.academy.medicine.MedicineApplyInfo;
import cn.academy.medicine.Properties;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.List;

public class ItemMedicineBase extends ACItem {
    public ItemMedicineBase(String name){
        super(name);
        setMaxStackSize(1);
    }

    public MedicineApplyInfo getInfo(ItemStack stack){
        return MedSynth.readApplyInfo(stack);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list2, boolean wtf){
        List<String> list = list2;
        MedicineApplyInfo info = getInfo(stack);

        if (info.target != Properties.instance.Targ_Disposed) {
            list.add(info.target.displayDesc() + " " + info.method.displayDesc());
            list.add(info.strengthType.displayDesc());
        } else {
            list.add(EnumChatFormatting.RED + Properties.instance.Targ_Disposed.displayDesc());
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player){
        MedicineApplyInfo info = getInfo(stack);

        if (!world.isRemote) {
            info.target.apply(player, info);
        }

        if (!player.capabilities.isCreativeMode) {
            stack.stackSize -= 1;
        }

        return stack;
    }
}

