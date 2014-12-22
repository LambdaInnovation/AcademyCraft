/**
 * 
 */
package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.academy.core.AcademyCraftMod;
import cn.liutils.api.util.GenericUtils;

/**
 * @author WeathFolD
 *
 */
//TODO move this class to core.
public class Abilities {

	private static List<Category> catList = new ArrayList<Category>();
	
	public static int getCategories() {
		return catList.size();
	}
	
	public static Category getCategory(int caid) {
		Category cat = GenericUtils.safeFetchFrom(catList, caid);
		if (cat == null) {
			AcademyCraftMod.log.error("Trying to get a category that does not exist.");
			return catList.get(0);
		}
		return cat;
	}
	
	public static void register(Category cat) {
		cat.catId = catList.size();
		catList.add(cat);
	}

	
	/*
	 * Registry Part 
	 */
	public static final SkillBase skillEmpty = new SkillBase();
	public static final SkillDebug skillDebug = new SkillDebug();
	public static final SkillHoldTest skillHoldTest = new SkillHoldTest();
	
	private static final Category catEmpty = 
			new Category(Arrays.<SkillBase>asList(skillEmpty, skillDebug, skillHoldTest))
			{
				{
					this.addLevel(new Level(this));
				}
			};
	
	static {
		register(catEmpty);
	}
}