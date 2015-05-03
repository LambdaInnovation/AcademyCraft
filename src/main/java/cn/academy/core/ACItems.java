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

import org.apache.commons.io.IOUtils;

import cn.academy.phone.item.ItemPhone;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.loading.item.ItemLoader;

/**
 * All registration of item goes here.
 * @author WeathFolD
 */
@RegistrationClass
@RegSubmoduleInit
public class ACItems {

    @RegItem
    @RegItem.UTName("phone_low")
    public static ItemPhone phoneLowend = new ItemPhone(1, 10000);
    
    public static void init() {
    	try {
	    	ItemLoader loader = new ItemLoader();
	    	String json = IOUtils.toString(ACItems.class.getResource("/assets/liutils/test.json"));
	    	System.out.println("Json is: ");
	    	System.out.println(json);
	    	
	    	loader.feed(json);
	    	loader.loadAll();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }

}
