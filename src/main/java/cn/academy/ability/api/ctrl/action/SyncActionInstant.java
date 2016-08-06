/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.ctrl.action;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;

/**
 * SyncAction that only does something on creation but needs additional validation in both sides.
 * @author WeAthFolD
 */
public abstract class SyncActionInstant<TSkill extends Skill> extends SkillSyncAction<TSkill> {

    public SyncActionInstant(TSkill skill) {
        super(skill);
    }
    
    @Override
    public final void onStart() {
        super.onStart();
        if(!isRemote) {
            if(!validate()) {
                ActionManager.abortAction(this);
            } else {
                ActionManager.endAction(this);
            }
        }
    }
    
    @Override
    public final void onEnd() {
        execute();
    }
    
    /**
     * Check if this action is to be executed.
     */
    public abstract boolean validate();
    
    /**
     * Execute the action.
     */
    public void execute() {}

}
