package android.opengl.OpenGLES10;

import android.opengl.GLES20;
import android.opengl.OpenGLES10.OpenGLESState.AttributeId;
import android.opengl.OpenGLES10.OpenGLESState.UniformId;
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

public class ShaderProgram
{
    private static final String TAG = "ShaderProgram";
    private String name = null;
    private int program;

    private int activeUniforms;
    private int activeUniformsMaxLength;
    private ArrayList<UniformSimple > uniforms = new ArrayList<UniformSimple >();

    private int activeAttributes;
    private int activeAttributesMaxLength;
    private ArrayList<AttributeSimple > attributes = new ArrayList<AttributeSimple >();

    private int attachedShaders;
    
	public ShaderProgram(String name, Shader vertexShader, Shader fragmentShader)
	{
		this.name = name;
		program = createProgram(vertexShader, fragmentShader);
	}
	
	public ShaderProgram(String name, Object binary, int length, int binaryformat)
	{
		this.name = name;
		// TODO: Binary shader
	}
	
	public void dispose()
	{
		GLES20.glDeleteProgram(program);
	}

	public void use()
	{
		GLES20.glUseProgram(program);
	}
	
	public void unuse()
	{
		GLES20.glUseProgram(0);
	}
	
	public void validate()
	{
	    GLES20.glValidateProgram(program);

		int[] validated = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, validated, 0);

