package cn.li.mc1211.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.lang.reflect.Method;

/**
 * Vanilla client registration helpers shared by Forge/Fabric adapters.
 * Fluid render-layer registration is loader-specific (Forge ItemBlockRenderTypes
 * vs Fabric BlockRenderLayerMap).
 */
public final class ClientHelper {
    private ClientHelper() {
    }

    public interface RendererFactory {
        Object create();
    }

    public interface ScreenFactory {
        Object create(Object menu, Object playerInventory, Object title);
    }

    public static void bindTextureForSetup(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager textureManager = minecraft.getTextureManager();
        textureManager.bindForSetup(texture);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerBlockEntityRenderer(BlockEntityType<?> blockEntityType, RendererFactory factory) {
        BlockEntityRenderers.register((BlockEntityType) blockEntityType, context -> (BlockEntityRenderer) factory.create());
    }

    /**
     * {@link MenuScreens#register} is package-private on 1.21.1; invoke via reflection
     * so shared mc code stays loader-neutral (NeoForge uses RegisterMenuScreensEvent).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerMenuScreen(MenuType<?> menuType, ScreenFactory factory) {
        try {
            Method register = MenuScreens.class.getDeclaredMethod(
                "register", MenuType.class, MenuScreens.ScreenConstructor.class);
            register.setAccessible(true);
            MenuScreens.ScreenConstructor constructor = (menu, playerInventory, title) ->
                (Screen) factory.create(menu, playerInventory, title);
            register.invoke(null, menuType, constructor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register menu screen for " + menuType, e);
        }
    }
}
