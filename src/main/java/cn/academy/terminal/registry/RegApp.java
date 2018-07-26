package cn.academy.terminal.registry;

import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.lambdalib2.util.ReflectionUtils;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegApp {
}

class RegAppImpl {
    private static void init(FMLInitializationEvent ev) {
        ReflectionUtils.getFields(RegApp.class).forEach(field -> {
            field.setAccessible(true);
            try {
                AppRegistry.register((App) field.get(null));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }
}