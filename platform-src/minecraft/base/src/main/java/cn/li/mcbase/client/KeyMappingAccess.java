package cn.li.mcbase.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * KeyMapping current-binding accessors. Mojang keeps the live key private and
 * the loader access transformers (e.g. Forge's {@code getKey()}) do not exist
 * on Fabric, so the current binding is read through the public
 * {@code saveString()} round-trip — the same value Options > Controls
 * persists. No reflection; works on every loader.
 */
public final class KeyMappingAccess {
    private KeyMappingAccess() {
    }

    public static InputConstants.Key getKey(KeyMapping mapping) {
        return InputConstants.getKey(mapping.saveString());
    }

    public static int acKeyCode(KeyMapping mapping) {
        InputConstants.Key key = getKey(mapping);
        int value = key.getValue();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return -100 + value;
        }
        return value;
    }

    public static String boundKeyDisplayString(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    public static int boundKeyValue(KeyMapping mapping) {
        return getKey(mapping).getValue();
    }

    public static Component boundKeyDisplayName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage();
    }
}
