/**
 * 
 */
package cn.academy.api.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.academy.core.AcademyCraft;
import cn.liutils.util.GenericUtils;

/**
 * Static ability&skill data management. All the handling class are stored and queried here.
 * @author WeathFolD
 */
public class Abilities {

	private static List<Category> catList = new ArrayList<Category>();
	private static Map<String, SkillBase> skillMap = new HashMap<String, SkillBase>();
	
	public static void registerCat(Category cat) {
		cat.catId = catList.size();
		catList.add(cat);
	}
	
	public static Category getCategory(int caid) {
		return GenericUtils.assertObj(GenericUtils.safeFetchFrom(catList, caid));
	}
	
	public static int getCategoryCount() {
		return catList.size();
	}
	
	public static void registerSkill(Collection<SkillBase> skls) {
		for(SkillBase skl : skls) {
			registerSkill(skl);
		}
	}
	
	/**
	 * Register a skill into global list. This should be
	 * done for all normal skills (In case of cross-ability skill accessing)
	 */
	public static void registerSkill(SkillBase skill) {
		skillMap.put(skill.getInternalName(), skill);
	}
	
	public static SkillBase getSkill(String name) {
		return skillMap.get(name);
	}
	
	/*
	 * Registry Part 
	 */
	public static final SkillBase skillEmpty = new SkillBase();
	public static final SkillDebug skillDebug = new SkillDebug();
	public static final SkillHoldTest skillHoldTest = new SkillHoldTest();
	
	public static final Category catEmpty = new Category();
	
	static {
		registerCat(catEmpty);
	}
}