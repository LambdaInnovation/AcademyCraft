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
package cn.academy.core.client.glsl;

// WHY DO YOU SEPERATE VERSIONS IT'S NONSENSE LWJGL!
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;

import net.minecraft.util.ResourceLocation;
import cn.academy.core.AcademyCraft;
import cn.liutils.util.generic.RegistryUtils;

/**
 * A simple GL Shader Program wrapper.
 * @author WeAthFolD
 */
public class ShaderProgram {
	
	private boolean compiled = false;
	private int programID;
	
	public ShaderProgram() {
		programID = glCreateProgram();
	}
	
	public void linkShader(ResourceLocation location, int type) {
		try {
			String str = IOUtils.toString(RegistryUtils.getResourceStream(location));
			int shaderID = glCreateShader(type);
			glShaderSource(shaderID, str);
			glCompileShader(shaderID);
			
			int successful = glGetShaderi(shaderID, GL_COMPILE_STATUS);
			if(successful == GL_FALSE) {
				String log = glGetShaderInfoLog(shaderID, glGetShaderi(shaderID, GL_INFO_LOG_LENGTH));
				AcademyCraft.log.error("Error when linking shader '" + location + "'. code: " + successful + ", Error string: \n" + log);
				throw new RuntimeException();
			}
			
			glAttachShader(programID, shaderID);
		} catch (IOException e) {
			AcademyCraft.log.error("Error when linking shader " + location, e);
			throw new RuntimeException();
		}
	}
	
	public int getProgramID() {
		return programID;
	}
	
	public void useProgram() {
		if(compiled) {
			glUseProgram(programID);
		} else {
			AcademyCraft.log.error("Trying to use a uncompiled program");
			throw new RuntimeException();
		}
	}
	
	public void compile() {
		if(compiled) {
			AcademyCraft.log.error("Trying to compile shader " + this + " twice.");
			throw new RuntimeException();
		}
		
		glLinkProgram(programID);
		
		int status = glGetProgrami(programID, GL_LINK_STATUS);
		if(status == GL_FALSE) {
			String log = glGetProgramInfoLog(programID, glGetProgrami(programID, GL_INFO_LOG_LENGTH));
			AcademyCraft.log.error("Error when linking program #" + programID + ". Error code: " + status + ", Error string: ");
			AcademyCraft.log.error(log);
			throw new RuntimeException();
		}
		
		compiled = true;
	}
	
}
