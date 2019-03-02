package cn.academy.ability;

import cn.academy.ability.context.ClientRuntime;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.nbt.NBTS11n.BaseSerializer;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.util.ResourceLocation;

/**
 * This class has ability to create a SkillInstance to override
 * a specific ability key. Used in Skill for indexing.
 * @author WeAthFolD
 *
 * SkillInstance has been removed.  --Paindar
 */
public abstract class Controllable {

    @StateEventCallback
    private static void init(FMLInitializationEvent ev) {
        NetworkS11n.addDirect(Controllable.class, new NetS11nAdaptor<Controllable>() {
            @Override
            public void write(ByteBuf buf, Controllable obj) {
                buf.writeByte(obj.getCategory().getCategoryID());
                buf.writeByte(obj.getControlID());
            }

            @Override
            public Controllable read(ByteBuf buf) throws ContextException {
                Category cat = CategoryManager.INSTANCE.getCategory(buf.readByte());
                return cat.getControllable(buf.readByte());
            }
        });
        NBTS11n.addBase(Controllable.class, new BaseSerializer<NBTBase, Controllable>() {
            @Override
            public NBTBase write(Controllable value) {
                return new NBTTagByteArray(new byte[] {
                        (byte) value.getCategory().getCategoryID(),
                        (byte) value.getControlID()
                });
            }

            @Override
            public Controllable read(NBTBase tag, Class<? extends Controllable> type) {
                byte[] bytes = ((NBTTagByteArray) tag).getByteArray();
                Category cat = CategoryManager.INSTANCE.getCategory(bytes[0]);
                return cat.getControllable(bytes[1]);
            }
        });
    }

    private Category category;
    private int id;
    
    public Controllable() {}
    
    final void addedControllable(Category _category, int _id) {
        category = _category;
        id = _id;
    }
    
    public final Category getCategory() {
        return category;
    }
    
    public final int getControlID() {
        return id;
    }

    /**
     * Invoked when given {@link Controllable} was activated. (e.g. The skill was in the switched preset),
     *  register the KeyDelegates into the runtime.
     *
     *  @param keyID The key ID associated with the skill currently
     */
    @SideOnly(Side.CLIENT)
    public /*abstract*/ void activate(ClientRuntime rt, int keyID) {}
    
    /**
     * Return the icon of this controllable. Used in KeyHint display UI.
     */
    public abstract ResourceLocation getHintIcon();
    
    /**
     * Return the hint text of the controllable. Used in KeyHint display UI.
     */
    public abstract String getHintText();
    
    /**
     * @return Whether this controllable should override the vanilla key control.
     */
    public boolean shouldOverrideKey() {
        return true;
    }
    
}