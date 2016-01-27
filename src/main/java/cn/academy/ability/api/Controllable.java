package cn.academy.ability.api;

import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.InstanceSerializer;
import cn.lambdalib.networkcall.s11n.RegSerializable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.ResourceLocation;

/**
 * This class has ability to create a SkillInstance to override
 * a specific ability key. Used in Skill for indexing.
 * @author WeAthFolD
 */
@Registrant
@RegSerializable(instance = Controllable.ControllableSerializer.class)
public abstract class Controllable {
    
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

    @SideOnly(Side.CLIENT)
    @Deprecated
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return null;
    }

    /**
     * Invoked when given {@link Controllable} was activated. (e.g. The skill was in the switched preset),
     *  register the KeyDelegates into the runtime.
     *
     *  @param keyID The key ID associated with the skill currently
     */
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
    
    public static class ControllableSerializer implements InstanceSerializer<Controllable> {

        @Override
        public Controllable readInstance(NBTBase nbt) throws Exception {
            int[] arr = ((NBTTagIntArray)nbt).func_150302_c();
            return CategoryManager.INSTANCE.getCategory(arr[0]).getControllable(arr[1]);
        }

        @Override
        public NBTBase writeInstance(Controllable obj) throws Exception {
            return new NBTTagIntArray(new int[] { 
                obj.getCategory().getCategoryID(), obj.getControlID() });
        }
        
    }
    
}
