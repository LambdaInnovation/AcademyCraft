package cn.academy.support.bukkit;

import cn.academy.AcademyCraft;
import cn.academy.event.BlockDestroyEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Provides Bukkit support for AcademyCraft.
 * See AcademyBukkit.
 * @author EAirPeter
 */
@SideOnly(Side.SERVER)
public final class BukkitAdapter {

    public static final BukkitAdapter INSTANCE = new BukkitAdapter();

    private static boolean registered = false;

    // public static void setWorld(String _world)
    private static Method setWorld_;

    // public static void setPlayer(UUID _player)
    private static Method setPlayer_;

    // public static boolean checkBlockDestroy(int x, int y, int z)
    private static Method checkBlockDestroy_;

    public static final UUID dummy = UUID.randomUUID();

    private World world = null;
    private EntityPlayer player = null;

    @SubscribeEvent
    public void onBlockDestroy(BlockDestroyEvent event) {
        try {
            if (world != event.world) {
                world = event.world;
                setWorld_.invoke(null, world.getWorldInfo().getWorldName());
            }
            if (player != event.player) {
                player = event.player;
                setPlayer_.invoke(null,
                    player == null ? dummy : event.player.getUniqueID());
            }
            event.setCanceled((boolean) checkBlockDestroy_.invoke(null,
                event.pos.getX(), event.pos.getY(), event.pos.getZ()));
        }
        catch (Throwable e) {
            AcademyCraft.log.error("Failed to handle BlockDestroyEvent", e);
        }
    }

    public static void init(Class<?> adapter) {
        try {
            setWorld_ = adapter.getMethod("setWorld", String.class);
            setPlayer_ = adapter.getMethod("setPlayer", UUID.class);
            checkBlockDestroy_ = adapter.getMethod("checkBlockDestroy",
                int.class, int.class, int.class);
            if (!registered) {
                MinecraftForge.EVENT_BUS.register(INSTANCE);
                registered = true;
            }
        }
        catch (Throwable e) {
            AcademyCraft.log.error("Failed to load BukkitAdapter", e);
        }
    }
}