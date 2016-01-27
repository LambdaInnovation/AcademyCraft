/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.media;

import cn.academy.core.client.Resources;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 */
public class Media {

    int id;
    
    final String name;
    final ResourceLocation cover;
    final int length; // Length in seconds.
    
    public Media(String _name, int _length) {
        name = _name;
        length = _length;
        cover = Resources.getTexture("media/" + name + "_cover");
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return StatCollector.translateToLocal("ac.media." + name + ".name");
    }
    
    public String getDesc() {
        return StatCollector.translateToLocal("ac.media." + name + ".desc");
    }
    
    public String getLengthStr() {
        return getPlayingTime(length);
    }
    
    public static String getPlayingTime(int seconds) {
        int a = seconds / 60, b = seconds % 60;
        return String.format((a < 10 ? "0" : "") + a + ":" + (b < 10 ? "0" : "") + b);
    }

}
