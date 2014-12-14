/**
 * 
 */
package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.List;

import cn.liutils.api.util.GenericUtils;

/**
 * @author WeathFolD
 *
 */
public class Abilities {

	private static List<Category> catList = new ArrayList<Category>();
	
	public static int getCategories() {
		return catList.size();
	}
	
	public static Category getCategory(int caid) {
		return GenericUtils.safeFetchFrom(catList, caid);
	}
	
	public static void register(Category cat) {
		cat.catid = catList.size();
		catList.add(cat);
	}
	
}