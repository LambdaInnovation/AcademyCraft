/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.RegSerializable.SerializeField;
import cn.lambdalib.s11n.SerializeIncluded;
import cn.lambdalib.s11n.nbt.NBTS11n;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart(EntityPlayer.class)
public class MediaData extends DataPart {

    @SerializeIncluded
    private BitSet learned = new BitSet(32);
    
    public MediaData() {}
    
    public static MediaData get(EntityPlayer player) {
        return EntityData.get(player).getPart(MediaData.class);
    }
    
    /**
     * Should be called in SERVER only. Install the media to the player.
     * @return Whether the media is successfully installed
     */
    public boolean installMedia(int mediaID) {
        checkSide(Side.CLIENT);
        
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
        List<Media> ret = new ArrayList<>();
        for(int i = 0; i < MediaRegistry.getMediaCount(); ++i)
            if(isMediaInstalled(i))
                ret.add(MediaRegistry.getMedia(i));
        return ret;
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        NBTS11n.read(tag, this);
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }

}
