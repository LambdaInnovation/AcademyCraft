package cn.academy.terminal.app;

import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import cn.lambdalib2.util.Debug;

public class AppAbout extends App {

//    @RegApp(priority = -2)
    private static final AppAbout instance = new AppAbout();

    public AppAbout() {
        super("about");
        setPreInstalled();
    }

    @Override
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            public void onStart() {
                Debug.log("Hello about app");
            }
        };
    }
}
