package cn.academy.item;

import cn.academy.AcademyCraft;
import net.minecraft.item.Item;

/**
 * Base class for all hard-coded item in AC.
 * @author WeAthFolD
 */
public class ACItem extends Item {

    public ACItem(String name) {
        setUnlocalizedName("ac_" + name);
        // FIXME impl in json
//        setTextureName("academy:" + name);
        setCreativeTab(AcademyCraft.cct);
    }
    
}