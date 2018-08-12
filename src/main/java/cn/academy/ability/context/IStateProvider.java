package cn.academy.ability.context;

import cn.academy.ability.context.KeyDelegate.DelegateState;

public interface IStateProvider {

    DelegateState getState();

}