package cn.academy.ability.api.ctrl.instance;

import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapping for skills that just do something on click.
 * @author WeAthFolD
 */
public class SkillInstanceInstant extends SkillInstance {
    
    List<SyncAction> actions;

    public SkillInstanceInstant() {
        setNonMutex();
    }
    
    @Override
    public final void onStart() {
        if(actions != null) {
            for(SyncAction act : actions)
                ActionManager.startAction(act);
        }
        
        execute();
        this.endSkill();
    }
    
    /**
     * Called when this SkillInstance is executed. You can do additional stuff in player's client.
     */
    public void execute() {}
    
    /**
     * Automatically exeute this action on instance start.
     * @param action
     */
    public SkillInstanceInstant addExecution(SyncAction action) {
        if(actions == null) {
            actions = new ArrayList();
        }
        actions.add(action);
        return this;
    }

}