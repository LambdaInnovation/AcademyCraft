package cn.academy.datapart;

import cn.academy.ability.Controllable;
import cn.academy.event.ability.CategoryChangeEvent;
import cn.academy.event.ability.PresetUpdateEvent;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.mc.RegEventHandler;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.nbt.NBTS11n.BaseSerializer;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Handles preset.
 * @author WeAthFolD
 */
@RegDataPart(EntityPlayer.class)
public class PresetData extends DataPart<EntityPlayer> {

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        NBTS11n.addBase(Preset.class, new BaseSerializer<NBTBase, Preset>() {
            @Override
            public NBTBase write(Preset value) {
                NBTTagCompound tag = new NBTTagCompound();
                IntStream.range(0, MAX_KEYS).forEach(idx -> {
                    Controllable ctrl = value.data[idx];
                    if (ctrl != null) {
                        tag.setTag(String.valueOf(idx), NBTS11n.writeBase(ctrl, Controllable.class));
                    }
                });
                return tag;
            }
            @Override
            public Preset read(NBTBase tag_, Class<? extends Preset> type) {
                NBTTagCompound tag = (NBTTagCompound) tag_;

                Controllable[] data = new Controllable[MAX_KEYS];
                IntStream.range(0, MAX_KEYS).forEach(idx -> {
                    String tagName = String.valueOf(idx);
                    if (tag.hasKey(tagName)) {
                        data[idx] = NBTS11n.readBase(tag.getTag(tagName), Controllable.class);
                    }
                });

                return new Preset(data);
            }
        });

        NetworkS11n.addDirect(Preset.class, new NetS11nAdaptor<Preset>() {
            @Override
            public void write(ByteBuf buf, Preset obj) {
                int count = (int) IntStream.range(0, MAX_KEYS).filter(idx -> obj.hasMapping(idx)).count();
                buf.writeByte(count);

                IntStream.range(0, MAX_KEYS).forEach(idx -> {
                    if (obj.hasMapping(idx)) {
                        buf.writeByte(idx);
                        NetworkS11n.serializeWithHint(buf, obj.getControllable(idx), Controllable.class);
                    }
                });
            }
            @Override
            public Preset read(ByteBuf buf) throws ContextException {
                Preset ret = new Preset();
                int count = buf.readByte();
                while (count-- > 0) {
                    int id = buf.readByte();
                    ret.data[id] = NetworkS11n.deserializeWithHint(buf, Controllable.class);
                }
                return ret;
            }
        });
    }

    public static final int MAX_KEYS = 4;
    public static final int MAX_PRESETS = 4;

    private static final String
        MSG_SYNC_SWITCH = "switch",
        MSG_SYNC_UPDATE = "update";

    @SerializeIncluded
    int presetID = 0;
    @SerializeIncluded
    Preset[] presets = new Preset[4];

    public PresetData() {
        for(int i = 0; i < MAX_PRESETS; ++i) {
            presets[i] = new Preset();
        }

        setNBTStorage();
        setClientNeedSync();
    }

    // Modifier

    public void clear() {
        checkSide(Side.SERVER);

        for(int i = 0; i < 4; ++i)
            presets[i] = new Preset();

        sync();
    }

    public void setPreset(int id, Preset p) {
        checkSide(Side.SERVER);

        presets[id] = p;
        sync();
    }

    public void switchCurrent(int nid) {
        Preconditions.checkElementIndex(nid, MAX_PRESETS);
        checkSide(Side.SERVER);

        presetID = nid;
        sync();
    }

    // Cross-network

    public void switchFromClient(int id) {
        Preconditions.checkElementIndex(id, MAX_PRESETS);
        checkSide(Side.CLIENT);

        presetID = id;
        sendMessage(MSG_SYNC_SWITCH, id);
    }

    public void setPresetFromClient(int id, Preset p) {
        checkSide(Side.CLIENT);

        presets[id] = p;
        sendMessage(MSG_SYNC_UPDATE, id, p);
        firePresetUpdate();
    }

    //

    // Observer

    public Preset getPreset(int id) {
        return presets[id];
    }

    public int getCurrentID() {
        return presetID;
    }
    
    public Preset getCurrentPreset() {
        return presets[presetID];
    }

    //

    @Override
    public void fromNBT(NBTTagCompound tag) {
        NBTS11n.read(tag, this);
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }

    @Listener(channel=MSG_SYNC_SWITCH, side=Side.SERVER)
    private void handleSwitch(int idx) {
        switchCurrent(idx);
    }

    @Listener(channel=MSG_SYNC_UPDATE, side=Side.SERVER)
    private void handleSet(int idx, Preset mapping) {
        setPreset(idx, mapping);
        firePresetUpdate();
    }

    @Override
    protected void onSynchronized() {
        debug("OnSynchronized " + isClient() + " " + getCurrentPreset());
        firePresetUpdate();
    }

    private void firePresetUpdate() {
        MinecraftForge.EVENT_BUS.post(new PresetUpdateEvent(getEntity()));
    }

    public static PresetData get(EntityPlayer player) {
        return EntityData.get(player).getPart(PresetData.class);
    }

    public static class Preset {

        private final Controllable[] data;
        
        public Preset(Controllable[] _data) {
            data = Arrays.copyOf(_data, MAX_KEYS);
        }

        public Preset() {
            data = new Controllable[MAX_KEYS];
            Arrays.fill(data, null);
        }
        
        public boolean hasMapping(int key) {
            return getControllable(key) != null;
        }

        /**
         * @return The controllable that maps to this key, or null if not present
         */
        public Controllable getControllable(int key) {
            return key >= data.length ? null : data[key];
        }
        
        public boolean hasControllable(Controllable c) {
            for (Controllable cc : data) {
                if (cc == c) {
                    return true;
                }
            }
            return false;
        }

        public Controllable[] copyData() {
            return Arrays.copyOf(data, MAX_KEYS);
        }
        
        @Override
        public String toString() {
            ToStringHelper helper = MoreObjects.toStringHelper(this);

            for (int i = 0; i < data.length; ++i) {
                helper.add("#" + i, data[i]);
            }

            return helper.toString();
        }
        
    }

    public static enum Events {
        @RegEventHandler()
        instance;
        
        @SubscribeEvent
        public void onCategoryChanged(CategoryChangeEvent event) {
            if (!SideUtils.isClient()) {
                PresetData.get(event.player).clear();
            }
        }
        
    }

}