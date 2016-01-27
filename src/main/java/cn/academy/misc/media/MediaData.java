/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("media")
public class MediaData extends DataPart {
    
    BitSet learned = new BitSet(32);
    
    public MediaData() {}
    
    public static MediaData get(EntityPlayer player) {
        return EntityData.get(player).getPart(MediaData.class);
    }
    
    /**
     * Should be called in SERVER only. Install the media to the player.
     * @return Whether the media is successfully installed
     */
    public boolean installMedia(int mediaID) {
        if(isRemote())
            throw new RuntimeException("Wrong side");
        
        if(learned.get(mediaID))
            return false;
        
        learned.set(mediaID);
        
        sync();
        return true;
    }
    
    public boolean isMediaInstalled(int mediaID) {
        return learned.get(mediaID);
    }
    
    public List<Media> getInstalledMediaList() {
        List<Media> ret = new ArrayList();
        for(int i = 0; i < MediaRegistry.getMediaCount(); ++i)
            if(isMediaInstalled(i))
                ret.add(MediaRegistry.getMedia(i));
        return ret;
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        learned = BitSet.valueOf(tag.getByteArray("l"));
    }

    @Override
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByteArray("l", learned.toByteArray());
        
        return tag;    
    }

}
