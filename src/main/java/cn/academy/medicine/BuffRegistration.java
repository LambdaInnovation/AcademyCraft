package cn.academy.medicine;

import cn.lambdalib.annoreg.core.AnnotationData;
import cn.lambdalib.annoreg.core.LoadStage;
import cn.lambdalib.annoreg.core.RegistryType;
import cn.lambdalib.annoreg.core.RegistryTypeDecl;

@RegistryTypeDecl
public class BuffRegistration extends RegistryType {

    public BuffRegistration() {
        super(RegBuff.class, "Buff");
        setLoadStage(LoadStage.PRE_INIT);
    }

    @Override
    public boolean registerClass(AnnotationData data) throws Exception {
        return super.registerClass(data);
    }
}
