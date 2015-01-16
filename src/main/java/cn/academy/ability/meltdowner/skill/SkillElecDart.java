package cn.academy.ability.meltdowner.skill;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.meltdowner.entity.EntityElecDart;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.core.proxy.ACClientProps;

public class SkillElecDart extends SkillBase {
	
	private static final int TICK_BEFORE_EMIT = 20;
	
	public static class State extends SkillState {
		
		private EntityElecDart dart;
		
		public State(EntityPlayer player) {
			super(player);
		}

		protected void onStart() {
			if (!isRemote) {
				dart = new EntityElecDart(player);
				player.worldObj.spawnEntityInWorld(dart);
			}
			this.finishSkillAfter(TICK_BEFORE_EMIT);
		}
		
		protected void onFinish() {
			if (dart != null) {
				dart.emit();
			}
		}
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new State(player);
			}
			
		});
	}
	
	public String getInternalName() {
		return "md_elecdart";
	}

	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.MD_LOGO;//TODO skill logo
	}

}
