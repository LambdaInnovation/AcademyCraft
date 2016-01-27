/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
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
