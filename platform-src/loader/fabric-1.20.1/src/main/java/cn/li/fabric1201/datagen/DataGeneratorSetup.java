package cn.li.fabric1201.datagen;

import cn.li.mcbase.datagen.DataGeneratorInterop;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Fabric 1.20.1 DataGenerator entry point.
 */
public class DataGeneratorSetup implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        fabricDataGenerator.createPack();
        DataGeneratorInterop.invoke(
                "[academy] Error invoking Clojure DataGenerator setup: ",
                "cn.li.fabric1201.datagen.setup",
                "register-data-generators!",
                fabricDataGenerator,
                null);
    }
}
