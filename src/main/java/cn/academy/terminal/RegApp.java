package cn.academy.terminal;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.ReflectionUtils;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegApp {
}

class RegAppImpl {
    @StateEventCallback
    private static void preInit(FMLPreInitializationEvent ev) {
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