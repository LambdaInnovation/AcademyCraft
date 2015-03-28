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
package cn.academy.phone.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.academy.phone.registry.RegApp;

/**
 * @author WeathFolD
 *
 */
public class AppRegistry {
    
    public static AppRegistry instance = new AppRegistry();

    List<App> appList = new ArrayList();
    
    Map<Integer, List<App>> lvLists = new HashMap();
    
    public int regApp(App app) {
        appList.add(app);
        for(int i = 0; i <= app.level; ++i) {
            lazy(i).add(app);
        }
        System.out.println("Regged app " + app.name);
        app.id = appList.size() - 1;
        return appList.size() - 1;
    }
    
    public App getApp(int id) {
        return appList.get(id);
    }
    
    public List<App> getAppListFor(int lv) {
        return lazy(lv);
    }
    
    public boolean canInstall(int lv, App app) {
        return getAppListFor(lv).contains(app);
    }
    
    private List<App> lazy(int lv) {
        List<App> ret = lvLists.get(lv);
        if(ret == null) {
            ret = new ArrayList();
            lvLists.put(lv, ret);
        }
        return ret;
    }

}
