package cn.academy.tutorial;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.client.gui.GuiTutorial;
import cn.academy.Resources;
import cn.academy.item.ItemApp;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.AppRegistry;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static cn.academy.tutorial.Conditions.itemObtained;
import static cn.academy.tutorial.ViewGroups.*;

public class TutorialInit {

    @SideOnly(Side.CLIENT)
    @StateEventCallback
    private static void initConditions(FMLPostInitializationEvent ev) {
        defnTut("welcome");

        defnTut("ores")
                .addCondition(itemObtained(ACBlocks.constraint_metal))
                .addCondition(itemObtained(ACBlocks.imagsil_ore))
                .addCondition(itemObtained(ACBlocks.crystal_ore))
                .addCondition(itemObtained(ACBlocks.reso_ore))
                .addPreview(drawsBlock(ACBlocks.constraint_metal))
                .addPreview(drawsBlock(ACBlocks.imagsil_ore))
                .addPreview(drawsBlock(ACBlocks.crystal_ore))
                .addPreview(drawsBlock(ACBlocks.reso_ore))
                .addPreview(displayIcon("items/phase_liquid_mat",
                        0, 0,
                        1, Colors.white()))
                .addPreview(recipes(ACItems.constraint_plate))
                .addPreview(recipes(ACItems.imag_silicon_ingot))
                .addPreview(recipes(ACItems.wafer))
                .addPreview(recipes(ACItems.imag_silicon_piece));

        defnTut("phase_generator")
                .addCondition(itemObtained(ACBlocks.phase_gen))
                .addPreview(recipes(ACBlocks.phase_gen));

        defnTut("solar_generator")
                .addCondition(itemObtained(ACBlocks.solar_gen))
                .addPreview(recipes(ACBlocks.solar_gen));

        defnTut("wind_generator")
                .addCondition(itemObtained(ACBlocks.windgen_base))
                .addCondition(itemObtained(ACItems.windgen_fan))
                .addCondition(itemObtained(ACBlocks.windgen_main))
                .addCondition(itemObtained(ACBlocks.windgen_pillar))
                .addPreview(recipes(ACBlocks.windgen_base))
                .addPreview(recipes(ACBlocks.windgen_pillar))
                .addPreview(recipes(ACBlocks.windgen_main))
                .addPreview(recipes(ACItems.windgen_fan));

        defnTut("metal_former")
                .addCondition(itemObtained(ACBlocks.metal_former))
                .addPreview(recipes(ACBlocks.metal_former));

        defnTut("imag_fusor")
                .addCondition(itemObtained(ACBlocks.imag_fusor))
                .addPreview(recipes(ACBlocks.imag_fusor));

        ACTutorial tutorialTerminal = defnTut("terminal")
                .addCondition(itemObtained(ACItems.terminal_installer))
                .addPreview(recipes(ACItems.terminal_installer));

        for(App app : AppRegistry.enumeration()) {
            if(!app.isPreInstalled()) {
                tutorialTerminal.addCondition(itemObtained(ItemApp.getItemForApp(app)));
                tutorialTerminal.addPreview(recipes(ItemApp.getItemForApp(app)));
            }
        }

        defnTut("ability_developer")
                .addCondition(itemObtained(ACItems.developer_portable))
                .addCondition(itemObtained(ACBlocks.dev_normal))
                .addCondition(itemObtained(ACBlocks.dev_advanced))
                .addPreview(recipes(ACItems.developer_portable))
                .addPreview(recipes(ACBlocks.dev_normal))
                .addPreview(recipes(ACBlocks.dev_advanced));

        defnTut("ability_basis");


        defnTut("misc");

        defnTut("develop_ability");

        defnTut("wireless_network");
    }

    public static ACTutorial defnTut(String name) {
        return TutorialRegistry.addTutorial(name);
    }
}