package ac.academy.buff;

import java.util.ArrayList;

import cn.lambdalib.util.datapart.DataPart;
import net.minecraft.nbt.NBTTagCompound;

public class BuffDataPart extends DataPart {
	ArrayList<Buff> activedBuff = new ArrayList<Buff>();
	
	@Override
	public void fromNBT(NBTTagCompound tag) {
		activedBuff.stream().forEach(buff->{
			buff.fromNBTTag(tag.getCompoundTag(buff.getType().id));
		});
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		activedBuff.stream().forEach(buff->{
			nbt.setTag(buff.getType().id, buff.toNBTTag());
		});
		return nbt;
	}
}
