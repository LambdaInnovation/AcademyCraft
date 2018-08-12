package cn.academy.ability;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

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