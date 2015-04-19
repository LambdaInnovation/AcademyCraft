package cn.academy.ability.api;

public class Tickable {
    //TODO implement a schedule system here.
    
    private int ticks = 0;
    
    protected void onTick() {}
    
    public final void tick() {
        ++ticks;
        onTick();
    }
    
    public final int getTick() {
        return ticks;
    }
}
