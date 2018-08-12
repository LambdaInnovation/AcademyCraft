package cn.academy.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.client.gui.GuiTutorial;
import cn.academy.Resources;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.worldgen.WorldGenInit;
import cn.academy.energy.ModuleEnergy;
import cn.academy.item.ItemApp;
import cn.academy.support.rf.RFSupport;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.ModuleTerminal;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static cn.academy.tutorial.Conditions.itemObtained;
import static cn.academy.tutorial.ViewGroups.*;

public class TutorialInit {

    @StateEventCallback
    private static void initConditions(FMLPostInitializationEvent ev) {
        defnTut("welcome");

        defnTut("ores")
                .addCondition(itemObtained(WorldGenInit.oreConstraintMetal))
                .addCondition(itemObtained(WorldGenInit.oreImagSil))
                .addCondition(itemObtained(WorldGenInit.oreImagCrystal))
                .addCondition(itemObtained(WorldGenInit.oreResoCrystal))
                .addPreview(drawsBlock(WorldGenInit.oreConstraintMetal))
                .addPreview(drawsBlock(WorldGenInit.oreImagSil))
                .addPreview(drawsBlock(WorldGenInit.oreImagCrystal))
                .addPreview(drawsBlock(WorldGenInit.oreResoCrystal))
                .addPreview(displayIcon("items/matter_unit/phase_liquid_mat",
                        0, 0,
                        1, Color.white()))
                .addPreview(recipes(WorldGenInit.constPlate))
                .addPreview(recipes(WorldGenInit.ingotImagSil))
                .addPreview(recipes(WorldGenInit.wafer))
                .addPreview(recipes(WorldGenInit.silPiece));

        defnTut("phase_generator")
                .addCondition(itemObtained(ModuleEnergy.phaseGen))
                .addPreview(recipes(ModuleEnergy.phaseGen));

        defnTut("solar_generator")
                .addCondition(itemObtained(ModuleEnergy.solarGen))
                .addPreview(recipes(ModuleEnergy.solarGen));

        defnTut("wind_generator")
                .addCondition(itemObtained(ModuleEnergy.windgenBase))
                .addCondition(itemObtained(ModuleEnergy.windgenFan))
                .addCondition(itemObtained(ModuleEnergy.windgenMain))
                .addCondition(itemObtained(ModuleEnergy.windgenPillar))
                .addPreview(recipes(ModuleEnergy.windgenBase))
                .addPreview(recipes(ModuleEnergy.windgenPillar))
                .addPreview(recipes(ModuleEnergy.windgenMain))
                .addPreview(recipes(ModuleEnergy.windgenFan));

        defnTut("metal_former")
                .addCondition(itemObtained(WorldGenInit.metalFormer))
                .addPreview(recipes(WorldGenInit.metalFormer));

        defnTut("imag_fusor")
                .addCondition(itemObtained(WorldGenInit.imagFusor))
                .addPreview(recipes(WorldGenInit.imagFusor));

        ACTutorial tutorialTerminal = defnTut("terminal")
                .addCondition(itemObtained(ModuleTerminal.terminalInstaller))
                .addPreview(recipes(ModuleTerminal.terminalInstaller));

        for(App app : AppRegistry.enumeration()) {
            if(!app.isPreInstalled()) {
                tutorialTerminal.addCondition(itemObtained(ItemApp.getItemForApp(app)));
                tutorialTerminal.addPreview(recipes(ItemApp.getItemForApp(app)));
            }
        }

        defnTut("ability_developer")
                .addCondition(itemObtained(ModuleAbility.developerPortable))
                .addCondition(itemObtained(ModuleAbility.developerNormal))
                .addCondition(itemObtained(ModuleAbility.developerAdvanced))
                .addPreview(recipes(ModuleAbility.developerPortable))
                .addPreview(recipes(ModuleAbility.developerNormal))
                .addPreview(recipes(ModuleAbility.developerAdvanced));

        defnTut("ability_basis");

        defnTut("energy_bridge")
                .addCondition(itemObtained(RFSupport.rfInput))
                .addCondition(itemObtained(RFSupport.rfOutput))
                .addPreview(recipes(RFSupport.rfInput))
                .addPreview(recipes(RFSupport.rfOutput));

        defnTut("misc");

        defnTut("develop_ability");

        defnTut("wireless_network");

        // Add app for tutorial
        AppRegistry.register(new App("tutorial") {

            @Override
            public AppEnvironment createEnvironment() {
                return new AppEnvironment() {
                    @Override
                    @SideOnly(Side.CLIENT)
                    public void onStart() {
                        Minecraft.getMinecraft().displayGuiScreen(new GuiTutorial());
                    }
                };
            }

            // Random gives icon for more fun >)
            @Override
            public ResourceLocation getIcon() {
                float rand = RandUtils.nextFloat();
                if (rand < 0.2f) {
                    return icon(0);
                } else if (rand < 0.3f) {
                    return icon(1);
                } else {
                    return icon(2);
                }
            }

            private ResourceLocation icon(int id) {
                return Resources.preloadMipmapTexture("guis/apps/tutorial/icon_" + id);
            }
        }.setPreInstalled());
    }

    private static ACTutorial defnTut(String name) {
        return TutorialRegistry.addTutorial(name);
    }
}