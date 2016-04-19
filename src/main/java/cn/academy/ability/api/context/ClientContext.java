package cn.academy.ability.api.context;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.ability.api.context.Context.Status;

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
        super(_parent.player);
        parent = _parent;
    }

    @Override
    public Status getStatus() {
        return parent.status;
    }

    @Override
    public void terminate() {
        parent.terminate();
    }
}
