/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.client;

import net.minecraft.client.model.ModelBiped;

/**
 * @author WeathFolD
 *
 */
public class SimpleModelBiped extends ModelBiped {

    public SimpleModelBiped() {
        super(0.0f);
    }

    public void draw() {
        float par7 = 0.0625f;
        this.bipedHead.render(par7);
        this.bipedBody.render(par7);
        this.bipedRightArm.render(par7);
        this.bipedLeftArm.render(par7);
        this.bipedRightLeg.render(par7);
        this.bipedLeftLeg.render(par7);
        this.bipedHeadwear.render(par7);
    }

}