package cn.academy.util;

import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.ReflectionUtils;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegACKeyHandler {
    String name();
    int keyID();
    String comment() default "";
}

@SideOnly(Side.CLIENT)
class RegACKeyHandlerImple {
    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        ReflectionUtils.getFields(RegACKeyHandler.class).forEach(field -> {
            field.setAccessible(true);
            RegACKeyHandler anno = field.getAnnotation(RegACKeyHandler.class);
            try {
                ACKeyManager.instance.addKeyHandler(anno.name(), anno.comment(), anno.keyID(), (KeyHandler) field.get(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
}