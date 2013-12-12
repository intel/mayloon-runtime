package android.opengl.OpenGLES10;

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

// Column-major order
public class Matrix4x4f
{
    public float[] m = new float[16];
    
	public Matrix4x4f()
	{
		for (int i = 0; i < 16; i++)
		{
			m[i] = 0;
		}
	}

	public Matrix4x4f(Matrix4x4f other)
	{
		for (int i = 0; i < 16; i++)
		{
			m[i] = other.m[i];
		}
	}

	public float getItem(int i, int j)
	{
        return m[i * 4 + j];
	}
	
	public void setItem(int i, int j, float value) {
	    m[i * 4 + j] = value;
	}
	

	public Matrix4x4f copyFrom (Matrix4x4f other)
	{
		for (int i = 0; i < 16; i++)
		{
			m[i] = other.m[i];
		}

		return this;
	}

	public Matrix4x4f copyFrom (float[] other)
	{
		for (int i = 0; i < 16; i++)
		{
			m[i] = other[i];
		}

		return this;
	}

	public boolean equalsTo (Matrix4x4f other)
	{
		for (int i = 0; i < 16; i++)
		{
			if (m[i] != other.m[i])
			{
				return false;
			}
		}

		return true;
	}

	public boolean notEqualsTo (Matrix4x4f other)
	{

		return !equalsTo(other);
	}
}