package cn.li.forge1201.client.render;

import cn.li.forge1201.AcademyCraft1201;
import cn.li.mc1201.client.render.ModRenderTypes;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = AcademyCraft1201.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModShaders {
    private ModShaders() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        ForgeClientRenderRegistry.registerShaders(event);
    }

    public static ShaderInstance getPlasmaBodyShader() {
        return ModRenderTypes.getPlasmaBodyShader();
    }

    public static ShaderInstance getSkillProgbarShader() {
        return ModRenderTypes.getSkillProgbarShader();
    }

    public static ShaderInstance getMonoShader() {
        return ModRenderTypes.getMonoShader();
    }

    public static ShaderInstance getCpbarOverloadShader() {
        return ModRenderTypes.getCpbarOverloadShader();
    }

    public static ShaderInstance getAlphaDiscardShader() {
        return ModRenderTypes.getAlphaDiscardShader();
    }
}
