package cn.academy.test;

import cn.academy.ability.api.context.Context;
import cn.academy.ability.api.context.ContextManager;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.s11n.network.NetworkMessage.NetworkListener;
import cn.lambdalib.util.key.KeyHandler;
import cn.lambdalib.util.key.KeyManager;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

@Registrant
public class ContextTest {

    static Side remoteToSide(boolean r) {
        return r ? Side.CLIENT : Side.SERVER;
    }

    public static class TestContext extends Context {

        public TestContext(EntityPlayer player) {
            super(player);
        }

        @NetworkListener(Context.MSG_TICK)
        void onTick() {
        }

        @NetworkListener(Context.MSG_MADEALIVE)
        void onMakeAlive() {
            debug("Made alive " + remoteToSide(isRemote()));
        }

        @NetworkListener(Context.MSG_TERMINATED)
        void onTerminate() {
            debug("Terminate " + remoteToSide(isRemote()));
        }

    }

    public static class TestKey extends KeyHandler {

        TestContext ctx;

        @Override
        public void onKeyDown() {
            if (ctx != null) {
                ctx.terminate();
            }

            ctx = new TestContext(getPlayer());
            ContextManager.instance.activate(ctx);
        }

        @Override
        public void onKeyUp() {
            ctx.terminate();
            ctx = null;
        }
    }

    @RegInitCallback
    public static void init() {
        KeyManager.dynamic.addKeyHandler("ContextTest", Keyboard.KEY_B, new TestKey());
    }

}
