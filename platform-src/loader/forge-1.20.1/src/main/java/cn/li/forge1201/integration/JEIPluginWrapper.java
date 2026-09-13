package cn.li.forge1201.integration;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import cn.li.forge1201.AcademyCraft1201;
import cn.li.mcbase.clj.ClojureInterop;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI Plugin wrapper that delegates to Clojure implementation.
 *
 * This class is annotated with @JEIPlugin so JEI can discover it.
 * The actual implementation is in cn.li.forge1201.integration.jei-impl namespace.
 */
@JeiPlugin
public class JEIPluginWrapper implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(AcademyCraft1201.MODID, "content_plugin");

    private IModPlugin clojurePlugin;

    public JEIPluginWrapper() {
        try {
            // Load the Clojure namespace
            ClojureInterop.requireNamespace("cn.li.forge1201.integration.jei-impl");

            // Get the plugin factory function
            IFn createPlugin = Clojure.var("cn.li.forge1201.integration.jei-impl", "create-jei-plugin");
            this.clojurePlugin = (IModPlugin) createPlugin.invoke();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JEI plugin from Clojure", e);
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        if (clojurePlugin != null) {
            clojurePlugin.registerItemSubtypes(registration);
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (clojurePlugin != null) {
            clojurePlugin.registerCategories(registration);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (clojurePlugin != null) {
            clojurePlugin.registerRecipes(registration);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (clojurePlugin != null) {
            clojurePlugin.registerRecipeCatalysts(registration);
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (clojurePlugin != null) {
            clojurePlugin.registerGuiHandlers(registration);
        }
    }
}
