package cn.academy.ability.context;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IStateProvider {

    DelegateState getState();

}