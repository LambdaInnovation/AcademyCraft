package cn.academy.medicine;


import cn.lambdalib.util.mc.StackUtils;
import com.google.common.base.Preconditions;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Handles medicine synthesizing logic.
 *
 * TODO Debug display for medicine data
 *
 */
public class MedSynth {

    public static void writeApplyInfo(ItemStack stack, MedicineApplyInfo info){
        NBTTagCompound tag0 = StackUtils.loadTag(stack);
        NBTTagCompound tag = new NBTTagCompound();
        tag0.setTag("medicine", tag);
        tag.setInteger("target", Properties.instance.writeTarget(info.target));
        tag.setInteger("strengthType", Properties.instance.writeStrength(info.strengthType));
        tag.setFloat("strengthMod", info.strengthModifier);
        tag.setInteger("method", Properties.instance.writeMethod(info.method));
        tag.setFloat("sens", info.sensitiveRatio);
    }

    public static MedicineApplyInfo readApplyInfo(ItemStack stack){

        if(Preconditions.checkNotNull(StackUtils.loadTag(stack)).getTag("medicine") instanceof NBTTagCompound){
            NBTTagCompound tag = (NBTTagCompound) StackUtils.loadTag(stack).getTag("medicine");
            Properties.Target target = Properties.instance.readTarget(tag.getInteger("target"));
            Properties.Strength strengthType = Properties.instance.readStrength(tag.getInteger("strengthType"));
            float strengthMod=tag.getFloat("strengthMod");
            Properties.ApplyMethod method= Properties.instance.readMethod(tag.getInteger("method"));
            float sensitiveRatio = tag.getFloat("sens");
            return new MedicineApplyInfo(target, strengthType, strengthMod, method, sensitiveRatio);
        }
        throw new IllegalArgumentException("Invalid stack tag to read medicine info");
    }

}
