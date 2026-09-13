package cn.li.neoforge1211.shim;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import clojure.lang.IFn;

/** Universal CustomLoaderBuilder skeleton — replaces proxy sites
 *  extending CustomLoaderBuilder for ItemModelBuilder. */
public class DelegatingCustomLoaderBuilder extends CustomLoaderBuilder<ItemModelBuilder> {

    private final IFn toJsonFn;

    public DelegatingCustomLoaderBuilder(ResourceLocation loader, ItemModelBuilder parent,
                                          ExistingFileHelper helper, IFn toJsonFn) {
        super(loader, parent, helper, false);
        this.toJsonFn = toJsonFn;
    }

    @Override public JsonObject toJson(JsonObject json) {
        return (JsonObject) toJsonFn.invoke();
    }
}
