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
package cn.academy.core;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.loading.item.ItemLoader;
import cn.liutils.util.GenericUtils;

/**
 * All registration of item goes here.
 * @author WeathFolD
 */
@Registrant
@RegSubmoduleInit
public class ACItems {
	
	public static ItemLoader items;
    
    public static void init() {
    	try {
	    	items = new ItemLoader();
	    	String json = IOUtils.toString(GenericUtils.getResourceStream(new ResourceLocation("academy:items.json")));
	    	
	    	items.feed(json);
	    	items.loadAll();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public static <T extends Item> T getItem(String name) {
    	return (T) items.getObject(name);
    }

}
