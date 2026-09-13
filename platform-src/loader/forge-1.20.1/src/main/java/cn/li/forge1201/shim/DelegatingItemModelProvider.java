package cn.li.forge1201.shim;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import clojure.lang.IFn;

/** Universal ItemModelProvider skeleton — replaces proxy sites
 *  extending ItemModelProvider.  The registerModelsFn receives
 *  the DelegatingItemModelProvider instance as first argument. */
public class DelegatingItemModelProvider extends ItemModelProvider {

    private final IFn registerModelsFn;

    public DelegatingItemModelProvider(PackOutput packOutput, String modId,
                                        ExistingFileHelper exFileHelper, IFn registerModelsFn) {
        super(packOutput, modId, exFileHelper);
        this.registerModelsFn = registerModelsFn;
    }

    @Override protected void registerModels() {
        if (registerModelsFn != null) {
            registerModelsFn.invoke(this);
        }
    }
}
