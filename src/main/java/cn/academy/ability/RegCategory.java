package cn.academy.ability;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.ReflectionUtils;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegCategory {}

class RegCategoryImpl {

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        ReflectionUtils.getFields(RegCategory.class).forEach(field -> {
            Debug.assert2(Modifier.isStatic(field.getModifiers()), "Field must be static");
            try {
                Object obj = field.get(null);
                CategoryManager.INSTANCE.register((Category) obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}