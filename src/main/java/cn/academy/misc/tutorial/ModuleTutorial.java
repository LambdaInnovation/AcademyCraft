/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.core.client.Resources;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.misc.tutorial.client.GuiTutorial;
import cn.academy.support.rf.RFSupport;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.ModuleTerminal;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.annoreg.mc.RegPostInitCallback;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import static cn.academy.misc.tutorial.Conditions.*;
import static cn.academy.misc.tutorial.ViewGroups.*;

@Registrant
@RegACRecipeNames
public class ModuleTutorial {
    
    @RegItem
    @RecipeName("tutorial")
    public static ItemTutorial itemTutorial;

    private static ACTutorial defnTut(String name) {
        return TutorialRegistry.addTutorial(name);
    }

    @RegPostInitCallback
    public static void initConditions() {
        defnTut("welcome");

        defnTut("ores")
                .setCondition(itemObtained(ModuleCrafting.oreConstraintMetal)
                        .or(itemObtained(ModuleCrafting.oreImagSil))
                        .or(itemObtained(ModuleCrafting.oreImagCrystal))
                        .or(itemObtained(ModuleCrafting.oreResoCrystal)))
                .addPreview(drawsBlock(ModuleCrafting.oreConstraintMetal))
                .addPreview(drawsBlock(ModuleCrafting.oreImagSil))
                .addPreview(drawsBlock(ModuleCrafting.oreImagCrystal))
                .addPreview(drawsBlock(ModuleCrafting.oreResoCrystal))
                .addPreview(displayIcon("items/matter_unit/phase_liquid_mat",
                        0, 0,
                        1, Color.white()))
                .addPreview(recipes(ModuleCrafting.constPlate))
                .addPreview(recipes(ModuleCrafting.ingotImagSil))
                .addPreview(recipes(ModuleCrafting.wafer))
                .addPreview(recipes(ModuleCrafting.silPiece));

        defnTut("phase_generator")
                .setCondition(itemObtained(ModuleEnergy.phaseGen))
                .addPreview(recipes(ModuleEnergy.phaseGen));

        defnTut("solar_generator")
                .setCondition(itemObtained(ModuleEnergy.solarGen))
                .addPreview(recipes(ModuleEnergy.solarGen));

        defnTut("wind_generator")
                .setCondition(itemObtained(ModuleEnergy.windgenBase)
                        .or(itemObtained(ModuleEnergy.windgenFan))
                        .or(itemObtained(ModuleEnergy.windgenMain))
                        .or(itemObtained(ModuleEnergy.windgenPillar)))
                .addPreview(recipes(ModuleEnergy.windgenBase))
                .addPreview(recipes(ModuleEnergy.windgenPillar))
                .addPreview(recipes(ModuleEnergy.windgenMain))
                .addPreview(recipes(ModuleEnergy.windgenFan));

        defnTut("metal_former")
                .setCondition(itemObtained(ModuleCrafting.metalFormer))
                .addPreview(recipes(ModuleCrafting.metalFormer));

        defnTut("imag_fusor")
                .setCondition(itemObtained(ModuleCrafting.imagFusor))
                .addPreview(recipes(ModuleCrafting.imagFusor));

        //TODO add support for app installer
        defnTut("terminal")
                .setCondition(itemObtained(ModuleTerminal.terminalInstaller))
                .addPreview(recipes(ModuleTerminal.terminalInstaller));

        defnTut("ability_developer")
                .setCondition(itemObtained(ModuleAbility.developerPortable)
                        .or(itemObtained(ModuleAbility.developerNormal))
                        .or(itemObtained(ModuleAbility.developerAdvanced)))
                .addPreview(recipes(ModuleAbility.developerPortable))
                .addPreview(recipes(ModuleAbility.developerNormal))
                .addPreview(recipes(ModuleAbility.developerAdvanced));

        defnTut("ability_basis");

        defnTut("energy_bridge")
                .setCondition(itemObtained(RFSupport.rfInput)
                        .or(itemObtained(RFSupport.rfOutput)))
                .addPreview(recipes(RFSupport.rfInput))
                .addPreview(recipes(RFSupport.rfOutput));

        defnTut("misc");

        defnTut("how_to_develop_ability");

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
}
