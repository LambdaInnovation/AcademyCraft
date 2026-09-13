package cn.li.mcver;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** Minecraft-version seam for DynamicTexture construction. */
public final class DynamicTextureAccess {
    private DynamicTextureAccess() {}

    public static DynamicTexture create(String debugName, NativeImage image) {
        return new DynamicTexture(() -> debugName, image);
    }
}
