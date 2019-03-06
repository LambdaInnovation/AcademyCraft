package cn.academy.terminal;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.ReflectionUtils;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Comparator;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegApp {
    int priority() default 0;
}

class RegAppImpl {
    @StateEventCallback
    private static void preInit(FMLPreInitializationEvent ev) {
        ReflectionUtils.getFields(RegApp.class)
            .stream()
            .sorted(Comparator.comparingInt(it -> -it.getAnnotation(RegApp.class).priority()))
            .forEach(field -> {
                field.setAccessible(true);
                try {
                    AppRegistry.register((App) field.get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
    }
}