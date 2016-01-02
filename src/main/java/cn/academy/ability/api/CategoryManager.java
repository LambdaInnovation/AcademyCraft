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
package cn.academy.ability.api;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;

/**
 * Handler of all category instances. You must register all the categories here.
 * @author WeAthFolD
 */
public class CategoryManager {

    public static CategoryManager INSTANCE = new CategoryManager();
    List<Category> catList = new ArrayList();
    
    private CategoryManager() {}
    
    public void register(Category cat) {
        catList.add(cat);
        cat.catID = catList.size() - 1;
    }
    
    public Category getCategory(int id) {
        return catList.get(id);
    }
    
    public List<Category> getCategories() {
        return ImmutableList.copyOf(catList);
    }
    
    public int getCategoryCount() {
        return catList.size();
    }
    
    public Category getCategory(String name) {
        //This is a very small list so looping is acceptable
        for(Category c : catList) {
            if(c.getName().equals(name))
                return c;
        }
        return null;
    }
    
}
