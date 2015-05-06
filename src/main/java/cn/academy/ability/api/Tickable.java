package cn.academy.ability.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

@RegistrationClass
public class Tickable {
    private class Task {
        Runnable r;
        int tick;
    }
    
    private int ticks = 0;
    private Collection<Task> tasks = new HashSet();
    private Collection<Task> tasksAdded = new HashSet();
    private boolean inTaskIteration = false;
    
    protected void onTick() {}
    
    private void tick() {
        ++ticks;
        Iterator<Task> it = tasks.iterator();
        
        inTaskIteration = true;
        while (it.hasNext()) {
            Task t = it.next();
            if (t.tick <= getTick()) {
                t.r.run();
                it.remove();
            }
        }
        inTaskIteration = false;
        
        tasks.addAll(tasksAdded);
        tasksAdded.clear();
        
        onTick();
    }
    
    public final int getTick() {
        return ticks;
    }
    
    /**
     * Add this instance to tick list.
     * IMPORTANT: you must call this method on the correct thread.
     */
    public final void startTickEvent() {
        tickList.get().addInIteration(this);
    }
    
    /**
     * Remove this instance from tick list.
     * IMPORTANT: you must call this method on the correct thread.
     */
    public final void stopTickEvent() {
        tickList.get().removeInIteration(this);
    }
    
    protected final void schedule(int delay, Runnable r) {
        if (delay <= 0) {
            throw new IllegalArgumentException();
        }
        Task t = new Task();
        t.r = r;
        t.tick = getTick() + delay;
        (inTaskIteration ? tasksAdded : tasks).add(t);
    }
    
    /*
     * Event dispatch
     */
    private static class TickableList extends HashSet<Tickable> {
        private int it = 0;
        private HashSet<Tickable> added = new HashSet();
        private HashSet<Tickable> removed = new HashSet();
        
        public void startIteration() {
            ++it;
        }
        
        public void endIteration() {
            if (--it == 0) {
                super.addAll(added);
                added.clear();
                super.removeAll(removed);
                removed.clear();
            }
        }
        
        public void addInIteration(Tickable t) {
            if (it == 0) {
                super.add(t);
            } else {
                added.add(t);
            }
        }
        
        public void removeInIteration(Tickable t) {
            if (it == 0) {
                super.remove(t);
            } else {
                removed.add(t);
            }
        }
    }
    
    private static final ThreadLocal<TickableList> tickList = new ThreadLocal<TickableList>() {
        @Override protected TickableList initialValue() {
            return new TickableList();
        }
    };
    
    /**
     * Send a tick event to all tickable.
     * Try to use a same method for both server and client thread.
     */
    public static void tickAll() {
        TickableList l = tickList.get();
        l.startIteration();
        for (Tickable t : l) {
            t.tick();
        }
        l.endIteration();
    }
    
    @RegEventHandler
    public static class TickEventHandler {
        
        @SubscribeEvent
        public void onClientTick(ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START)
                tickAll();
        }

        @SubscribeEvent
        public void onServerTick(ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START)
                tickAll();
        }
    }
}
