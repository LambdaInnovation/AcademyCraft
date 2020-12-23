package cn.academy.ability.context;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.ReflectionUtils;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;

/**
 * Mark this on a {@link ClientContext} class. That context class will work as a companion class that is only constructed at local/
 * client, but will receive all the context messages all the same. You can use this feature to reduce side dependency
 * issue (@SideOnly) and make code more clear.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegClientContext {
    Class<? extends Context> value();
}

@SideOnly(Side.CLIENT)
class RegClientContextImpl {

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        ReflectionUtils.getClasses(RegClientContext.class).forEach(type -> {
            RegClientContext anno = type.getAnnotation(RegClientContext.class);
            for (Constructor ctor : type.getDeclaredConstructors()) {
                if (ctor.getParameterCount() == 1 &&
                    Context.class.isAssignableFrom(ctor.getParameterTypes()[0])) {
                    ctor.setAccessible(true);
                    ClientContext.clientTypes.put(anno.value(), ctx -> {
                        try {
                            return (ClientContext) ctor.newInstance(ctx);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    return;
                }
            }

            throw new IllegalArgumentException("No appropriate constructor found for " + type);
        });
    }

}