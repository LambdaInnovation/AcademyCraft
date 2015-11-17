package ac.academy.buff;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraftforge.common.util.Constants.NBT;

public class Buff {
	private final BuffType type;
	private int duration;
	private int level;
	private boolean isDurationForever;
	
	private Buff(BuffType type,int level,int durationTick,boolean isForever){
		this.type = type;
		this.level = level;
		this.duration = durationTick;
		this.isDurationForever = isForever;
	}
	
	public Buff(BuffType type,int level,int durationTick) {
		this(type,level,durationTick,false);
	}
	
	public Buff(BuffType type,int level) {
		this(type,level,0,true);
	}
	
	public BuffType getType(){
		return this.type;
	}
	
	public boolean isForever() {
		return this.isDurationForever;
	}

	public int getDuration() {
		return this.duration;
	}
	
	public void setDuration(int durationTick) {
		this.duration = durationTick;
		this.isDurationForever = false;
	}
	
	public void setDurationForever() {
		this.isDurationForever=true;
		this.duration=0;
	}
	
	public boolean onUpdate(EntityLivingBase entity) {
        if (this.duration>0) {
            if (this.type.isThisTickReady(this.duration,this.level)) {
                this.type.performEffect(entity, level);
            }
            this.duration--;
        }
        return this.duration>0;
    }
	
	public NBTTagCompound toNBTTag(){
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setBoolean("isForever", Boolean.valueOf(isDurationForever));
		nbt.setByte("level", Byte.valueOf((byte) level));
		nbt.setInteger("duration", Integer.valueOf(this.duration));
		return nbt;
	}
	
	public void fromNBTTag(NBTTagCompound nbt){
		this.isDurationForever=nbt.getBoolean("isForever");
		this.level = nbt.getByte("level");
		this.duration = nbt.getInteger("duration");
	}
}
