package cn.academy.ability.api.context;

import com.google.common.base.Throwables;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Constructor;

@SideOnly(Side.CLIENT)
@RegistryTypeDecl
public class ClientContextRegistry extends RegistryType {

    public ClientContextRegistry() {
        super(RegClientContext.class, "RegClientContext");
        setLoadStage(LoadStage.INIT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean registerClass(AnnotationData data) throws Exception {
        for (Constructor ctor : data.getTheClass().getDeclaredConstructors()) {
            if (ctor.getParameterCount() == 1 &&
                    Context.class.isAssignableFrom(ctor.getParameterTypes()[0])) {
                ctor.setAccessible(true);
                Context.clientTypes.put(((RegClientContext) data.anno).value(), ctx -> {
                    try {
                        return (ClientContext) ctor.newInstance(ctx);
                    } catch (Exception ex) {
                        throw Throwables.propagate(ex);
                    }
                });
                return true;
            }
        }

        throw new IllegalArgumentException("No appropriate constructor found for " + data.getTheClass());
    }
}