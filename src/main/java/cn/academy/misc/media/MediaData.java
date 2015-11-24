/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.media;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;

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
