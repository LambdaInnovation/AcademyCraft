package cn.li.neoforge1211.client.render;

import cn.li.neoforge1211.AcademyCraft1211;
import cn.li.mc1211.client.render.ModRenderTypes;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.io.IOException;

@EventBusSubscriber(modid = AcademyCraft1211.MODID, value = Dist.CLIENT)
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
