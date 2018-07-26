package cn.academy.ability.api.context;

import cn.academy.ability.api.context.KeyDelegate.DelegateState;

public interface IStateProvider {

    DelegateState getState();

}