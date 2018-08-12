package cn.academy.client.render.item;

import cn.academy.Resources;
import cn.lambdalib2.template.client.render.item.RenderModelItem;
import cn.lambdalib2.util.deprecated.ItemModelCustom;

/**
 * @author WeAthFolD
 */
public class RenderDeveloperPortable extends RenderModelItem {

    public RenderDeveloperPortable() {
        super(new ItemModelCustom(Resources.getModel("developer_portable")), 
            Resources.getTexture("models/developer_portable"));
        renderInventory = false;
        this.scale = 6;
        this.equipRotation = new Vec3d(0, -10, -5);
        this.equipOffset = new Vec3d(0.6, 0, -.2);
        this.entityItemOffset.z = 0.2;
        this.entityItemRotation.x = 15;
        this.thirdPersonScale = 0.6;
        this.thirdPersonOffset = new Vec3d(0.1, 0.05, 0.2);
    }

}