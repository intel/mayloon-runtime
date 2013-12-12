package android.opengl.OpenGLES10;

import android.opengl.GLES10;
import android.util.Log;

import java.util.ArrayList;

/*
     Copyright 2009 Johannes Vuorinen
     
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at 
     
     http://www.apache.org/licenses/LICENSE-2.0 
     
     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
 */


public class MatrixStack
{
    private OpenGLESState openGLESState;
    private OpenGLES10Context context;
    private int mode;
    private ArrayList<Matrix4x4f> modelViewStack = new ArrayList<Matrix4x4f>();
    private ArrayList<Matrix4x4f> projectionStack = new ArrayList<Matrix4x4f>();
    private ArrayList<ArrayList<Matrix4x4f>> textureStacks;
    private ArrayList<Matrix4x4f> currentStack;
    
	public MatrixStack(OpenGLESState s, OpenGLES10Context context)
	{
		this.openGLESState = s;
		this.context = context;

	}
	
	public void dispose()
	{
		modelViewStack = null;
		projectionStack = null;
		textureStacks = null;
	}

	public void init()
	{
		modelViewStack.add(new Matrix4x4f());
		modelViewStack.set(0, OpenGLESMath.loadIdentity(modelViewStack.get(0)));

		projectionStack.add(new Matrix4x4f());
		projectionStack.set(0, OpenGLESMath.loadIdentity(projectionStack.get(0)));

		textureStacks = new ArrayList<ArrayList<Matrix4x4f>>(context.maxTextureImageUnits);
		for (int i = 0; i < context.maxTextureImageUnits; i++)
		{
			textureStacks.get(i).add(new Matrix4x4f());
			textureStacks.get(i).set(0, OpenGLESMath.loadIdentity(textureStacks.get(i).get(0)));
		}

		currentStack = modelViewStack;
	}
	
	public void setMatrixMode(int m)
	{
		mode = m;

		switch (mode)
		{
			case GLES10.GL_MODELVIEW:
				currentStack = modelViewStack;
				break;
			case GLES10.GL_PROJECTION:
				currentStack = projectionStack;
				break;
			case GLES10.GL_TEXTURE:
				currentStack = textureStacks.get(openGLESState.getActiveTexture());
				openGLESState.setTextureMatrix(openGLESState.getActiveTexture(), true); // TODO: could be optimized more.. only true when non-identity matrix.
				break;
			default:
				Log.d("MatrixStack", "ERROR: Unknown matrix mode.");
				break;
		}
	}
	
	public void pushMatrix()
	{
		currentStack.add(new Matrix4x4f(currentStack.get(currentStack.size() - 1)));
	}
	
	public void popMatrix()
	{
		currentStack.remove(currentStack.size() - 1);
	}

	public void loadIdentity()
	{
	    currentStack.set(currentStack.size() - 1, OpenGLESMath.loadIdentity(currentStack.get(currentStack.size() - 1)));
	}
	
	public void loadMatrix(float[] m)
	{
		Matrix4x4f mat = currentStack.get(currentStack.size() - 1);
		mat.copyFrom(m);
		currentStack.set(currentStack.size() - 1, mat);
	}
	
	public void translate(float x, float y, float z)
	{
	    currentStack.set(currentStack.size() - 1, OpenGLESMath.translate(currentStack.get(currentStack.size() - 1), x, y, z));
	}
	
	public void rotate(float angle, float x, float y, float z)
	{
	    currentStack.set(currentStack.size() - 1, OpenGLESMath.rotate(currentStack.get(currentStack.size() - 1), angle, x, y, z));
	}
	
	public void scale(float x, float y, float z)
	{
	    currentStack.set(currentStack.size() - 1, OpenGLESMath.scale(currentStack.get(currentStack.size() - 1), x, y, z));
	}
	
	public void frustum(float left, float right, float bottom, float top, float zNear, float zFar)
	{
	    currentStack.set(currentStack.size() - 1, OpenGLESMath.frustum(currentStack.get(currentStack.size() - 1), left, right, bottom, top, zNear, zFar));
	}
	
	public void ortho(float left, float right, float bottom, float top, float zNear, float zFar)
	{
	    currentStack.set(currentStack.size() - 1, OpenGLESMath.ortho(currentStack.get(currentStack.size() - 1), left, right, bottom, top, zNear, zFar));
	}
	
	public void multiply(float[] m)
	{
	    currentStack.set(currentStack.size() - 1,  OpenGLESMath.multiply(currentStack.get(currentStack.size() - 1), m));
	}

	public Matrix4x4f getModelViewMatrix()
	{
		return modelViewStack.get(modelViewStack.size() - 1);
	}
	
	public Matrix4x4f getProjectionMatrix()
	{
		return projectionStack.get(projectionStack.size() - 1);
	}
	
	public Matrix4x4f getTextureMatrix(int index)
	{
	    ArrayList<Matrix4x4f> matrix = textureStacks.get(index);
		return matrix.get(matrix.size() - 1);
	}
}
