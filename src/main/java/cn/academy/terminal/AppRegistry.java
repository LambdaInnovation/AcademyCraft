package cn.academy.terminal;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class AppRegistry {

    private static List<App> appList = new ArrayList<>();

    public static void register(App app) {
        appList.add(app);
        app.appid = appList.size() - 1;
    }

    public static App get(int id) {
        return appList.get(id);
    }

    public static App getByName(String name) {
        for (App app : appList) {
            if (app.getName().equals(name))
                return app;
        }
        return null;
    }

    public static int size() {
        return appList.size();
    }

    public static List<App> enumeration() {
        return ImmutableList.copyOf(appList);
    }

    private AppRegistry() {}

}