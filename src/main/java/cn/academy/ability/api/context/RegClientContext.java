package cn.academy.ability.api.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
