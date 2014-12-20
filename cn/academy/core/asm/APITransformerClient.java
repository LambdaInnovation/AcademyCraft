/**
 * 
 */
package cn.academy.core.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Core asm transformer of ACAPI.
 * @author WeathFolD
 *
 */
public class APITransformerClient implements IClassTransformer {

	public APITransformerClient() {
	}

	/* (non-Javadoc)
	 * @see net.minecraft.launchwrapper.IClassTransformer#transform(java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public byte[] transform(String n1, String n2, byte[] data) {
		
		if(n1.equals("net.minecraft.client.renderer.ItemRenderer")) {
			System.out.println("Transforming " + n1 + ", " + n2);
			ClassReader cr = new ClassReader(data);
			ClassWriter cw = new ClassWriter(Opcodes.ASM4);
			
			ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
				
				@Override
			    public MethodVisitor visitMethod(int access, String name, String desc,
			            String signature, String[] exceptions) {
			    	MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			    	if(name.equals("renderItemInFirstPerson")) {
			    		return new FPSkillEffect(mv);
			    	} 
//			    	else if(name.equals("renderItem") && 
//			    			desc.equals("(Lnet/minecraft/entity/EntityLivingBase;"
//			    					+ "Lnet/minecraft/item/ItemStack;"
//			    					+ "ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;"
//			    					+ ")V")) {
//			    		return new TPSkillEffect(mv);
//			    	}
			    	return mv;
			    }
				
			};
			
			cr.accept(cv, 0);
			return cw.toByteArray();
		}
		else if(n1.equals("net.minecraft.client.renderer.entity.RenderPlayer")) {
			System.out.println("Transforming " + n1 + ", " + n2);
			ClassReader cr = new ClassReader(data);
			ClassWriter cw = new ClassWriter(Opcodes.ASM4);
			
			ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
				
				@Override
			    public MethodVisitor visitMethod(int access, String name, String desc,
			            String signature, String[] exceptions) {
			    	MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			    	if(name.equals("renderEquippedItems")) {
			    		return new TPSkillEffect(mv);
			    	} 
			    	return mv;
			    }
				
			};
			
			cr.accept(cv, 0);
			return cw.toByteArray();
		}
		return data;
	}
	
	private static class TPSkillEffect extends MethodVisitor {
		
		int visitTime = 0;

		public TPSkillEffect(MethodVisitor mv) {
			super(Opcodes.ASM4, mv);
		}
		
		@Override
	    public void visitMethodInsn(int opcode, String owner, String name,
	            String desc) {
			mv.visitMethodInsn(opcode, owner, name, desc);
			if(Opcodes.INVOKEVIRTUAL == opcode && 
				owner.equals("net/minecraft/client/model/ModelRenderer") &&
				name.equals("postRender") &&
				(++visitTime == 2)) {
				//visited = true;
				System.out.println("injected renderItems");
				
				// SkillRenderingHandler.doRender(EntityLivingBase, ItemStack, int, ItemRenderType)
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "cn/academy/api/client/SkillRenderingHandler", 
					"doRender", "(Lnet/minecraft/client/entity/AbstractClientPlayer;)V");
			}
		}
	}
	
	private static class FPSkillEffect extends MethodVisitor {
		
//		public AddSkillEffect() {
//			super(Opcodes.ASM4);
//		}
		
		boolean visited = false;

		public FPSkillEffect(MethodVisitor mv) {
			super(Opcodes.ASM4, mv);
		}
		
		@Override
	    public void visitInsn(int opcode) {
	    	mv.visitInsn(opcode);
	    }
		
		@Override
	    public void visitMethodInsn(int opcode, String owner, String name,
	            String desc) {
			mv.visitMethodInsn(opcode, owner, name, desc);
			if(!visited && Opcodes.INVOKESTATIC == opcode && name.equals("glPopMatrix")) {
				visited = true;
//				mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//				mv.visitLdcInsn("Called");
//				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
				System.out.println("Injected renderInFirstPerson");
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "cn/academy/api/client/SkillRenderingHandler", "renderFirstPerson", "()V");
			}
	    }
		
		@Override
	    public void visitFieldInsn(int opcode, String owner, String name,
	            String desc) {
	    	mv.visitFieldInsn(opcode, owner, name, desc);
	    }
		
	}

}
