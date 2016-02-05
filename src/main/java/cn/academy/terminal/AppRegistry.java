/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
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

    public static int size() {
        return appList.size();
    }

    public static List<App> enumeration() {
        return ImmutableList.copyOf(appList);
    }

    private AppRegistry() {}

}
