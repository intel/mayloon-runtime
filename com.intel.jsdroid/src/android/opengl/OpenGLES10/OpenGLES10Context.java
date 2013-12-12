package android.opengl.OpenGLES10;

import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLES20;
import android.util.Log;

import java.util.Arrays;

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


public class OpenGLES10Context
{
    private static final String TAG = "OpenGLES10Context";
    

    private MatrixStack matrixStack = null;
    private OpenGLESState openGLESState = null;
    private int shaderProgramId;
    
    public int colorReadFormat;
    public int colorReadType;
    public int maxCombinedTextureImageUnits;
    public int maxCubeMapTextureSize;
    public int maxFragmentUniformVectors;
    public int maxRenderBufferSize;
    public int maxTextureImageUnits;
    public int maxTextureSize;
    public int maxVaryingVectors;
    public int maxVertexAttribs;
    public int maxVertexTextureImageUnits;
    public int maxVertexUniformVectors;
    public int[] maxViewportDims = new int[2];
    public int numCompressedTextureFormats;
    public int numShaderBinaryFormats;
    public int[] shaderBinaryFormats = null;
    public boolean shaderCompilerSupported;
    public int depthBits;
    public int stencilBits;

	public OpenGLES10Context()
	{
		this.matrixStack = new MatrixStack(openGLESState, this);
		this.openGLESState = new OpenGLESState();
		this.shaderProgramId = 0;
		this.init();
		matrixStack.init();
		openGLESState.init(this);
	}
	
