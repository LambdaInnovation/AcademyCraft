package cn.academy.ability.context;

import cn.academy.ability.context.KeyDelegate.DelegateState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IStateProvider {

    DelegateState getState();

}