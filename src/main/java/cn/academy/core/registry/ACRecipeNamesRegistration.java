/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.registry;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.AnnotationData;
import cn.lambdalib.annoreg.core.LoadStage;
import cn.lambdalib.annoreg.core.RegistryType;
import cn.lambdalib.annoreg.core.RegistryTypeDecl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author WeAthFolD
 */
@RegistryTypeDecl
public class ACRecipeNamesRegistration extends RegistryType {
    
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RegACRecipeNames {}

    public ACRecipeNamesRegistration() {
        super(RegACRecipeNames.class, "AC_RecipeNames");
        setLoadStage(LoadStage.POST_INIT);
    }

    @Override
    public boolean registerClass(AnnotationData data) throws Exception {
        AcademyCraft.addToRecipe(data.getTheClass());
        return true;
    }

    @Override
    public boolean registerField(AnnotationData data) throws Exception {
        return true;
    }

}
