package cn.academy.core.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author WeAthFolD
 *
 */
public class RendererList extends Render {
    
    List<Render> renderers = new ArrayList<>();
    
    public RendererList(RenderManager rm, Render ...rs) {
        super(rm);
        renderers.addAll(Arrays.asList(rs));
    }
    
    public RendererList append(Render e) {
        renderers.add(e);
        return this;
    }

    @Override
    public void doRender(Entity ent, double x,
            double y, double z, float a, float b) {
        for(Render r : renderers)
            r.doRender(ent, x, y, z, a, b);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }

}