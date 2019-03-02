package cn.academy.ability;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Debug;
import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handler of all category instances. You must register all the categories here.
 * @author WeAthFolD
 */
public class CategoryManager {

    public static CategoryManager INSTANCE = new CategoryManager();

    @StateEventCallback
    private static void postInit(FMLPostInitializationEvent ev) {
         INSTANCE.bake();
    }

    private final List<Category> catList = new ArrayList<>();

    private boolean _baked = false;
    
    private CategoryManager() {}
    
    public void register(Category cat) {
        Debug.require(!_baked, "CategoryManager.registry() can only be called before postInit");
        catList.add(cat);
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
        for(Category c : catList) {
            if(c.getName().equals(name))
                return c;
        }
        return null;
    }

    private void bake() {
        _baked = true;
        catList.sort(Comparator.comparing(it -> it.getName()));
        for (int idx = 0; idx < catList.size(); ++idx) {
            catList.get(idx).catID = idx;
        }
    }
    
}