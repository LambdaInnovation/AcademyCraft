package cn.academy.core.config;

import cn.academy.ability.api.event.CategoryChangeEvent;
import cn.academy.ability.api.event.SkillLearnEvent;
import cn.academy.core.config.ConfigEnv.FloatPipe;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.function.Predicate;

@Registrant
@RegDataPart(value=EntityPlayer.class, lazy=true)
public class PlayerConfigEnv extends DataPart<EntityPlayer> {

    public static ConfigEnv get(EntityPlayer player) {
        return getData_(player).env;
    }

    /**
     * Require the player's config environment to be rebuilt. At which time the a {@link PlayerEnvRebuildEvent} will
     * be post and {@link ConfigEnv#addFloatPipe(Predicate, FloatPipe)} will be made available, and event listeneres
     * should add pipes into the ConfigEnv.
     */
    public static void requireRebuild(EntityPlayer player) {
        getData_(player).requireRebuild();
    }

    private static PlayerConfigEnv getData_(EntityPlayer player) {
        return EntityData.get(player).getPart(PlayerConfigEnv.class);
    }

    /**
     *  Fired at both client at server when the {@link #requireRebuild(EntityPlayer)} is called on the respective side.
     *  Listeners should take the env and add pipes into the environment.
     */
    public class PlayerEnvRebuildEvent extends PlayerEvent {
        public final ConfigEnv env;

        public PlayerEnvRebuildEvent() {
            super (getEntity());
            env = PlayerConfigEnv.this.env;
        }
    }

    private ConfigEnv env;
    private boolean rebuilding;

    public PlayerConfigEnv() { }

    @Override
    public void wake() {
        requireRebuild();
    }

    private void requireRebuild() {
        env = new ConfigEnv() {
            @Override public void addFloatPipe(Predicate<String> pathSelector, FloatPipe pipe) {
                if (rebuilding) {
                    super.addFloatPipe(pathSelector, pipe);
                } else {
                    throw new IllegalStateException("Not rebuilding env, modifying to pipes not allowed");
                }
            }
        };
        env.setParent(ConfigEnv.global, "");

        debug("RequireRebuild " + getSide());

        rebuilding = true;
        MinecraftForge.EVENT_BUS.post(new PlayerEnvRebuildEvent());
        rebuilding = false;
    }

    @RegEventHandler(Bus.Forge)
    public static class RebuildTrigger {

        @SubscribeEvent
        public void changeCategory(CategoryChangeEvent evt) {
            go(evt.player);
        }

        @SubscribeEvent
        public void learnSkill(SkillLearnEvent evt) {
            go(evt.player);
        }

        private void go(EntityPlayer player) {
            PlayerConfigEnv.getData_(player).requireRebuild();
        }

    }

}
