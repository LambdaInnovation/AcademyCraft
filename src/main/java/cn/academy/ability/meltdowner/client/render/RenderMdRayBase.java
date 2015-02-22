package cn.academy.ability.meltdowner.client.render;

import net.minecraft.util.ResourceLocation;
import cn.academy.ability.meltdowner.entity.EntityMdRayBase;
import cn.academy.misc.client.render.RendererRayBlended;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderMdRayBase <T extends EntityMdRayBase> extends RendererRayBlended<T> {

	public RenderMdRayBase() {
		super(null,
			  null,
			 1);
	}
	
	@Override
	protected void drawAtOrigin(T ent, double len, boolean firstPerson) {
		ResourceLocation[] texData = ent.getTexData();
		int i = ent.ticksExisted % (texData.length - 1);
		this.tex = texData[i + 1];
		this.blendTex = texData[0];
		super.drawAtOrigin(ent, len, firstPerson);
	}
	
}