	private void init() {
	    int[] value = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_IMPLEMENTATION_COLOR_READ_FORMAT, value, 0);
        colorReadFormat = value[0];
        GLES20.glGetIntegerv(GLES20.GL_IMPLEMENTATION_COLOR_READ_TYPE, value, 0);
        colorReadType = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, value, 0);
        maxCombinedTextureImageUnits = value[0];
        maxCombinedTextureImageUnits = Math.min(3, maxCombinedTextureImageUnits); // TODO: currently shaders support 3 textures
        GLES20.glGetIntegerv(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE, value, 0);
        maxCubeMapTextureSize = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, value, 0);
        maxFragmentUniformVectors = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, value, 0);
        maxRenderBufferSize = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, value, 0);
        maxTextureSize = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, value, 0);
        maxTextureImageUnits = value[0];
        maxTextureImageUnits = Math.min(3, maxTextureImageUnits); // TODO: currently shaders support 3 textures
        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, value, 0);
        maxVaryingVectors = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, value, 0);
        maxVertexAttribs = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, value, 0);
        maxVertexTextureImageUnits = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS, value, 0);
        maxVertexUniformVectors = value[0];
        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, maxViewportDims, 0);
        GLES20.glGetIntegerv(GLES20.GL_NUM_COMPRESSED_TEXTURE_FORMATS, value, 0);
        numCompressedTextureFormats = value[0];
        GLES20.glGetIntegerv(GLES20.GL_NUM_SHADER_BINARY_FORMATS, value, 0);
        numShaderBinaryFormats = value[0];
        shaderBinaryFormats = new int[numShaderBinaryFormats];
        GLES20.glGetIntegerv(GLES20.GL_SHADER_BINARY_FORMATS, shaderBinaryFormats, 0);

        boolean[] tmp = new boolean[1];
        GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER, tmp, 0);
        shaderCompilerSupported = tmp[0];

        GLES20.glGetIntegerv(GLES20.GL_DEPTH_BITS, value, 0);
        depthBits = value[0];
        GLES20.glGetIntegerv(GLES20.GL_STENCIL_BITS, value, 0);
        stencilBits = value[0];

        print();
	}
	
	private void print() {
        Log.d(TAG, "OpenGL Implementation Details:");
        Log.d(TAG, "------------------------------");
        Log.d(TAG, "Max viewport dimensions: " + maxViewportDims[0] + "*"+ maxViewportDims[1]);
        Log.d(TAG, "Depth bits: " + depthBits);
        Log.d(TAG, "Stencil bits: " + stencilBits);
        Log.d(TAG, "Color read format: " + colorReadFormat);
        Log.d(TAG, "Color read type: " + colorReadType);
        Log.d(TAG, "Max render buffer size: " + maxRenderBufferSize);
        Log.d(TAG, "Max texture size: " + maxTextureSize);
        Log.d(TAG, "Number of compressed texture formats: " + numCompressedTextureFormats);
        Log.d(TAG, "Max combined texture image units: " + maxCombinedTextureImageUnits);
        Log.d(TAG, "Max cubemap texture size: " + maxCubeMapTextureSize);
        Log.d(TAG, "Shader compiler support: " + shaderCompilerSupported);
        Log.d(TAG, "Number of shader binary formats: " + numShaderBinaryFormats);
        for (int i = 0; i < numShaderBinaryFormats; i++)
        {
            Log.d(TAG, "Supported shader binary format: " + shaderBinaryFormats[i]);
        }
        Log.d(TAG, "Max vertex attributes: " + maxVertexAttribs);
        Log.d(TAG, "Max vertex uniform vectors: " + maxVertexUniformVectors);
        Log.d(TAG, "Max varying vectors: " + maxVaryingVectors);
        Log.d(TAG, "Max fragment uniform vectors: " + maxFragmentUniformVectors);
        Log.d(TAG, "Max texture image units: " + maxTextureImageUnits);
        Log.d(TAG, "Max vertex texture image units: " + maxVertexTextureImageUnits);
        Log.d(TAG, "------------------------------"); 
	}
	
	public void dispose()
	{

	}

	// OpenglES 1.0 functions
	 
	public void glActiveTexture(int texture)
	{
		openGLESState.setActiveTexture(texture - GLES10.GL_TEXTURE0);
		GLES20.glActiveTexture(texture);
	}

	public void glAlphaFunc(int func, float ref)
	{
		openGLESState.setAlphaFunc(func);
		openGLESState.setAlphaFuncValue(Math.max(Math.min(1, ref), 0));
	}

	 
	public void glAlphaFuncx(int func, int ref)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}

	 
	public void glBindTexture(int target, int texture)
	{
		openGLESState.setBoundTexture(texture);
		openGLESState.setTextureFormat();
		GLES20.glBindTexture(target, texture);
	}

	 
	public void glBlendFunc(int sfactor, int dfactor)
	{
	    GLES20.glBlendFunc(sfactor, dfactor);
	}

	 
	public void glClear(int mask)
	{
	    GLES20.glClear(mask);
	}
	 
	public void glClearColor(float red, float green, float blue, float alpha)
	{
	    GLES20.glClearColor(red, green, blue, alpha);
	}
	 
	public void glClearColorx(int red, int green, int blue, int alpha)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glClearDepthf(float depth)
	{
	    GLES20.glClearDepthf(depth);
	}
	 
	public void glClearDepthx(int depth)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glClearStencil(int s)
	{
	    GLES20.glClearStencil(s);
	}
	 
	public void glClientActiveTexture(int texture)
	{
		openGLESState.setClientActiveTexture(texture - GLES10.GL_TEXTURE0);
	}
	 
	public void glColor4f(float red, float green, float blue, float alpha)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glColor4x(int red, int green, int blue, int alpha)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha)
	{
	    GLES20.glColorMask(red, green, blue, alpha);
	}
	 
	public void glColorPointer(int size, int type, int stride, java.nio.Buffer pointer)
	{
		openGLESState.setColor(size, type, stride, pointer);
	}
	 
	public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Object data)
	{
	    Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Object data)
	{
	    Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border)
	{
	    GLES20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
	}
	 
	public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height)
	{
	    GLES20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
	}
	 
	public void glCullFace(int mode)
	{
	    GLES20.glCullFace(mode);
	}
	 
	public void glDeleteTextures(int n, int[] textures, int offset)
	{
	    GLES20.glDeleteTextures(n, textures, offset);
	}
	 
	public void glDepthFunc(int func)
	{
	    GLES20.glDepthFunc(func);
	}
	 
	public void glDepthMask(boolean flag)
	{
	    GLES20.glDepthMask(flag);
	}
	 
	public void glDepthRangef(float zNear, float zFar)
	{
	    GLES20.glDepthRangef(zNear, zFar);
	}
	 
	public void glDepthRangex(int zNear, int zFar)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glDisable(int cap)
	{
		switch (cap)
		{
			case GLES10.GL_LIGHTING:
				openGLESState.setLighting(false);
				break;
			case GLES10.GL_LIGHT0:
			case GLES10.GL_LIGHT1:
			case GLES10.GL_LIGHT2:
			case GLES10.GL_LIGHT3:
			case GLES10.GL_LIGHT4:
			case GLES10.GL_LIGHT5:
			case GLES10.GL_LIGHT6:
			case GLES10.GL_LIGHT7:
				openGLESState.setLight(cap - GLES10.GL_LIGHT0, false);
				break;
			case GLES10.GL_TEXTURE_2D:
				openGLESState.setTexture(false);
				break;
			case GLES10.GL_CULL_FACE:
			case GLES10.GL_BLEND:
			case GLES10.GL_DITHER:
			case GLES10.GL_STENCIL_TEST:
			case GLES10.GL_DEPTH_TEST:
			case GLES10.GL_SCISSOR_TEST:
			case GLES10.GL_POLYGON_OFFSET_FILL:
			case GLES10.GL_SAMPLE_ALPHA_TO_COVERAGE:
			case GLES10.GL_SAMPLE_COVERAGE:
			    GLES20.glDisable(cap);
				break;
			case GLES10.GL_NORMALIZE:
				openGLESState.setNormalize(false);
				break;
			case GLES10.GL_RESCALE_NORMAL:
				openGLESState.setRescaleNormal(false);
				break;
			case GLES10.GL_FOG:
				openGLESState.setFog(false);
				break;
			case GLES10.GL_ALPHA_TEST:
				openGLESState.setAlphaTest(false);
				break;
			case GLES11.GL_CLIP_PLANE0:
			case GLES11.GL_CLIP_PLANE1:
			case GLES11.GL_CLIP_PLANE2:
			case GLES11.GL_CLIP_PLANE3:
			case GLES11.GL_CLIP_PLANE4:
			case GLES11.GL_CLIP_PLANE5:
				openGLESState.setClipPlane(cap - GLES11.GL_CLIP_PLANE0, false);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown cap " + cap);
				break;
		}
	}
	 
	public void glDisableClientState(int array)
	{
		switch (array)
		{
			case GLES10.GL_VERTEX_ARRAY:
				openGLESState.setPosition(false);
				break;
			case GLES10.GL_COLOR_ARRAY:
				openGLESState.setColor(false);
				break;
			case GLES10.GL_NORMAL_ARRAY:
				openGLESState.setNormal(false);
				break;
			case GLES10.GL_TEXTURE_COORD_ARRAY:
				openGLESState.setTexCoord(false);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown argument " + array);
				break;
		}
	}
	 
	public void glDrawArrays(int mode, int first, int count)
	{
		if (shaderProgramId == 0)
		{
			prepareToDraw();
		}
		GLES20.glDrawArrays(mode, first, count);
	}
	 
	public void glDrawElements(int mode, int count, int type, java.nio.Buffer indices)
	{
		if (shaderProgramId == 0)
		{
			prepareToDraw();
		}
		GLES20.glDrawElements(mode, count, type, indices);
	}
	 
	public void glEnable(int cap)
	{
		switch (cap)
		{
			case GLES10.GL_LIGHTING:
				openGLESState.setLighting(true);
				break;
			case GLES10.GL_LIGHT0:
			case GLES10.GL_LIGHT1:
			case GLES10.GL_LIGHT2:
			case GLES10.GL_LIGHT3:
			case GLES10.GL_LIGHT4:
			case GLES10.GL_LIGHT5:
			case GLES10.GL_LIGHT6:
			case GLES10.GL_LIGHT7:
			{
				openGLESState.setLight(cap - GLES10.GL_LIGHT0, true);
			}
				break;
			case GLES10.GL_TEXTURE_2D:
				openGLESState.setTexture(true);
				break;
			case GLES10.GL_CULL_FACE:
			case GLES10.GL_BLEND:
			case GLES10.GL_DITHER:
			case GLES10.GL_STENCIL_TEST:
			case GLES10.GL_DEPTH_TEST:
			case GLES10.GL_SCISSOR_TEST:
			case GLES10.GL_POLYGON_OFFSET_FILL:
			case GLES10.GL_SAMPLE_ALPHA_TO_COVERAGE:
			case GLES10.GL_SAMPLE_COVERAGE:
			    GLES20.glEnable(cap);
				break;
			case GLES10.GL_NORMALIZE:
				openGLESState.setNormalize(true);
				break;
			case GLES10.GL_FOG:
				openGLESState.setFog(true);
				break;
			case GLES10.GL_RESCALE_NORMAL:
				openGLESState.setRescaleNormal(true);
				break;
			case GLES10.GL_ALPHA_TEST:
				openGLESState.setAlphaTest(true);
				break;
			case GLES11.GL_CLIP_PLANE0:
			case GLES11.GL_CLIP_PLANE1:
			case GLES11.GL_CLIP_PLANE2:
			case GLES11.GL_CLIP_PLANE3:
			case GLES11.GL_CLIP_PLANE4:
			case GLES11.GL_CLIP_PLANE5:
				openGLESState.setClipPlane(cap - GLES11.GL_CLIP_PLANE0, true);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown cap " + cap);
				break;
		}
	}
	 
	public void glEnableClientState(int array)
	{
		switch (array)
		{
			case GLES10.GL_VERTEX_ARRAY:
				openGLESState.setPosition(true);
				break;
			case GLES10.GL_COLOR_ARRAY:
				openGLESState.setColor(true);
				break;
			case GLES10.GL_NORMAL_ARRAY:
				openGLESState.setNormal(true);
				break;
			case GLES10.GL_TEXTURE_COORD_ARRAY:
				openGLESState.setTexCoord(true);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown argument " + array);
				break;
		}
	}
	 
	public void glFinish()
	{
	    GLES20.glFinish();
	}
	 
	public void glFlush()
	{
	    GLES20.glFlush();
	}
	 
	public void glFogf(int pname, float param)
	{
		switch (pname)
		{
			case GLES10.GL_FOG_MODE:
			{
				int p = (int)param;
				if (p == GLES10.GL_LINEAR || p == GLES10.GL_EXP || p == GLES10.GL_EXP2)
				{
					openGLESState.setFogMode(p);
				}
				else
				{
					Log.d(TAG, "ERROR: Unknown fog mode " + param);
				}
				break;
			}
			case GLES10.GL_FOG_DENSITY:
				openGLESState.setFogDensity(param);
				break;
			case GLES10.GL_FOG_START:
				openGLESState.setFogStart(param);
				break;
			case GLES10.GL_FOG_END:
				openGLESState.setFogEnd(param);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown fog parameter " + pname);
				break;
		}
	}
	 
	public void glFogfv(int pname, float[] params, int offset)
	{
		switch (pname)
		{
			case GLES10.GL_FOG_COLOR:
				openGLESState.setFogColor(new Vector3f(Arrays.copyOfRange(params, offset, offset + 3)));
				break;
			default:
				Log.d(TAG, "ERROR: Unknown fog parameter " + pname);
				break;
		}
	}
	 
	public void glFogx(int pname, int param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glFogxv(int pname, int params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glFrontFace(int mode)
	{
		GLES20.glFrontFace(mode);
	}
	 
	public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar)
	{
		matrixStack.frustum(left, right, bottom, top, zNear, zFar);
	}
	 
	public void glFrustumx(int left, int right, int bottom, int top, int zNear, int zFar)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGenTextures(int n, int[] textures, int offset)
	{
	    GLES20.glGenTextures(n, textures, offset);
	}
	 
	public int glGetError()
	{
		return GLES20.glGetError();
	}
	 
	public void glGetIntegerv(int pname, int[] params, int offset)
	{
		GLES20.glGetIntegerv(pname, params, offset);
	}
	 
	public String glGetString(int name)
	{
		return GLES20.glGetString(name);
	}
	 
	public void glHint(int target, int mode)
	{
		switch (target)
		{
			case GLES10.GL_FOG_HINT:
				openGLESState.setFogHint(mode);
				break;
//			case GLES10.GL_LIGHTING_HINT:
//				openGLESState.setLightingHint(mode);
//				break;
			default:
				GLES20.glHint(target, mode);
				break;
		}
	}
	 
	public void glLightModelf(int pname, float param)
	{
		switch (pname)
		{
			case GLES10.GL_LIGHT_MODEL_TWO_SIDE:
				openGLESState.setLightModelTwoSide(param != 0);
				break;
//			case GLES10.GL_LIGHT_MODEL_LOCAL_VIEWER:
//				openGLESState.setLightModelLocalViewer(param != 0);
//				break;
			default:
				Log.d(TAG, "ERROR: Unknown light model" + pname);
				break;
		}
	}
	 
	public void glLightModelfv(int pname, float[] params, int offset)
	{
		switch (pname)
		{
			case GLES10.GL_LIGHT_MODEL_AMBIENT:
				openGLESState.setGlobalAmbientColor(new Vector4f(Arrays.copyOfRange(params, offset, offset + 4)));
				break;
			default:
				Log.d(TAG, "ERROR: Unknown light model" + pname);
				break;
		}
	}
	 
	public void glLightModelx(int pname, int param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glLightModelxv(int pname, int params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glLightf(int l, int pname, float param)
	{
		int lightIndex = l - GLES10.GL_LIGHT0;
		switch (pname)
		{
			case GLES10.GL_SPOT_EXPONENT:
				openGLESState.setLightSpotExponent(lightIndex, param);
				if (true)
				{
					if (param > 128)
					{
						Log.d(TAG, "ERROR: Spot exponent cannot be over 128");
					}
				}
				break;
			case GLES10.GL_SPOT_CUTOFF:
				openGLESState.setLightSpotCutoffAngleCos(lightIndex, (float) Math.cos(param * Math.PI / 180.0f));
				if (true)
				{
					if (param > 90 && param != 180)
					{
						Log.d(TAG, "ERROR: Spot cutoff cannot be over 90 and different from 180.");
					}
				}
				break;
			case GLES10.GL_CONSTANT_ATTENUATION:
				openGLESState.setLightConstantAttenuation(lightIndex, param);
				break;
			case GLES10.GL_LINEAR_ATTENUATION:
				openGLESState.setLightLinearAttenuation(lightIndex, param);
				break;
			case GLES10.GL_QUADRATIC_ATTENUATION:
				openGLESState.setLightQuadraticAttenuation(lightIndex, param);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown light parameter " + pname);
				break;
		}
	}
	 
	public void glLightfv(int l, int pname, float[] params, int offset)
	{
		int lightIndex = l - GLES10.GL_LIGHT0;
		Vector4f value = new Vector4f(Arrays.copyOfRange(params, offset, params.length));
		switch (pname)
		{
			case GLES10.GL_AMBIENT:
				openGLESState.setLightAmbient(lightIndex, value);
				break;
			case GLES10.GL_DIFFUSE:
				openGLESState.setLightDiffuse(lightIndex, value);
				break;
			case GLES10.GL_SPECULAR:
				openGLESState.setLightSpecular(lightIndex, value);
				break;
			case GLES10.GL_POSITION:
			{
				Matrix4x4f modelViewMatrix = matrixStack.getModelViewMatrix();
				Vector4f vec = new Vector4f(Arrays.copyOfRange(params, offset, params.length));
				vec = OpenGLESMath.multiply(modelViewMatrix, vec);
				openGLESState.setLightPosition(lightIndex,vec);

				if (true)
				{
					if (vec.getItem(3) == 0.0f && !OpenGLESMath.isUnitVector(vec))
					{
						Log.d(TAG, "ERROR: Directional light's position is not unit vector.");
					}
				}
			}
				break;
			case GLES10.GL_SPOT_DIRECTION:
			{
				Matrix4x4f modelViewMatrix = matrixStack.getModelViewMatrix();
				Matrix3x3f modelViewMatrix3x3 = new Matrix3x3f();
				modelViewMatrix3x3 = OpenGLESMath.copyMatrix4x4UpperLeftToMatrix3x3(modelViewMatrix);
				Vector3f vec = new Vector3f(Arrays.copyOfRange(params, offset, params.length));
				vec = OpenGLESMath.multiply(modelViewMatrix3x3, vec);
				openGLESState.setLightSpotDirection(lightIndex, vec);
			}
				break;
			default:
				Log.d(TAG, "ERROR: Unknown light parameter " + pname);
				break;
		}
	}
	 
	public void glLightx(int light, int pname, int param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glLightxv(int light, int pname, int params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glLineWidth(float width)
	{
	    GLES20.glLineWidth(width);
	}
	 
	public void glLineWidthx(int width)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glLoadIdentity()
	{
		matrixStack.loadIdentity();
	}
	 
	public void glLoadMatrixf(float[] m, int offset)
	{
		matrixStack.loadMatrix(Arrays.copyOfRange(m, offset, m.length));
	}
	 
	public void glLoadMatrixx(int m)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glLogicOp(int opcode)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glMaterialf(int face, int pname, float param)
	{
		switch (pname)
		{
			case GLES10.GL_SHININESS:
				openGLESState.setMaterialShininess(param);
				if (true)
				{
					if (param > 128)
					{
						Log.d(TAG, "ERROR: Shininess cannot be over 128");
					}
				}
				break;
			default:
				Log.d(TAG, "ERROR: Unknown material parameter " + pname);
				break;
		}
	}
	 
	public void glMaterialfv(int face, int pname, float[] params, int offset)
	{
	    Vector4f vec = new Vector4f(Arrays.copyOfRange(params, offset, offset + 4));
		switch (pname)
		{
			case GLES10.GL_AMBIENT:
				openGLESState.setMaterialAmbient(vec);
				break;
			case GLES10.GL_DIFFUSE:
				openGLESState.setMaterialDiffuse(vec);
				break;
			case GLES10.GL_SPECULAR:
				openGLESState.setMaterialSpecular(vec);
				break;
			case GLES10.GL_EMISSION:
				openGLESState.setMaterialEmission(vec);
				break;
			case GLES10.GL_AMBIENT_AND_DIFFUSE:
				openGLESState.setMaterialAmbient(vec);
				openGLESState.setMaterialDiffuse(vec);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown material parameter " + pname);
				break;
		}

	}
	 
	public void glMaterialx(int face, int pname, int param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glMaterialxv(int face, int pname, int params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glMatrixMode(int mode)
	{
		matrixStack.setMatrixMode(mode);
	}
	 
	public void glMultMatrixf(float[] m, int offset)
	{
		matrixStack.multiply(Arrays.copyOfRange(m, offset, m.length));
	}
	 
	public void glMultMatrixx(int m)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glMultiTexCoord4f(int target, float s, float t, float r, float q)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glMultiTexCoord4x(int target, int s, int t, int r, int q)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glNormal3f(float nx, float ny, float nz)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glNormal3x(int nx, int ny, int nz)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glNormalPointer(int type, int stride, java.nio.Buffer pointer)
	{
		openGLESState.setNormal(3, type, stride, pointer);
	}
	 
	public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar)
	{
		matrixStack.ortho(left, right, bottom, top, zNear, zFar);
	}
	 
	public void glOrthox(int left, int right, int bottom, int top, int zNear, int zFar)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glPixelStorei(int pname, int param)
	{
		glPixelStorei(pname, param);
	}
	 
	public void glPointSize(float size)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glPointSizex(int size)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glPolygonOffset(float factor, float units)
	{
		glPolygonOffset(factor, units);
	}
	 
	public void glPolygonOffsetx(int factor, int units)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glPopMatrix()
	{
		matrixStack.popMatrix();
	}
	 
	public void glPushMatrix()
	{
		matrixStack.pushMatrix();
	}
	 
	public void glReadPixels(int x, int y, int width, int height, int format, int type, java.nio.Buffer pixels)
	{
	    GLES20.glReadPixels(x, y, width, height, format, type, pixels);
	}
	 
	public void glRotatef(float angle, float x, float y, float z)
	{
		matrixStack.rotate(-angle, x, y, z);
	}
	 
	public void glRotatex(int angle, int x, int y, int z)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glSampleCoverage(float value, boolean invert)
	{
	    GLES20.glSampleCoverage(value, invert);
	}
	 
	public void glSampleCoveragex(int value, byte invert)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glScalef(float x, float y, float z)
	{
		matrixStack.scale(x, y, z);
	}
	 
	public void glScalex(int x, int y, int z)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glScissor(int x, int y, int width, int height)
	{
	    GLES20.glScissor(x, y, width, height);
	}
	 
	public void glShadeModel(int mode)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glStencilFunc(int func, int ref, int mask)
	{
	    GLES20.glStencilFunc(func, ref, mask);
	}
	 
	public void glStencilMask(int mask)
	{
	    GLES20.glStencilMask(mask);
	}
	 
	public void glStencilOp(int fail, int zfail, int zpass)
	{
	    GLES20.glStencilOp(fail, zfail, zpass);
	}
	 
	public void glTexCoordPointer(int size, int type, int stride, java.nio.Buffer pointer)
	{
		openGLESState.setTexCoord(size, type, stride, pointer);
	}
	 
	public void glTexEnvf(int target, int pname, float param)
	{
//		switch (pname)
//		{
////			case GLES10.GL_BLUR_AMOUNT:
////				openGLESState.setTextureEnvBlurAmount(param);
////				break;
//			default:
//				GLES20.glTexEnvi(target, pname, (int)(param));
//				break;
//		}
	    Log.d(TAG, "Not implemented");
	}
	 
	public void glTexEnvfv(int target, int pname, float[] params)
	{
//		switch (pname)
//		{
//			case GLES10.GL_TEXTURE_ENV_COLOR:
//				openGLESState.setTextureEnvColor(params);
//				break;
//			default:
//			    GLES20.glTexEnvi(target, pname, (int)(params[0]));
//				break;
//		}
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glTexEnvx(int target, int pname, int param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glTexEnvxv(int target, int pname, int params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, java.nio.Buffer pixels)
	{
		openGLESState.setBoundTextureFormat(internalformat);
		openGLESState.setTextureFormat();
		GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
	}
	 
	public void glTexParameterf(int target, int pname, float param)
	{
	    GLES20.glTexParameterf(target, pname, param);
	}
	 
	public void glTexParameterx(int target, int pname, int param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, java.nio.Buffer pixels)
	{
	    GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
	}
	 
	public void glTranslatef(float x, float y, float z)
	{
		matrixStack.translate(x, y, z);
	}
	 
	public void glTranslatex(int x, int y, int z)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glVertexPointer(int size, int type, int stride, java.nio.Buffer pointer)
	{
		openGLESState.setPosition(size, type, stride, pointer);
	}
	 
	public void glViewport(int x, int y, int width, int height)
	{
	    GLES20.glViewport(x, y, width, height);
	}

	// OpenGL ES 1.1 functions

	// OpenGL ES 1.1 functions
	 
	public void glBindBuffer(int target, int buffer)
	{
	    GLES20.glBindBuffer(target, buffer);
	}
	 
	public void glBufferData(int target, int size, java.nio.Buffer data, int usage)
	{
	    GLES20.glBufferData(target, size, data, usage);
	}
	 
	public void glBufferSubData(int target, int offset, int size, java.nio.Buffer data)
	{
	    GLES20.glBufferSubData(target, offset, size, data);
	}
	 
	public void glClipPlanef(int plane, float[] equation, int offset)
	{
		Matrix4x4f tranposeInverseModelViewMatrix = new Matrix4x4f();
		tranposeInverseModelViewMatrix = OpenGLESMath.inverse(matrixStack.getModelViewMatrix()); // TODO: calculated also before drawing, optimize?
		tranposeInverseModelViewMatrix = OpenGLESMath.transpose(tranposeInverseModelViewMatrix);
		Vector4f clipPlane = new Vector4f(Arrays.copyOfRange(equation, offset, offset + 4));
		clipPlane = OpenGLESMath.multiply(tranposeInverseModelViewMatrix, clipPlane);

		openGLESState.setClipPlane(plane - GLES11.GL_CLIP_PLANE0, clipPlane);
	}
	 
	public void glClipPlanex(int plane, int equation)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glColor4ub(byte red, byte green, byte blue, byte alpha)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glDeleteBuffers(int n, int[] buffers, int offset)
	{
	    GLES20.glDeleteBuffers(n, buffers, offset);
	}
	 
	public void glGenBuffers(int n, int[] buffers, int offset)
	{
	    GLES20.glGenBuffers(n, buffers, offset);
	}
	 
	public void glGetClipPlanef(int pname, float[] eqn)
	{
		openGLESState.getClipPlane(pname - GLES11.GL_CLIP_PLANE0, eqn);
	}
	 
	public void glGetFloatv(int pname, float[] params, int offset)
	{
		switch (pname)
		{
			case GLES11.GL_MODELVIEW_MATRIX:
				for (int i = 0; i < 16; i++)
				{
					params[i] = matrixStack.getModelViewMatrix().m[i];
				}
				break;
			default:
				GLES20.glGetFloatv(pname, params, offset);
				break;
		}

	}
	 
	public void glGetLightfv(int light, int pname, float[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetLightxv(int light, int pname, int[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetMaterialfv(int face, int pname, float[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetMaterialxv(int face, int pname, int[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetTexEnvfv(int env, int pname, float[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetTexEnviv(int env, int pname, int[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetTexEnvxv(int env, int pname, int[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetTexParameterfv(int target, int pname, float[] params, int offset)
	{
		GLES20.glGetTexParameterfv(target, pname, params, offset);
	}
	 
	public void glGetTexParameteriv(int target, int pname, int[] params, int offset)
	{
		GLES20.glGetTexParameteriv(target, pname, params, offset);
	}
	 
	public void glGetTexParameterxv(int target, int pname, int[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetBooleanv(int pname, int[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetFixedv(int pname, int[] params, int offset)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glGetPointerv(int pname, Object[]params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public boolean glIsBuffer(int buffer)
	{
		return GLES20.glIsBuffer(buffer);
	}
	 
	public boolean glIsEnabled(int cap)
	{
		return GLES20.glIsEnabled(cap);
	}
	 
	public boolean glIsTexture(int texture)
	{
		return GLES20.glIsTexture(texture);
	}
	 
	public void glPointParameterf(int pname, float param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glPointParameterfv(int pname, float params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glPointParameterx(int pname, int param)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glPointParameterxv(int pname, int params)
	{
		Log.d(TAG, "ERROR: Not implemented.");
	}
	 
	public void glTexEnvi(int target, int pname, int param)
	{
		switch (pname)
		{
			case GLES10.GL_TEXTURE_ENV_MODE:
				switch (param)
				{
					case GLES10.GL_MODULATE:
					case GLES10.GL_ADD:
					case GLES10.GL_DECAL:
					case GLES10.GL_BLEND:
					case GLES10.GL_REPLACE:
					case GLES11.GL_COMBINE:
					//case GLES11.GL_BLUR:
						openGLESState.setTextureEnvMode(param);
						break;
					default:
						Log.d(TAG, "ERROR: Unknown GL_TEXTURE_ENV_MODE parameter " + param);
						break;
				}
				break;
			case GLES11.GL_COMBINE_RGB:
				switch (param)
				{
					case GLES10.GL_REPLACE:
					case GLES10.GL_MODULATE:
					case GLES10.GL_ADD:
					case GLES11.GL_ADD_SIGNED:
					case GLES11.GL_INTERPOLATE:
					case GLES11.GL_SUBTRACT:
					case GLES11.GL_DOT3_RGB:
					case GLES11.GL_DOT3_RGBA:
						openGLESState.setTextureEnvCombineRGB(param);
						break;
					default:
						Log.d(TAG, "ERROR: Unknown GL_COMBINE_RGB parameter " + param);
						break;
				}
				break;
			case GLES11.GL_COMBINE_ALPHA:
				switch (param)
				{
					case GLES10.GL_REPLACE:
					case GLES10.GL_MODULATE:
					case GLES10.GL_ADD:
					case GLES11.GL_ADD_SIGNED:
					case GLES11.GL_INTERPOLATE:
					case GLES11.GL_SUBTRACT:
						openGLESState.setTextureEnvCombineAlpha(param);
						break;
					default:
						Log.d(TAG, "ERROR: Unknown GL_COMBINE_ALPHA parameter " + param);
						break;
				}
				break;
			case GLES11.GL_SRC0_RGB:
			case GLES11.GL_SRC1_RGB:
			case GLES11.GL_SRC2_RGB:
				if (GLES10.GL_TEXTURE0 <= param && param <= GLES10.GL_TEXTURE31)
				{
					openGLESState.setTextureEnvSrcRGB(pname - GLES11.GL_SRC0_RGB, param - GLES10.GL_TEXTURE0);
				}
				else if (param == GLES10.GL_TEXTURE)
				{
					openGLESState.setTextureEnvSrcRGB(pname - GLES11.GL_SRC0_RGB, openGLESState.getActiveTexture());
				}
				else
				{
					openGLESState.setTextureEnvSrcRGB(pname - GLES11.GL_SRC0_RGB, param);
				}
				break;
			case GLES11.GL_SRC0_ALPHA:
			case GLES11.GL_SRC1_ALPHA:
			case GLES11.GL_SRC2_ALPHA:
				if (GLES10.GL_TEXTURE0 <= param && param <= GLES10.GL_TEXTURE31)
				{
					openGLESState.setTextureEnvSrcAlpha(pname - GLES11.GL_SRC0_ALPHA, param - GLES10.GL_TEXTURE0);
				}
				else if (param == GLES10.GL_TEXTURE)
				{
					openGLESState.setTextureEnvSrcAlpha(pname - GLES11.GL_SRC0_ALPHA, openGLESState.getActiveTexture());
				}
				else
				{
					openGLESState.setTextureEnvSrcAlpha(pname - GLES11.GL_SRC0_ALPHA, param);
				}
				break;
			case GLES11.GL_OPERAND0_RGB:
			case GLES11.GL_OPERAND1_RGB:
			case GLES11.GL_OPERAND2_RGB:
				openGLESState.setTextureEnvOperandRGB(pname - GLES11.GL_OPERAND0_RGB, param);
				break;
			case GLES11.GL_OPERAND0_ALPHA:
			case GLES11.GL_OPERAND1_ALPHA:
			case GLES11.GL_OPERAND2_ALPHA:
				openGLESState.setTextureEnvOperandAlpha(pname - GLES11.GL_OPERAND0_ALPHA, param);
				break;
			case GLES11.GL_RGB_SCALE:
				openGLESState.setTextureEnvRGBScale(param);
				break;
			case GLES11.GL_ALPHA_SCALE:
				openGLESState.setTextureEnvAlphaScale(param);
				break;
			default:
				Log.d(TAG, "ERROR: Unknown parameter " + pname);
				break;
		}
	}
	 
	public void glTexEnviv(int target, int pname, int[] params, int offset)
	{
	    Log.d(TAG, "ERROR: Not implemented.");
	    //GLES20.glTexEnvfv(target, pname, (float)params);
	}
	 
	public void glTexParameteri(int target, int pname, int param)
	{
	    GLES20.glTexParameteri(target, pname, param);
	}
	 
	public void glTexParameteriv(int target, int pname, int[] params, int offset)
	{
	    GLES20.glTexParameteriv(target, pname, params, offset);
	}


	// OpenGLE S 1.1 extensions
	 
	public void glCurrentPaletteMatrixOES(int matrixpaletteindex)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glLoadPaletteFromModelViewMatrixOES()
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glMatrixIndexPointerOES(int size, int type, int stride, Object pointer)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glWeightPointerOES(int size, int type, int stride, Object pointer)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glPointSizePointerOES(int type, int stride, Object pointer)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexsOES(short x, short y, short z, short width, short height)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexiOES(int x, int y, int z, int width, int height)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexxOES(int x, int y, int z, int width, int height)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexsvOES(short coords)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexivOES(int coords)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexxvOES(int coords)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexfOES(float x, float y, float z, float width, float height)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}
	 
	public void glDrawTexfvOES(float coords)
	{
		Log.d(TAG, "WARNING: No effect in OpenGL ES 2.x");
	}


	// Non-API
	public int getCachedShaderAmount()
	{
		return openGLESState.getCachedShaderAmount();
	}


	private void prepareToDraw()
	{
		Matrix4x4f modelViewMatrix = matrixStack.getModelViewMatrix();
		openGLESState.setModelViewMatrix(modelViewMatrix);
		Matrix4x4f projectionMatrix = matrixStack.getProjectionMatrix();

		Matrix4x4f mvp = new Matrix4x4f();
		mvp = OpenGLESMath.multiply(modelViewMatrix, projectionMatrix);
		openGLESState.setModelViewProjectionMatrix(mvp);

		if (openGLESState.isNormal())
		{
			// If only uniform scaling used (TODO: detect somehow)
			Matrix3x3f modelViewMatrix3x3 = new Matrix3x3f();
			modelViewMatrix3x3 = OpenGLESMath.copyMatrix4x4UpperLeftToMatrix3x3(modelViewMatrix);
			modelViewMatrix3x3 = OpenGLESMath.adjoint(modelViewMatrix3x3);
			modelViewMatrix3x3 = OpenGLESMath.transpose(modelViewMatrix3x3);
			openGLESState.setTransposeAdjointModelViewMatrix(modelViewMatrix3x3);

			if (openGLESState.isRescaleNormal())
			{
				openGLESState.setRescaleNormalFactor((float) (1.0f / Math.sqrt(modelViewMatrix3x3.m[0] * modelViewMatrix3x3.m[0] + modelViewMatrix3x3.m[3] * modelViewMatrix3x3.m[3] + modelViewMatrix3x3.m[6] * modelViewMatrix3x3.m[6])));
			}

			// else do it slow but works always
			/*Matrix4x4<GLfloat> transposeInverseModelViewMatrix;
			 OpenGLESMath::inverse(&transposeInverseModelViewMatrix, modelViewMatrix);
			 OpenGLESMath::transpose(&transposeInverseModelViewMatrix);
			 Matrix3x3<GLfloat> modelViewMatrix3x3;
			 OpenGLESMath::copyMatrix4x4UpperLeftToMatrix3x3(&modelViewMatrix3x3, &transposeInverseModelViewMatrix);
			 openGLESState.setTransposeAdjointModelViewMatrix(modelViewMatrix3x3);
			
			 if (openGLESState.isRescaleNormal()) {
			 openGLESState.setRescaleNormalFactor(1.0f/sqrtf(transposeInverseModelViewMatrix.m[0]*transposeInverseModelViewMatrix.m[0] + transposeInverseModelViewMatrix.m[4]*transposeInverseModelViewMatrix.m[4] + transposeInverseModelViewMatrix.m[8]*transposeInverseModelViewMatrix.m[8]));
			 }*/
		}

		for (int i = 0; i < this.maxTextureImageUnits; i++)
		{
			if (openGLESState.isTexCoord(i))
			{
				Matrix4x4f textureMatrix = matrixStack.getTextureMatrix(i);
				openGLESState.setTextureMatrix(i, textureMatrix);
			}
		}


		openGLESState.setCurrentProgram();
	}
}
