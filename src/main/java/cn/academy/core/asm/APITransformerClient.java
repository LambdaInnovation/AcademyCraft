/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
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

import cpw.mods.fml.common.FMLCommonHandler;
import cn.academy.core.ACCorePlugin;

/**
 * Core asm transformer of ACAPI.
 * Turns out that we have to manually handle the obfuscated environment. What are you doing forge guys? 0A0
 * @author WeathFolD
 */
public class APITransformerClient implements IClassTransformer {
	
	final String[]
		fEntityLivingBase = { "Lnet/minecraft/entity/EntityLivingBase;", "Lrh;" },
		fItemStack = { "Lnet/minecraft/item/ItemStack;", "Labp;" },
		mRenderItemFP = { "renderItemInFirstPerson", "a" };
	
	String descRenderItem, descRenderThirdPerson, mdRenderItemFP;

	public APITransformerClient() {}

	@Override
	public byte[] transform(String n1, String n2, byte[] data) {
		
		if(n2.equals("net.minecraft.client.renderer.ItemRenderer")) {
			descRenderItem = buildMethodDesc(fEntityLivingBase, fItemStack, "ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;", "V");
			descRenderThirdPerson = buildMethodDesc(fEntityLivingBase, fItemStack, "Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;", "V");
			mdRenderItemFP = getDesc(mRenderItemFP);
			
			ClassReader cr = new ClassReader(data);
			ClassWriter cw = new ClassWriter(Opcodes.ASM4);
			ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
				
				@Override
			    public MethodVisitor visitMethod(int access, String name, String desc,
			            String signature, String[] exceptions) {
			    	MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			    	if((name.equals(mdRenderItemFP)) && desc.equals("(F)V")) {
			    		System.out.println("[AC]Injecting renderItemInFirstPerson");
			    		return new FPSkillEffect(mv);
			    	} else if(name.equals("renderItem")) {
			    		if(desc.equals(descRenderItem)) {
			    			System.out.println("[AC]Injecting renderItem");
			    			return new TPSkillEffect(mv);
			    		} else {
			    			System.out.println("[AC]Bad descriptor: " + desc);
			    		}
			    	}
			    	return mv;
			    }
				
			};
			
			cr.accept(cv, 0);
			return cw.toByteArray();
		}
		return data;
	}
	
	private class TPSkillEffect extends MethodVisitor {
		
		int visTime = 0;
		
		public TPSkillEffect(MethodVisitor mv) {
			super(Opcodes.ASM4, mv);
		}
		
		@Override
	    public void visitMethodInsn(int opcode, String owner, String name,
	            String desc) {
			if(Opcodes.INVOKESTATIC == opcode && name.equals("glPopMatrix") && (++visTime == 4)) { //Before the last glPopMatrix() call
				mv.visitVarInsn(Opcodes.ALOAD, 1);
				mv.visitVarInsn(Opcodes.ALOAD, 2);
				mv.visitVarInsn(Opcodes.ALOAD, 4);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					"cn/academy/core/client/render/SkillRenderManager", 
					"renderThirdPerson", 
					descRenderThirdPerson);
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
	
	private static String buildMethodDesc(Object ...objs) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for(int i = 0; i < objs.length - 1; ++i) {
			sb.append(parse(objs[i]));
		}
		sb.append(")");
		sb.append(parse(objs[objs.length - 1]));
		return sb.toString();
	}
	
	private static String parse(Object o) {
		return (String) (o instanceof String ? o : getDesc((String[])o));
	}
	
	/**
	 * Get a correct method name/descriptor regarding the current environment.
	 */
	private static String getDesc(String[] data) {
		return getDesc(data[0], data[1]);
	}
	
	private static String getDesc(String normal, String obf) {
		System.out.println("ObfEnabled: " + ACCorePlugin.runtimeObfEnabled);
		return ACCorePlugin.runtimeObfEnabled ? obf : normal;
	}

}
