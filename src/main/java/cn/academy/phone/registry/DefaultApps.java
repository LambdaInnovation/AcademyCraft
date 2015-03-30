package cn.academy.phone.registry;

import cn.academy.phone.app.App;
import cn.academy.phone.app.TestAppGui;
import cn.annoreg.core.RegistrationClass;

/**
 * This class is just for testing.
 * @author WeathFolD
 */
@RegistrationClass
public class DefaultApps {

    @RegApp
    public static App
        appRecord = new App("record", TestAppGui.class, 0),
        appSkillTree = new App("skillTree", TestAppGui.class, 0),
        appSettings = new App("settings", TestAppGui.class, 0);

}
