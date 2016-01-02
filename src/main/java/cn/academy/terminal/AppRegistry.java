package cn.academy.terminal;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class AppRegistry {

    public static AppRegistry INSTANCE = new AppRegistry();

    private AppRegistry() {
    }

    List<App> appList = new ArrayList();

    public void register(App app) {
        appList.add(app);
        app.appid = appList.size() - 1;
    }

    public App get(int id) {
        return appList.get(id);
    }

    public int size() {
        return appList.size();
    }

    public static List<App> enumeration() {
        return ImmutableList.copyOf(INSTANCE.appList);
    }

}
