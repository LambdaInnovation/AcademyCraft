/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
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
			    	} else if(name.equals("renderItem") && 
			    			desc.equals("(Lnet/minecraft/entity/EntityLivingBase;"
			    					+ "Lnet/minecraft/item/ItemStack;"
			    					+ "ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V")) {
			    		System.out.println("Injecting renderItem");
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
		
		int visTime = 0;
		
		public TPSkillEffect(MethodVisitor mv) {
			super(Opcodes.ASM4, mv);
		}
		
		@Override
	    public void visitMethodInsn(int opcode, String owner, String name,
	            String desc) {
			if(Opcodes.INVOKESTATIC == opcode && name.equals("glPopMatrix") && (++visTime == 4)) { //Before the last glPopMatrix() call
				System.out.println("Injected renderItem");
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				mv.visitVarInsn(Opcodes.ALOAD, 2);
				mv.visitVarInsn(Opcodes.ALOAD, 4);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "cn/academy/core/client/render/SkillRenderManager", "renderThirdPerson", 
						"(Lnet/minecraft/entity/EntityLivingBase;"
						+ "Lnet/minecraft/item/ItemStack;"
						+ "Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V");
			}
			mv.visitMethodInsn(opcode, owner, name, desc);
	    }
	}
	
	private static class FPSkillEffect extends MethodVisitor {
		
		int visTime = 0;

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
			if(Opcodes.INVOKESTATIC == opcode && name.equals("glDisable") && (++visTime == 2)) { //Before the glDisable(GL_RESCALE_NORMAL) call
				System.out.println("Injected renderInFirstPerson");
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "cn/academy/core/client/render/SkillRenderManager", "renderFirstPerson", "()V");
			}
			mv.visitMethodInsn(opcode, owner, name, desc);
	    }
		
		@Override
	    public void visitFieldInsn(int opcode, String owner, String name,
	            String desc) {
	    	mv.visitFieldInsn(opcode, owner, name, desc);
	    }
		
	}

}
