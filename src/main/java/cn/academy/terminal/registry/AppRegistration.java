/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.registry;

import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.registry.AppRegistration.RegApp;
import cn.lambdalib.annoreg.base.RegistrationFieldSimple;
import cn.lambdalib.annoreg.core.LoadStage;
import cn.lambdalib.annoreg.core.RegistryTypeDecl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RegistryTypeDecl
public class AppRegistration extends RegistrationFieldSimple<RegApp, App> {

    public AppRegistration() {
        super(RegApp.class, "ac_App");
        setLoadStage(LoadStage.PRE_INIT);
    }

    /**
     * Register an App field into AppRegistry.
     * @author WeAthFolD
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface RegApp {}

    @Override
    protected void register(App value, RegApp anno, String field)
            throws Exception {
        AppRegistry.INSTANCE.register(value);
    }
    
}