		if (validated[0] != 0)
		{
		    String infoLog = null;
		    infoLog = GLES20.glGetProgramInfoLog(program);
			Log.e(TAG, "ERROR: Validation error in program " + name + ":\n" + infoLog);
		}
	}

	public ArrayList<AttributeSimple > getActiveAttributes()
	{
		return attributes;
	}
	
	public ArrayList<UniformSimple > getActiveUniforms()
	{
		return uniforms;
	}

	public void setAttributeVertexPointer(int location, int size, int type, boolean normalized, int stride, java.nio.Buffer ptr)
	{
		GLES20.glVertexAttribPointer(location, size, type, normalized, stride, ptr);
	}

	public void setUniform1i(String name, int i)
	{
	    GLES20.glUniform1i(getUniformLocation(name), i);
	}
	
	public void setUniform1i(int loc, int i)
	{
	    GLES20.glUniform1i(loc, i);
	}
	
	public void setUniform1iv(String name, int count, int[] i)
	{
	    GLES20.glUniform1iv(getUniformLocation(name), count, i, 0);
	}
	
	public void setUniform1iv(int loc, int count, int[] i)
	{
	    GLES20.glUniform1iv(loc, count, i, 0);
	}
	
	public void setUniform1f(String name, float v)
	{
	    GLES20.glUniform1f(getUniformLocation(name), v);
	}
	
	public void setUniform1f(int loc, float v)
	{
	    GLES20.glUniform1f(loc, v);
	}
	
	public void setUniform3f(String name, float x, float y, float z)
	{
	    GLES20.glUniform3f(getUniformLocation(name), x, y, z);
	}
	
	public void setUniform3f(int loc, float x, float y, float z)
	{
	    GLES20.glUniform3f(loc, x, y, z);
	}
	
	public void setUniform3fv(String name, int count, float[] v)
	{
	    GLES20.glUniform3fv(getUniformLocation(name), count, v, 0);
	}
	
	public void setUniform3fv(int loc, int count, float[] v)
	{
	    GLES20.glUniform3fv(loc, count, v, 0);
	}
	
	public void setUniform4f(String name, float x, float y, float z, float w)
	{
	    GLES20.glUniform4f(getUniformLocation(name), x, y, z, w);
	}
	
	public void setUniform4f(int loc, float x, float y, float z, float w)
	{
	    GLES20.glUniform4f(loc, x, y, z, w);
	}
	
	public void setUniform4fv(String name, int count, float[] v)
	{
	    GLES20.glUniform4fv(getUniformLocation(name), count, v, 0);
	}
	
	public void setUniform4fv(int loc, int count, float[] v)
	{
	    GLES20.glUniform4fv(loc, count, v, 0);
	}
	
	public void setUniformMatrix4fv(String name, float[] m)
	{
	    GLES20.glUniformMatrix4fv(getUniformLocation(name), 1, false, m, 0);
	}
	
	public void setUniformMatrix4fv(int loc, float[] m)
	{
	    GLES20.glUniformMatrix4fv(loc, 1, false, m, 0);
	}
	
	public void setUniformMatrix3fv(String name, float[] m)
	{
	    GLES20.glUniformMatrix3fv(getUniformLocation(name), 1, false, m, 0);
	}
	
	public void setUniformMatrix3fv(int loc, float[] m)
	{
	    GLES20.glUniformMatrix3fv(loc, 1, false, m , 0);
	}

	private int createProgram(Shader vertexShader, Shader fragmentShader)
	{
		int vertexShaderId = vertexShader.compile();
		int fragmentShaderId = fragmentShader.compile();

		GLES20.glReleaseShaderCompiler();

		int program = GLES20.glCreateProgram();

		if (program == 0)
		{
			Log.e(TAG, "ERROR: Creating program " + name + " failed.");
			return 0;
		}

		GLES20.glAttachShader(program, vertexShaderId);

		GLES20.glAttachShader(program, fragmentShaderId);

		GLES20.glLinkProgram(program);

		int[] linked = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);

		if (linked[0] != 0)
		{
			String infoLog = GLES20.glGetProgramInfoLog(program);
			if (infoLog.length() > 0) {
				if (linked[0] != 0)
				{
					Log.w(TAG, "WARNING: Linked program " + name + " with warnings:\n" + infoLog);
				}
				else
				{
				    Log.e(TAG, "ERROR: Linking program " + name + " failed:\n" + infoLog);
				}
			}

			if (linked[0] != 0)
			{
				Log.i(TAG, "Linked program " + name + " successfully.");
			}
			else
			{
			    GLES20.glDeleteProgram(program);
				return 0;
			}
		}

		int[] value = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_ATTRIBUTES, value, 0);
		activeAttributes = value[1];
		GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, value, 0);
		activeAttributesMaxLength = value[1];
		GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_UNIFORMS, value, 0);
		activeUniforms = value[1];
		GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_UNIFORM_MAX_LENGTH, value, 0);
		activeUniformsMaxLength = value[1];
		GLES20.glGetProgramiv(program, GLES20.GL_ATTACHED_SHADERS, value, 0);
		attachedShaders = value[1];

		Log.d(TAG, "Active attributes: " + activeAttributes);
		Log.d(TAG, "Active attributes max length: " + activeAttributesMaxLength);
		Log.d(TAG, "Active uniforms: " + activeUniforms);
		Log.d(TAG, "Active uniforms max length: " + activeUniformsMaxLength);
		Log.d(TAG, "Attached shaders: " + attachedShaders);

		Log.d(TAG, "Attributes");
		for (int i = 0; i < activeAttributes; i++)
		{
			byte[] name = new byte[activeAttributesMaxLength];
			int[] size = new int[1];
			int[] type = new int[1];

			GLES20.glGetActiveAttrib(program, i, activeAttributesMaxLength, null, 0, size, 0, type, 0, name, 0);
			String attributeName = new String(name);
			int attributeLocation = GLES20.glGetAttribLocation(program, attributeName);

            int id = -1;
            if (attributeName == "a_position")
            {
                id = AttributeId.POSITION;
            }
            else if (attributeName == "a_normal")
            {
                id = AttributeId.NORMAL;
            }
            else if (attributeName == "a_color")
            {
                id = AttributeId.COLOR;
            }
            else if (attributeName == "a_texCoord0")
            {
                id = AttributeId.TEXCOORD0;
            }
            else if (attributeName == "a_texCoord1")
            {
                id = AttributeId.TEXCOORD1;
            }
            else if (attributeName == "a_texCoord2")
            {
                id = AttributeId.TEXCOORD2;
            }
			else
			{
				Log.e(TAG, "ERROR: Missing " + attributeName);
				return 0;
			}

			attributes.add(new AttributeSimple(id, attributeLocation));

			String typeString;

			switch (type[0])
			{
				case GLES20.GL_FLOAT:
					typeString = "GL_FLOAT";
					break;
				case GLES20.GL_FLOAT_VEC2:
					typeString = "GL_FLOAT_VEC2";
					break;
				case GLES20.GL_FLOAT_VEC3:
					typeString = "GL_FLOAT_VEC3";
					break;
				case GLES20.GL_FLOAT_VEC4:
					typeString = "GL_FLOAT_VEC4";
					break;
				case GLES20.GL_FLOAT_MAT2:
					typeString = "GL_FLOAT_MAT2";
					break;
				case GLES20.GL_FLOAT_MAT3:
					typeString = "GL_FLOAT_MAT3";
					break;
				case GLES20.GL_FLOAT_MAT4:
					typeString = "GL_FLOAT_MAT4";
					break;
				default:
					typeString = "Unknown";
					Log.e(TAG, "ERROR: Unknown type.");
					break;
			}

			Log.d(TAG, attributeName + ": type " + typeString + " location: " + attributeLocation);
		}

		Log.d(TAG, "Uniforms");
		for (int i = 0; i < activeUniforms; i++)
		{

			byte[] name = new byte[activeUniformsMaxLength];
			int[] size = new int[1];
			int[] uniformType = new int[1];
			GLES20.glGetActiveUniform(program, i, activeUniformsMaxLength, null, 0, size, 0, uniformType, 0, name, 0);
			String uniformName = new String(name);
			int uniformLocation = GLES20.glGetUniformLocation(program, uniformName);

            int id = -1;
            if (uniformName == "u_lightModelLocalViewerEnabled")
            {
                id = UniformId.LIGHT_MODEL_LOCAL_VIEWER_ENABLED;
            }
            else if (uniformName == "u_lightModelTwoSideEnabled")
            {
                id = UniformId.LIGHT_MODEL_TWO_SIDE_ENABLED;
            }
            else if (uniformName == "u_lightingEnabled")
            {
                id = UniformId.LIGHTING_ENABLED;
            }
            else if (uniformName == "u_light0Enabled")
            {
                id = UniformId.LIGHT0_ENABLED;
            }
            else if (uniformName == "u_light1Enabled")
            {
                id = UniformId.LIGHT1_ENABLED;
            }
            else if (uniformName == "u_light2Enabled")
            {
                id = UniformId.LIGHT2_ENABLED;
            }
            else if (uniformName == "u_light0.ambient")
            {
                id = UniformId.LIGHT0_AMBIENT;
            }
            else if (uniformName == "u_light1.ambient")
            {
                id = UniformId.LIGHT1_AMBIENT;
            }
            else if (uniformName == "u_light2.ambient")
            {
                id = UniformId.LIGHT2_AMBIENT;
            }
            else if (uniformName == "u_light0.diffuse")
            {
                id = UniformId.LIGHT0_DIFFUSE;
            }
            else if (uniformName == "u_light1.diffuse")
            {
                id = UniformId.LIGHT1_DIFFUSE;
            }
            else if (uniformName == "u_light2.diffuse")
            {
                id = UniformId.LIGHT2_DIFFUSE;
            }
            else if (uniformName == "u_light0.specular")
            {
                id = UniformId.LIGHT0_SPECULAR;
            }
            else if (uniformName == "u_light1.specular")
            {
                id = UniformId.LIGHT1_SPECULAR;
            }
            else if (uniformName == "u_light2.specular")
            {
                id = UniformId.LIGHT2_SPECULAR;
            }
            else if (uniformName == "u_light0.position")
            {
                id = UniformId.LIGHT0_POSITION;
            }
            else if (uniformName == "u_light1.position")
            {
                id = UniformId.LIGHT1_POSITION;
            }
            else if (uniformName == "u_light2.position")
            {
                id = UniformId.LIGHT2_POSITION;
            }
            else if (uniformName == "u_light0.spotDirection")
            {
                id = UniformId.LIGHT0_SPOT_DIRECTION;
            }
            else if (uniformName == "u_light1.spotDirection")
            {
                id = UniformId.LIGHT1_SPOT_DIRECTION;
            }
            else if (uniformName == "u_light2.spotDirection")
            {
                id = UniformId.LIGHT2_SPOT_DIRECTION;
            }
            else if (uniformName == "u_light0.spotExponent")
            {
                id = UniformId.LIGHT0_SPOT_EXPONENT;
            }
            else if (uniformName == "u_light1.spotExponent")
            {
                id = UniformId.LIGHT1_SPOT_EXPONENT;
            }
            else if (uniformName == "u_light2.spotExponent")
            {
                id = UniformId.LIGHT2_SPOT_EXPONENT;
            }
            else if (uniformName == "u_light0.spotCutoffAngleCos")
            {
                id = UniformId.LIGHT0_SPOT_CUTOFF_ANGLE_COS;
            }
            else if (uniformName == "u_light1.spotCutoffAngleCos")
            {
                id = UniformId.LIGHT1_SPOT_CUTOFF_ANGLE_COS;
            }
            else if (uniformName == "u_light2.spotCutoffAngleCos")
            {
                id = UniformId.LIGHT2_SPOT_CUTOFF_ANGLE_COS;
            }
            else if (uniformName == "u_light0.constantAttenuation")
            {
                id = UniformId.LIGHT0_CONSTANT_ATTENUATION;
            }
            else if (uniformName == "u_light1.constantAttenuation")
            {
                id = UniformId.LIGHT1_CONSTANT_ATTENUATION;
            }
            else if (uniformName == "u_light2.constantAttenuation")
            {
                id = UniformId.LIGHT2_CONSTANT_ATTENUATION;
            }
            else if (uniformName == "u_light0.linearAttenuation")
            {
                id = UniformId.LIGHT0_LINEAR_ATTENUATION;
            }
            else if (uniformName == "u_light1.linearAttenuation")
            {
                id = UniformId.LIGHT1_LINEAR_ATTENUATION;
            }
            else if (uniformName == "u_light2.linearAttenuation")
            {
                id = UniformId.LIGHT2_LINEAR_ATTENUATION;
            }
            else if (uniformName == "u_light0.quadraticAttenuation")
            {
                id = UniformId.LIGHT0_QUADRATIC_ATTENUATION;
            }
            else if (uniformName == "u_light1.quadraticAttenuation")
            {
                id = UniformId.LIGHT1_QUADRATIC_ATTENUATION;
            }
            else if (uniformName == "u_light2.quadraticAttenuation")
            {
                id = UniformId.LIGHT2_QUADRATIC_ATTENUATION;
            }
            else if (uniformName == "u_modelViewMatrix")
            {
                id = UniformId.MODELVIEW_MATRIX;
            }
            else if (uniformName == "u_modelViewProjectionMatrix")
            {
                id = UniformId.MODELVIEW_PROJECTION_MATRIX;
            }
            else if (uniformName == "u_transposeAdjointModelViewMatrix")
            {
                id = UniformId.TRANPOSE_ADJOINT_MODEL_VIEW_MATRIX;
            }
            else if (uniformName == "u_rescaleNormalFactor")
            {
                id = UniformId.RESCALE_NORMAL_FACTOR;
            }
            else if (uniformName == "u_material.ambient")
            {
                id = UniformId.MATERIAL_AMBIENT;
            }
            else if (uniformName == "u_material.diffuse")
            {
                id = UniformId.MATERIAL_DIFFUSE;
            }
            else if (uniformName == "u_material.specular")
            {
                id = UniformId.MATERIAL_SPECULAR;
            }
            else if (uniformName == "u_material.emission")
            {
                id = UniformId.MATERIAL_EMISSION;
            }
            else if (uniformName == "u_material.shininess")
            {
                id = UniformId.MATERIAL_SHININESS;
            }
            else if (uniformName == "u_normalizeEnabled")
            {
                id = UniformId.NORMALIZE_ENABLED;
            }
            else if (uniformName == "u_rescaleNormalEnabled")
            {
                id = UniformId.RESCALE_NORMAL_ENABLED;
            }
            else if (uniformName == "u_alphaTestEnabled")
            {
                id = UniformId.ALPHA_TEST_ENABLED;
            }
            else if (uniformName == "u_alphaFunc")
            {
                id = UniformId.ALPHA_FUNC;
            }
            else if (uniformName == "u_alphaFuncValue")
            {
                id = UniformId.ALPHA_FUNC_VALUE;
            }
            else if (uniformName == "u_globalAmbientColor")
            {
                id = UniformId.GLOBAL_AMBIENT_COLOR;
            }
            else if (uniformName == "u_positionEnabled")
            {
                id = UniformId.POSITION_ENABLED;
            }
            else if (uniformName == "u_normalEnabled")
            {
                id = UniformId.NORMAL_ENABLED;
            }
            else if (uniformName == "u_colorEnabled")
            {
                id = UniformId.COLOR_ENABLED;
            }
            else if (uniformName == "u_texCoord0Enabled")
            {
                id = UniformId.TEXCOORD0_ENABLED;
            }
            else if (uniformName == "u_texCoord1Enabled")
            {
                id = UniformId.TEXCOORD1_ENABLED;
            }
            else if (uniformName == "u_texCoord2Enabled")
            {
                id = UniformId.TEXCOORD2_ENABLED;
            }
            else if (uniformName == "u_texture0Enabled")
            {
                id = UniformId.TEXTURE0_ENABLED;
            }
            else if (uniformName == "u_texture1Enabled")
            {
                id = UniformId.TEXTURE1_ENABLED;
            }
            else if (uniformName == "u_texture2Enabled")
            {
                id = UniformId.TEXTURE2_ENABLED;
            }
            else if (uniformName == "u_texture0Sampler")
            {
                id = UniformId.TEXTURE0_SAMPLER;
            }
            else if (uniformName == "u_texture1Sampler")
            {
                id = UniformId.TEXTURE1_SAMPLER;
            }
            else if (uniformName == "u_texture2Sampler")
            {
                id = UniformId.TEXTURE2_SAMPLER;
            }
            else if (uniformName == "u_texture0Matrix")
            {
                id = UniformId.TEXTURE0_MATRIX;
            }
            else if (uniformName == "u_texture1Matrix")
            {
                id = UniformId.TEXTURE1_MATRIX;
            }
            else if (uniformName == "u_texture2Matrix")
            {
                id = UniformId.TEXTURE2_MATRIX;
            }
            else if (uniformName == "u_texture0MatrixEnabled")
            {
                id = UniformId.TEXTURE0_MATRIX_ENABLED;
            }
            else if (uniformName == "u_texture1MatrixEnabled")
            {
                id = UniformId.TEXTURE1_MATRIX_ENABLED;
            }
            else if (uniformName == "u_texture2MatrixEnabled")
            {
                id = UniformId.TEXTURE2_MATRIX_ENABLED;
            }
            else if (uniformName == "u_texture0Format")
            {
                id = UniformId.TEXTURE0_FORMAT;
            }
            else if (uniformName == "u_texture1Format")
            {
                id = UniformId.TEXTURE1_FORMAT;
            }
            else if (uniformName == "u_texture2Format")
            {
                id = UniformId.TEXTURE2_FORMAT;
            }
            else if (uniformName == "u_texture0EnvMode")
            {
                id = UniformId.TEXTURE0_ENV_MODE;
            }
            else if (uniformName == "u_texture1EnvMode")
            {
                id = UniformId.TEXTURE1_ENV_MODE;
            }
            else if (uniformName == "u_texture2EnvMode")
            {
                id = UniformId.TEXTURE2_ENV_MODE;
            }
            else if (uniformName == "u_texture0EnvColor")
            {
                id = UniformId.TEXTURE0_ENV_COLOR;
            }
            else if (uniformName == "u_texture1EnvColor")
            {
                id = UniformId.TEXTURE1_ENV_COLOR;
            }
            else if (uniformName == "u_texture2EnvColor")
            {
                id = UniformId.TEXTURE2_ENV_COLOR;
            }
            else if (uniformName == "u_texture0EnvRGBScale")
            {
                id = UniformId.TEXTURE0_ENV_RGB_SCALE;
            }
            else if (uniformName == "u_texture1EnvRGBScale")
            {
                id = UniformId.TEXTURE1_ENV_RGB_SCALE;
            }
            else if (uniformName == "u_texture2EnvRGBScale")
            {
                id = UniformId.TEXTURE2_ENV_RGB_SCALE;
            }
            else if (uniformName == "u_texture0EnvAlphaScale")
            {
                id = UniformId.TEXTURE0_ENV_ALPHA_SCALE;
            }
            else if (uniformName == "u_texture1EnvAlphaScale")
            {
                id = UniformId.TEXTURE1_ENV_ALPHA_SCALE;
            }
            else if (uniformName == "u_texture2EnvAlphaScale")
            {
                id = UniformId.TEXTURE2_ENV_ALPHA_SCALE;
            }
            else if (uniformName == "u_texture0EnvBlurAmount")
            {
                id = UniformId.TEXTURE0_ENV_BLUR_AMOUNT;
            }
            else if (uniformName == "u_texture1EnvBlurAmount")
            {
                id = UniformId.TEXTURE1_ENV_BLUR_AMOUNT;
            }
            else if (uniformName == "u_texture2EnvBlurAmount")
            {
                id = UniformId.TEXTURE2_ENV_BLUR_AMOUNT;
            }
            else if (uniformName == "u_texture0EnvCombineRGB")
            {
                id = UniformId.TEXTURE0_ENV_COMBINE_RGB;
            }
            else if (uniformName == "u_texture1EnvCombineRGB")
            {
                id = UniformId.TEXTURE1_ENV_COMBINE_RGB;
            }
            else if (uniformName == "u_texture2EnvCombineRGB")
            {
                id = UniformId.TEXTURE2_ENV_COMBINE_RGB;
            }
            else if (uniformName == "u_texture0EnvCombineAlpha")
            {
                id = UniformId.TEXTURE0_ENV_COMBINE_ALPHA;
            }
            else if (uniformName == "u_texture1EnvCombineAlpha")
            {
                id = UniformId.TEXTURE1_ENV_COMBINE_ALPHA;
            }
            else if (uniformName == "u_texture2EnvCombineAlpha")
            {
                id = UniformId.TEXTURE2_ENV_COMBINE_ALPHA;
            }
            else if (uniformName == "u_fogEnabled")
            {
                id = UniformId.FOG_ENABLED;
            }
            else if (uniformName == "u_fogColor")
            {
                id = UniformId.FOG_COLOR;
            }
            else if (uniformName == "u_fogMode")
            {
                id = UniformId.FOG_MODE;
            }
            else if (uniformName == "u_fogDensity")
            {
                id = UniformId.FOG_DENSITY;
            }
            else if (uniformName == "u_fogStart")
            {
                id = UniformId.FOG_START;
            }
            else if (uniformName == "u_fogEnd")
            {
                id = UniformId.FOG_END;
            }
            else if (uniformName == "u_fogHint")
            {
                id = UniformId.FOG_HINT;
            }
            else if (uniformName == "u_lightingHint")
            {
                id = UniformId.LIGHTING_HINT;
            }
            else if (uniformName == "u_clipPlane0Enabled")
            {
                id = UniformId.CLIP_PLANE0_ENABLED;
            }
            else if (uniformName == "u_clipPlane1Enabled")
            {
                id = UniformId.CLIP_PLANE1_ENABLED;
            }
            else if (uniformName == "u_clipPlane2Enabled")
            {
                id = UniformId.CLIP_PLANE2_ENABLED;
            }
            else if (uniformName == "u_clipPlane3Enabled")
            {
                id = UniformId.CLIP_PLANE3_ENABLED;
            }
            else if (uniformName == "u_clipPlane4Enabled")
            {
                id = UniformId.CLIP_PLANE4_ENABLED;
            }
            else if (uniformName == "u_clipPlane5Enabled")
            {
                id = UniformId.CLIP_PLANE5_ENABLED;
            }
            else if (uniformName == "u_clipPlane0Equation")
            {
                id = UniformId.CLIP_PLANE0_EQUATION;
            }
            else if (uniformName == "u_clipPlane1Equation")
            {
                id = UniformId.CLIP_PLANE1_EQUATION;
            }
            else if (uniformName == "u_clipPlane2Equation")
            {
                id = UniformId.CLIP_PLANE2_EQUATION;
            }
            else if (uniformName == "u_clipPlane3Equation")
            {
                id = UniformId.CLIP_PLANE3_EQUATION;
            }
            else if (uniformName == "u_clipPlane4Equation")
            {
                id = UniformId.CLIP_PLANE4_EQUATION;
            }
            else if (uniformName == "u_clipPlane5Equation")
            {
                id = UniformId.CLIP_PLANE5_EQUATION;
            }
			else
			{
				Log.e(TAG, "ERROR: Missing " + uniformName);
				return 0;
			}

			uniforms.add(new UniformSimple(id, uniformLocation));

			String typeString;
			switch (uniformType[0])
			{
				case GLES20.GL_FLOAT:
					typeString = "GL_FLOAT";
					break;
				case GLES20.GL_FLOAT_VEC2:
					typeString = "GL_FLOAT_VEC2";
					break;
				case GLES20.GL_FLOAT_VEC3:
					typeString = "GL_FLOAT_VEC3";
					break;
				case GLES20.GL_FLOAT_VEC4:
					typeString = "GL_FLOAT_VEC4";
					break;
				case GLES20.GL_INT:
					typeString = "GL_INT";
					break;
				case GLES20.GL_INT_VEC2:
					typeString = "GL_INT_VEC2";
					break;
				case GLES20.GL_INT_VEC3:
					typeString = "GL_INT_VEC3";
					break;
				case GLES20.GL_INT_VEC4:
					typeString = "GL_INT_VEC4";
					break;
				case GLES20.GL_BOOL:
					typeString = "GL_BOOL";
					break;
				case GLES20.GL_BOOL_VEC2:
					typeString = "GL_BOOL_VEC2";
					break;
				case GLES20.GL_BOOL_VEC3:
					typeString = "GL_BOOL_VEC3";
					break;
				case GLES20.GL_BOOL_VEC4:
					typeString = "GL_BOOL_VEC4";
					break;
				case GLES20.GL_FLOAT_MAT2:
					typeString = "GL_FLOAT_MAT2";
					break;
				case GLES20.GL_FLOAT_MAT3:
					typeString = "GL_FLOAT_MAT3";
					break;
				case GLES20.GL_FLOAT_MAT4:
					typeString = "GL_FLOAT_MAT4";
					break;
				case GLES20.GL_SAMPLER_2D:
					typeString = "GL_SAMPLER_2D";
					break;
				case GLES20.GL_SAMPLER_CUBE:
					typeString = "GL_SAMPLER_CUBE";
					break;
				default:
					typeString = "Unknown";
					Log.e(TAG, "ERROR: Unknown type " + uniformType);
					break;
			}

			Log.d(TAG, uniformName + ": type " + typeString + " location: " + uniformLocation);
		}

		return program;
	}
	
	private int getUniformLocation(String uniformName)
	{
		int res = GLES20.glGetUniformLocation(program, uniformName);

		if (res == -1)
		{
			Log.e(TAG, "ERROR: Unknown uniform " + uniformName + " in program " + name);
		}

		return res;
	}
	
	private int getAttributeLocation(String attribName)
	{
		int res = GLES20.glGetAttribLocation(program, attribName);

		if (res == -1)
		{
			Log.e(TAG, "ERROR: Unknown attribute " + attribName + " in program " + name);
		}

		return res;
	}
}
