package cn.li.fabric262.datagen;

import cn.li.mcbase.datagen.DataGeneratorInterop;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Fabric 26.2 DataGenerator entry point.
 */
public class DataGeneratorSetup implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        fabricDataGenerator.createPack();
        DataGeneratorInterop.invoke(
                "[academy] Error invoking Clojure DataGenerator setup: ",
                "cn.li.fabric262.datagen.setup",
                "register-data-generators!",
                fabricDataGenerator,
                null);
    }
}
