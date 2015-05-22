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
package cn.academy.core.registry;

import java.io.IOException;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;

import cn.liutils.loading.item.ItemLoader;
import cn.liutils.util.GenericUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * @author WeAthFolD
 */
public class LoaderHelper {

	private static JsonObject defaultItem;
	private static final JsonParser parser = new JsonParser();
	
	public static ItemLoader createItemLoader() {
		if(defaultItem == null) {
			try {
				defaultItem = (JsonObject) parser.parse(IOUtils.toString(
					GenericUtils.getResourceStream(new ResourceLocation("academy:items_default.json"))
				));
			} catch (JsonSyntaxException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		ItemLoader loader = new ItemLoader();
		loader.feed(defaultItem);
		
		return loader;
	}
	
	
}
