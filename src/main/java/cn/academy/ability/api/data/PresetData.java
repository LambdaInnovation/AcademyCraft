/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.data;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.event.CategoryChangeEvent;
import cn.academy.ability.api.event.PresetUpdateEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("preset")
public class PresetData extends DataPart<EntityPlayer> {

    @Deprecated
    public static final int MAX_KEYS = 8;

    public static final int MAX_PRESETS = 4;
    
    int presetID = 0;
    Preset[] presets = new Preset[4];
    
    /*
     * Notify: Unlike normal DataParts, PresetData is
     * client-major after the initial creation.
     */
    
    public PresetData() {
        for(int i = 0; i < MAX_PRESETS; ++i) {
            presets[i] = new Preset();
        }
    }
    
    private AbilityData getAbilityData() {
        return AbilityData.get(getEntity());
    }
    
    public void clear() {
        endOverride();
        for(int i = 0; i < 4; ++i)
            presets[i] = new Preset();
        if(!isRemote())
            sync();
    }
    
    /**
     * Create a instance that have capability to edit a fixed preset.
     */
    public PresetEditor createEditor(int presetID) {
        return new PresetEditor(presets[presetID]);
    }
    
    public Preset getPreset(int id) {
        return presets[id];
    }

    public void switchCurrent(int nid) {
        presetID = nid;
        sync();
    }
    
    public int getCurrentID() {
        return presetID;
    }
    
    public Preset getCurrentPreset() {
        if(!isActive()) {
            return null;
        }
        return presets[presetID];
    }
    
    @Override
    public void tick() {}

    @Override
    public void fromNBT(NBTTagCompound tag) {
        presetID = tag.getByte("cur");
        for(int i = 0; i < MAX_PRESETS; ++i) {
            presets[i].fromNBT(tag.getCompoundTag("" + i));
        }
        
        MinecraftForge.EVENT_BUS.post(new PresetUpdateEvent(getEntity()));
    }

    @Override
    public NBTTagCompound toNBT() {
        return toNBTGeneric(false);
    }
    
    @Override
    public NBTTagCompound toNBTSync() {
        return toNBTGeneric(true);
    }
    
    public NBTTagCompound toNBTGeneric(boolean sync) {
        NBTTagCompound ret = new NBTTagCompound();
        ret.setByte("cur", (byte) presetID);
        for(int i = 0; i < MAX_PRESETS; ++i) {
            ret.setTag("" + i, presets[i].toNBT());
        }
        return ret;
    }
    
    public boolean isActive() {
        return getAbilityData().isLearned();
    }
    
    public static PresetData get(EntityPlayer player) {
        return EntityData.get(player).getPart(PresetData.class);
    }
    
    public class PresetEditor {
        
        /**
         * NO DIRECT EDITING
         */
        public final byte display[] = new byte[MAX_KEYS];
        
        public final Preset target;
        
        public PresetEditor(Preset _target) {
            target = _target;
            for(int i = 0; i < MAX_KEYS; ++i) {
                display[i] = target.data[i];
            }
        }
        
        public void edit(int key, int newMapping) {
            display[key] = (byte) newMapping;
        }
        
        /**
         * Return whether current edit state has the cid specified as mapping.
         */
        public boolean hasMapping(int cid) {
            for(byte b : display)
                if(b == cid)
                    return true;
            
            return false;
        }
        
        public boolean hasChanged() {
            for(int i = 0; i < MAX_KEYS; ++i) {
                if(display[i] != target.data[i])
                    return true;
            }
            return false;
        }
        
        public void save() {
            target.setData(display);
            MinecraftForge.EVENT_BUS.post(new PresetUpdateEvent(getEntity()));
            
            sync();
        }
        
    }
    
    /**
     * @return A preset that is in the scope of the same Player. 
     * Probably used for overriding control.
     */
    @Deprecated
    public Preset createPreset() {
        return new Preset();
    }

    @Deprecated
    public boolean isOverriding() {
        return false;
    }
    @Deprecated
    public void override(Preset special) {}
    @Deprecated
    public void endOverride() {}

    public class Preset {
        
        /**
         * Warning: Direct edit will cause sync loss.
         */
        public final byte data[] = new byte[MAX_KEYS];
        
        public Preset() {
            for(int i = 0; i < MAX_KEYS; ++i) {
                data[i] = -1;
            }
        }
        
        public Preset(byte[] _data) {
            setData(_data);
        }
        
        void setData(byte[] _data) {
            for(int i = 0; i < MAX_KEYS; ++i) {
                data[i] = _data[i];
            }
        }
        
        public byte[] getData() {
            return data;
        }
        
        public boolean hasMapping(int key) {
            return getControllable(key) != null;
        }
        
        public Controllable getControllable(int key) {
            int mapping = data[key];
            if(mapping == -1) {
                return null;
            }
            AbilityData data = getAbilityData();
            Category cat = data.getCategory();
            if(cat == null) return null;
            return cat.getControllable(mapping);
        }
        
        public boolean hasControllable(Controllable c) {
            AbilityData adata = getAbilityData();
            Category cat = adata.getCategory();
            if(cat == null)
                return false;
            for(byte b : data) {
                if(cat.getControllable(b) == c)
                    return true;
            }
            return false;
        }
        
        NBTTagCompound toNBT() {
            NBTTagCompound ret = new NBTTagCompound();
            ret.setByteArray("l", data);
            return ret;
        }
        
        void fromNBT(NBTTagCompound tag) {
            byte[] d = tag.getByteArray("l");
            for(int i = 0; i < MAX_KEYS; ++i) {
                data[i] = d[i];
            }
        }
        
        public String formatDetail() {
            StringBuilder sb = new StringBuilder();
            Category cat = AbilityData.get(getEntity()).getCategory();
            List<Controllable> ctrlList = cat.getControllableList();
            
            for(int i = 0; i < MAX_KEYS; ++i) {
                Controllable c = null;
                if(data[i] != -1) {
                    c = ctrlList.get(data[i]);
                }
                if(c != null) {
                    sb.append(i + " => " + c.toString() + "(" + data[i] + ")\n");
                }
            }
            return sb.toString();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Preset[").append(getEntity().getCommandSenderName()).append("] <");
            for(int i = 0; i < MAX_KEYS; ++i) {
                sb.append(data[i]).append(i == MAX_KEYS - 1 ? ">" : ",");
            }
            return sb.toString();
        }
        
        private PresetData getPData() {
            return PresetData.this;
        }
        
    }
    
    @RegEventHandler(Bus.Forge)
    public static class Events {
        
        @SubscribeEvent
        public void onCategoryChanged(CategoryChangeEvent event) {
            PresetData.get(event.player).clear();
        }
        
    }

}
