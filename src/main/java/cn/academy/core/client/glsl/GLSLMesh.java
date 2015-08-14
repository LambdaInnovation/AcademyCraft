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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.apache.commons.lang3.NotImplementedException;

import cn.liutils.render.material.Material;
import cn.liutils.render.mesh.Mesh;
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

/**
 * External mesh drawer using shader.
 * @author WeAthFolD
 */
public class GLSLMesh extends Mesh {
	
	int VBO;
	int IBO;
	
	public GLSLMesh() {
		VBO = glGenBuffers();
		IBO = glGenBuffers();
	}
	
	public void draw(ShaderProgram program) {
		draw(program.getProgramID());
	}
	
	/**
	 * Draw the mesh using the specified shader program.
	 */
	public void draw(int programID) {
		glUseProgram(programID);
		glBegin(GL_TRIANGLES);
		for(int j = 0; j < triangles.length; ++j) {
			int i = triangles[j];
			glVertex3d(vertices[i][0], vertices[i][1], vertices[i][2]);
			if(uvs != null) {
				glTexCoord2d(uvs[i][0], uvs[i][1]);
			} else {
				glTexCoord2d(0, 0);
			}
			if(normals != null) {
				glNormal3d(normals[i][0], normals[i][1], normals[i][2]);
			} else {
				glNormal3d(0, 0, 0);
			}
		}
		glEnd();
		glUseProgram(0);
	}
	
	@Override
	public void draw(Material mat) {
		throw new NotImplementedException("GLSLMesh doesn't handle old drawing routine");
	}
	
	@Override
	public void redraw(Material mat) {
		throw new NotImplementedException("GLSLMesh doesn't handle old drawing routine");
	}
	
}
