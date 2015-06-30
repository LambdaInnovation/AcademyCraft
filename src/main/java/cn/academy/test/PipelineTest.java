package cn.academy.test;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.pipeline.PipelineListener;
import cn.academy.ability.api.pipeline.SubscribePipeline;
import cn.academy.ability.api.pipeline.ValuePipeline;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.liutils.util.helper.KeyHandler;

/**
 * ValuePipeline unittest
 * @author WeAthFolD
 */
@Registrant
public class PipelineTest {
	
	static ValuePipeline pipeline = new ValuePipeline();
	static {
		pipeline.register(new Listener());
	}
	
	@RegACKeyHandler(name = "miku", defaultKey = Keyboard.KEY_K)
	public static KeyHandler key = new KeyHandler() {
		@Override
		public void onKeyDown() {
			System.out.println(pipeline.pipeFloat("val1", 233, true));
		}
	};
	
	public static class Listener implements PipelineListener {
		
		@SubscribePipeline("val1")
		public float mulx2(float val, boolean aaa) {
			return val * 2 * (aaa ? 1.5f : 2f);
		}

		@Override
		public boolean isListenerActivated() {
			return true;
		}
		
	}
	
}
