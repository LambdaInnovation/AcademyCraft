package cn.academy.medicine.api;

import net.minecraft.nbt.NBTTagCompound;

public abstract class BuffPerTick extends Buff {
    protected float perTick=0;

    public BuffPerTick(String id) {
        super(id);
    }

    @Override
    public void load(NBTTagCompound tag){
        perTick = tag.getFloat("amt");
    }

    @Override
    public void store(NBTTagCompound tag){
        tag.setFloat("amt", perTick);
    }

}
