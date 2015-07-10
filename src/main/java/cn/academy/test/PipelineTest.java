package cn.academy.test;

import org.lwjgl.input.Keyboard;

import cn.academy.core.registry.RegACKeyHandler;
import cn.academy.core.util.SubscribePipeline;
import cn.academy.core.util.ValuePipeline;
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
			System.out.println("val1 = " + pipeline.pipeFloat("val1", 233, true));
			System.out.println("ns.val2 = " + pipeline.pipeFloat("ns.val2", 30, true));
		}
	};
	
	public static class Listener  {
		
		@SubscribePipeline("val1")
		public float mulx2(float val, boolean aaa) {
			return val * 2 * (aaa ? 1.5f : 2f);
		}
		
		@SubscribePipeline("ns.?")
		public float mulx3(float val, boolean aaa) {
			return val * 3;
		}
		
	}
	
}
