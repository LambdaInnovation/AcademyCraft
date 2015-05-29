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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import cn.academy.core.AcademyCraft;
import cn.liutils.loading.Loader;
import cn.liutils.loading.block.BlockLoader;
import cn.liutils.loading.item.ItemLoader;

/**
 * Util class that eject all the instances in Loader into public static fields.
 * @author WeAthFolD
 */
public class InstanceEjector {
	
	/**
	 * Used to indicate that a instance needs to be ejected from an item loader or block loader.
	 * The type depends on whether it's a Item or Block.
	 * @author WeAthFolD
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface FromLoader {
		/**
		 * If you use the default value(""), the field will be converted (from lowerCamel) into under_line naming rule.
		 * @return The key to lookup the object in the loader
		 */
		String value() default "";
	}
	
	/**
	 * Eject all the item instances within the ItemLoader into the {@code public static Item xxx;} fields
	 * with @FromLoader annotation.
	 * @param obj
	 * @param loader
	 */
	public static void fromItemLoader(Class clazz, ItemLoader loader) {
		fromLoader(clazz, loader, Item.class);
	}
	
	public static void fromBlockLoader(Class clazz, BlockLoader loader) {
		fromLoader(clazz, loader, Block.class);
	}
	
	private static void fromLoader(Class clazz, Loader loader, Class baseClass) {
		for(Field f : clazz.getFields()) {
			if(f.isAnnotationPresent(FromLoader.class) && 
				baseClass.isAssignableFrom(f.getType())) {
				String name = f.getAnnotation(FromLoader.class).value();
				if(name.equals(""))
					name = toUnderline(f.getName());
				Object item = loader.getObject(name);
				if(item == null) {
					AcademyCraft.log.error("Error ejecting instance " + name + " to " + clazz + ": Object not found");
				} else {
					try {
						f.set(null, item);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Convert from lowerCamel to under_line naming rule.
	 */
	private static String toUnderline(String str) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);
			if(Character.isUpperCase(ch)) {
				sb.append('_').append(Character.toLowerCase(ch));
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
}
