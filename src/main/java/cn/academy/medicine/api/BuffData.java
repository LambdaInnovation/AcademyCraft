package cn.academy.medicine.api;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.helper.TickScheduler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import scala.reflect.ClassTag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@Registrant
@RegDataPart(value=EntityPlayer.class)
public class BuffData extends DataPart<EntityPlayer> {
    class ClientFakeBuff extends Buff {
        @Override
        public void onBegin(EntityPlayer player) {

        }

        @Override
        public void onTick(EntityPlayer player, BuffApplyData applyData) {

        }

        @Override
        public void onEnd(EntityPlayer player) {

        }

        @Override
        public void load(NBTTagCompound tag) {

        }

        @Override
        public void store(NBTTagCompound tag) {

        }

        public ClientFakeBuff(String id) {
            super(id);
        }
    }
    public class BuffRuntimeData {
        public Buff buff;
        public BuffApplyData applyData;
        public BuffRuntimeData(Buff buff, BuffApplyData data)
        {
            this.buff=buff;
            this.applyData=data;
        }
    }


    private List<BuffRuntimeData> activeBuffs = new ArrayList<>();

    private TickScheduler scheduler = new TickScheduler();

    public BuffData(){
        setClearOnDeath();
        setClientNeedSync();
        setNBTStorage();
        setTick(true);
        scheduler.every(10).atOnly(Side.SERVER).run(this::sync);

        scheduler.everyTick().atOnly(Side.SERVER).run(() -> {
                EntityPlayer player = getEntity();
                Iterator iter = activeBuffs.iterator();
                while (iter.hasNext()) {
                    if(iter.next() instanceof BuffRuntimeData)
                    {
                        BuffRuntimeData data = (BuffRuntimeData)iter.next();
                        if (data.applyData.tickLeft > 0 || data.applyData.isInfinite()) {
                            if (!data.applyData.isInfinite()) {
                                data.applyData.tickLeft -= 1;
                            }
                            data.buff.onTick(player, data.applyData);
                        } else {
                            data.buff.onEnd(player);
                            iter.remove();
                        }
                    }
                }
            });
    }


    public void addBuffInfinite(Buff buff){
        addBuff(buff, -1);
    }

    public void addBuff(Buff buff, int maxTicks){
        checkSide(Side.SERVER);
        activeBuffs.add(new BuffRuntimeData(buff, new BuffApplyData(maxTicks, maxTicks)));

        sync();
    }

    public <T extends Buff> T findBuff(ClassTag<T> tag){
        for(BuffRuntimeData buff : activeBuffs)
        {
            if(tag.runtimeClass().isInstance(buff)){
                return (T)(buff.buff);
            }
        }
        return null;
    }

    public void tick(){
        scheduler.runTick();
    }

    public void fromByteBuf(ByteBuf buf){
        checkSide(Side.CLIENT);
        activeBuffs.clear();

        int count = buf.readInt();
        for(int i=0;i<count;i++){
            String id = ByteBufUtils.readUTF8String(buf);
            int tickLeft = buf.readInt();
            int maxTicks = buf.readInt();
            activeBuffs.add(new BuffRuntimeData(new ClientFakeBuff(id), new BuffApplyData(tickLeft, maxTicks)));
        }
    }

    public void toByteBuf(ByteBuf buf){
        checkSide(Side.SERVER);
        buf.writeInt(activeBuffs.size());
        for(BuffRuntimeData data:activeBuffs)
        {
            ByteBufUtils.writeUTF8String(buf, data.buff.id);
            buf.writeInt(data.applyData.tickLeft).writeInt(data.applyData.maxTicks());
        }
    }

    public void toNBT(NBTTagCompound tag){
        NBTTagList list = new NBTTagList();
        for(BuffRuntimeData data:activeBuffs)
        {
            NBTTagCompound tag_ = new NBTTagCompound();
            NBTTagCompound buffTag = new NBTTagCompound();
            BuffRegistry.writeBuff(data.buff, buffTag);
            tag_.setInteger("maxTicks", data.applyData.maxTicks());
            tag_.setInteger("tickLeft", data.applyData.tickLeft);
            tag_.setTag("buff", buffTag);

            list.appendTag(tag_);
        }
        tag.setTag("buff",list);
    }

    public void  fromNBT(NBTTagCompound tag){
        NBTBase tBase = tag.getTag("buff");
        if (tBase instanceof NBTTagList){
            for(int i=0;i<((NBTTagList) tBase).tagCount();i++)
            {
                NBTTagCompound tag_=((NBTTagList) tBase).getCompoundTagAt(i);
                NBTTagCompound buffTag = (NBTTagCompound) tag.getTag("buff");
                Buff buff = BuffRegistry.readBuff(buffTag);
                BuffApplyData applyData = new BuffApplyData(tag.getInteger("tickLeft"),
                        tag.getInteger("maxTicks"));
                activeBuffs.add(new BuffRuntimeData(buff, applyData));

            }
        }
    }

    public static BuffData apply(EntityPlayer player)
    {
        return EntityData.get(player).getPart(BuffData.class);
    }

}
