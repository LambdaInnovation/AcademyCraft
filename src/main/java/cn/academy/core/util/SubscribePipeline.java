/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark this on a method inside a PipelineListener. Register this method to the pipeline.
 * A pipeline subscriber must has the following signature: <br/>
 * <code>
 * public [type] someMethod([type] value, AnyType par1, AnyType par2, ...) {}
 * </code>
 * <br/>
 * Currently supported types are int, float, double.
 * The given parameters are matched into subscriber methods in order. If the pars doesn't match, 
 *     ValuePipeline will trigger an warning.
 * @author WeAthFolD
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribePipeline {
    /**
     * @return The key name that this process method wants to listen to.
     */
    String value();
}
