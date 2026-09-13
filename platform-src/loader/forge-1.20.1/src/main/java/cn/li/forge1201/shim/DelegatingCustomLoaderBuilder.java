package cn.li.forge1201.shim;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import clojure.lang.IFn;

/** Universal CustomLoaderBuilder skeleton — replaces proxy sites
 *  extending CustomLoaderBuilder for ItemModelBuilder. */
public class DelegatingCustomLoaderBuilder extends CustomLoaderBuilder<ItemModelBuilder> {

    private final IFn toJsonFn;

    public DelegatingCustomLoaderBuilder(ResourceLocation loader, ItemModelBuilder parent,
                                          ExistingFileHelper helper, IFn toJsonFn) {
        super(loader, parent, helper);
        this.toJsonFn = toJsonFn;
    }

    @Override public JsonObject toJson(JsonObject json) {
        return (JsonObject) toJsonFn.invoke();
    }
}
