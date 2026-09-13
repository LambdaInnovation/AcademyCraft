package cn.li.mcver;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

/**
 * Reads texture dimensions without field reflection.
 */
public final class TextureSizeAccess {
    private TextureSizeAccess() {
    }

    public static int[] size(Object texture) {
        if (texture instanceof DynamicTexture dynamic) {
            NativeImage pixels = dynamic.getPixels();
            if (pixels != null) {
                return new int[]{pixels.getWidth(), pixels.getHeight()};
            }
        }
        return null;
    }

    public static int[] sizeFromManager(TextureManager manager, ResourceLocation resourceLocation) {
        if (manager == null || resourceLocation == null) {
            return null;
        }
        try {
            return size(manager.getTexture(resourceLocation));
        } catch (Exception ignored) {
            return null;
        }
    }
}
