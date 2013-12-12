package android.opengl.OpenGLES10;

import android.opengl.GLES20;
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

public class Shader
{
    private static final String TAG = "Shader";

    private int type;
    private ArrayList<ShaderSource > sources;
    private int id;
    
	public Shader(int type, ArrayList<ShaderSource > sources)
	{
		this.type = type;
		this.sources = new ArrayList<ShaderSource >(sources);

	}

	public int compile()
	{
		id = GLES20.glCreateShader(type);

		String typeString = type == GLES20.GL_FRAGMENT_SHADER ? "Fragment shader" : "Vertex shader";

		if (id == 0)
		{
			Log.e(TAG, "ERROR: Could not create " + typeString);
			return 0;
		}

		if (!readShaderSource())
		{
			Log.e(TAG, "ERROR: Could not read " + typeString + " source.");
			return 0;
		}

		GLES20.glCompileShader(id);

		int[] compiled = new int[1];
		GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, compiled, 0);

		if (compiled[0] == 0)
		{
			int[] infoLength = new int[1];
			GLES20.glGetShaderiv(id, GLES20.GL_INFO_LOG_LENGTH, infoLength, 0);

			if (infoLength[0] > 1)
			{
				String infoLog = GLES20.glGetShaderInfoLog(id);

				if (compiled[0] != 0)
				{
					Log.w(TAG, "WARNING: Compiled " + typeString + " with warnings:\n" + infoLog);
				}
				else
				{
					Log.e(TAG, "ERROR: Compiling " + typeString + " failed:\n" + infoLog);
				}
			}

			if (compiled[0] != 0)
			{
				Log.d(TAG, "Compiled " + typeString + " successfully.");
			}
			else
			{
				GLES20.glDeleteShader(id);
				return 0;
			}
		}

		return id;
	}
	
	private boolean readShaderSource()
	{
		String shaderSources = "";


		for (int i = 0; i < sources.size(); i++)
		{
		    shaderSources += sources.get(i).getSource();
		}

		GLES20.glShaderSource(id, shaderSources);

		return true;
	}
}
