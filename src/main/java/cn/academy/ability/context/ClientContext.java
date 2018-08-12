package cn.academy.ability.context;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * {@link ClientContext} is attached to a certain context and receives all its messages.
 * @see RegClientContext
 */
@SideOnly(Side.CLIENT)
public class ClientContext extends Context {

    private final Context parent;

    /**
     * This constructor must be kept in subclasses in order for reflection to work.
     */
    public ClientContext(Context _parent) {
        super(_parent.player, _parent.skill);
        parent = _parent;
    }

    @Override
    public Context.Status getStatus() {
        return parent.status;
    }

    @Override
    public void terminate() {
        parent.terminate();
    }
}