package android.opengl.OpenGLES10;

import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLES20;
import android.util.Log;

import java.util.Arrays;


public class OpenGLESState
{
    private static final String TAG = "OpenGLESState";

    private UniformBase[] uniforms = new UniformBase[UniformId.COUNT];
    private Attribute[] attributes = new Attribute[AttributeId.COUNT];
    private ShaderFile[] shaders = new ShaderFile[ShaderId.COUNT];
    private java.util.ArrayList<StateShaderProgram > stateShaderPrograms = new java.util.ArrayList<StateShaderProgram >();
    private StateShaderProgram currentStateShaderProgram;
    private int stateSize;
    private int stateSizeBool;
    private int[] currentState = new int[1 + (UniformId.STATE_UNIFORM_BOOL_COUNT / 32) + UniformId.STATE_UNIFORM_INT_COUNT];
    private int activeTexture;
    private int clientActiveTexture;
    private java.util.HashMap<Integer, Integer> boundTextureFormats = new java.util.HashMap<Integer, Integer>();
    private int[] boundTextures = null;
    
    public class ShaderId {
        public static final int MAIN_VERTEX_SHADER = 0;
        public static final int LIGHTING_VERTEX_SHADER = 1;
        public static final int LIGHTING_PER_VERTEX_VERTEX_SHADER = 2;
        public static final int LIGHTING_PER_FRAGMENT_VERTEX_SHADER = 3;
        public static final int CLIP_PLANE_VERTEX_SHADER = 4;
        public static final int TEXTURE_VERTEX_SHADER = 5;
        public static final int TEXTURE0_VERTEX_SHADER = 6;
        public static final int TEXTURE1_VERTEX_SHADER = 7;
        public static final int TEXTURE2_VERTEX_SHADER = 8;
        public static final int FOG_VERTEX_SHADER = 9;
        
        public static final int  MAIN_FRAGMENT_SHADER = 10;
        public static final int LIGHTING_FRAGMENT_SHADER = 11;
        public static final int LIGHTING_PER_FRAGMENT_FRAGMENT_SHADER = 12;
        public static final int ALPHA_TEST_FRAGMENT_SHADER = 13;
        public static final int CLIP_PLANE_FRAGMENT_SHADER = 14;
        public static final int TEXTURE_FRAGMENT_SHADER = 15;
        public static final int TEXTURE0_FRAGMENT_SHADER = 16;
        public static final int TEXTURE1_FRAGMENT_SHADER = 17;
        public static final int TEXTURE2_FRAGMENT_SHADER = 18;
        public static final int FOG_FRAGMENT_SHADER = 19;
        
        public static final int FIRST_VERTEX_SHADER = MAIN_VERTEX_SHADER;
        public static final int LAST_VERTEX_SHADER = FOG_VERTEX_SHADER;
        public static final int FIRST_FRAGMENT_SHADER = MAIN_FRAGMENT_SHADER;
        public static final int LAST_FRAGMENT_SHADER = FOG_FRAGMENT_SHADER;
        public static final int COUNT = LAST_FRAGMENT_SHADER + 1;
    };
    
    public class AttributeId {
        public static final int POSITION = 0;
        public static final int NORMAL = 1;
        public static final int COLOR = 2;
        public static final int TEXCOORD0 = 3;
        public static final int TEXCOORD1 = 4;
        public static final int TEXCOORD2 = 5;
        public static final int COUNT = TEXCOORD2 + 1;
    };
    
    public class UniformId {
        public static final int POSITION_ENABLED = 0;
        public static final int NORMAL_ENABLED = 1;
        public static final int COLOR_ENABLED = 2;
        public static final int TEXCOORD0_ENABLED = 3;
        public static final int TEXCOORD1_ENABLED = 4;
        public static final int TEXCOORD2_ENABLED = 5;
        public static final int LIGHTING_ENABLED = 6;
        public static final int LIGHT_MODEL_LOCAL_VIEWER_ENABLED = 7;
        public static final int LIGHT_MODEL_TWO_SIDE_ENABLED = 8;
        public static final int LIGHT0_ENABLED = 9;
        public static final int LIGHT1_ENABLED = 10;
        public static final int LIGHT2_ENABLED = 11;
        public static final int TEXTURE0_ENABLED = 12;
        public static final int TEXTURE1_ENABLED = 13;
        public static final int TEXTURE2_ENABLED = 14;
        public static final int TEXTURE0_MATRIX_ENABLED = 15;
        public static final int TEXTURE1_MATRIX_ENABLED = 16;
        public static final int TEXTURE2_MATRIX_ENABLED = 17;
        public static final int FOG_ENABLED = 18;
        public static final int ALPHA_TEST_ENABLED = 19;
        public static final int CLIP_PLANE0_ENABLED = 20;
        public static final int CLIP_PLANE1_ENABLED = 21;
        public static final int CLIP_PLANE2_ENABLED = 22;
        public static final int CLIP_PLANE3_ENABLED = 23;
        public static final int CLIP_PLANE4_ENABLED = 24;
        public static final int CLIP_PLANE5_ENABLED = 25;
        public static final int RESCALE_NORMAL_ENABLED = 26;
        public static final int NORMALIZE_ENABLED = 27;
        
        public static final int FOG_MODE = 28;
        public static final int FOG_HINT = 29;
        public static final int ALPHA_FUNC = 30;
        public static final int TEXTURE0_FORMAT = 31;
        public static final int TEXTURE1_FORMAT = 32;
        public static final int TEXTURE2_FORMAT = 33;
        public static final int TEXTURE0_ENV_MODE = 34;
        public static final int TEXTURE1_ENV_MODE = 35;
        public static final int TEXTURE2_ENV_MODE = 36;
        public static final int TEXTURE0_ENV_COMBINE_RGB = 37;
        public static final int TEXTURE1_ENV_COMBINE_RGB = 38;
        public static final int TEXTURE2_ENV_COMBINE_RGB = 39;
        public static final int TEXTURE0_ENV_COMBINE_ALPHA = 40;
        public static final int TEXTURE1_ENV_COMBINE_ALPHA = 41;
        public static final int TEXTURE2_ENV_COMBINE_ALPHA = 42;
        public static final int TEXTURE0_ENV_SRC0_RGB = 43;
        public static final int TEXTURE0_ENV_SRC1_RGB = 44;
        public static final int TEXTURE0_ENV_SRC2_RGB = 45;
        public static final int TEXTURE1_ENV_SRC0_RGB = 46;
        public static final int TEXTURE1_ENV_SRC1_RGB = 47;
        public static final int TEXTURE1_ENV_SRC2_RGB = 48;
        public static final int TEXTURE2_ENV_SRC0_RGB = 49;
        public static final int TEXTURE2_ENV_SRC1_RGB = 50;
        public static final int TEXTURE2_ENV_SRC2_RGB = 51;
        public static final int TEXTURE0_ENV_OPERAND0_RGB = 52;
        public static final int TEXTURE0_ENV_OPERAND1_RGB = 53;
        public static final int TEXTURE0_ENV_OPERAND2_RGB = 54;
        public static final int TEXTURE1_ENV_OPERAND0_RGB = 55;
        public static final int TEXTURE1_ENV_OPERAND1_RGB = 56;
        public static final int TEXTURE1_ENV_OPERAND2_RGB = 57;
        public static final int TEXTURE2_ENV_OPERAND0_RGB = 58;
        public static final int TEXTURE2_ENV_OPERAND1_RGB = 59;
        public static final int TEXTURE2_ENV_OPERAND2_RGB = 60;
        public static final int TEXTURE0_ENV_SRC0_ALPHA = 61;
        public static final int TEXTURE0_ENV_SRC1_ALPHA = 62;
        public static final int TEXTURE0_ENV_SRC2_ALPHA = 63;
        public static final int TEXTURE1_ENV_SRC0_ALPHA = 64;
        public static final int TEXTURE1_ENV_SRC1_ALPHA = 65;
        public static final int TEXTURE1_ENV_SRC2_ALPHA = 66;
        public static final int TEXTURE2_ENV_SRC0_ALPHA = 67;
        public static final int TEXTURE2_ENV_SRC1_ALPHA = 68;
        public static final int TEXTURE2_ENV_SRC2_ALPHA = 69;
        public static final int TEXTURE0_ENV_OPERAND0_ALPHA = 70;
        public static final int TEXTURE0_ENV_OPERAND1_ALPHA = 71;
        public static final int TEXTURE0_ENV_OPERAND2_ALPHA = 72;
        public static final int TEXTURE1_ENV_OPERAND0_ALPHA = 73;
        public static final int TEXTURE1_ENV_OPERAND1_ALPHA = 74;
        public static final int TEXTURE1_ENV_OPERAND2_ALPHA = 75;
        public static final int TEXTURE2_ENV_OPERAND0_ALPHA = 76;
        public static final int TEXTURE2_ENV_OPERAND1_ALPHA = 77;
        public static final int TEXTURE2_ENV_OPERAND2_ALPHA = 78;
        public static final int LIGHTING_HINT = 79;
        
        public static final int MODELVIEW_PROJECTION_MATRIX = 80;
        public static final int MODELVIEW_MATRIX = 81;
        public static final int TRANPOSE_ADJOINT_MODEL_VIEW_MATRIX = 82;
        public static final int TEXTURE0_MATRIX = 83;
        public static final int TEXTURE1_MATRIX = 84;
        public static final int TEXTURE2_MATRIX = 85;
        public static final int TEXTURE0_SAMPLER = 86;
        public static final int TEXTURE1_SAMPLER = 87;
        public static final int TEXTURE2_SAMPLER = 88;
        public static final int TEXTURE0_ENV_COLOR = 89;
        public static final int TEXTURE1_ENV_COLOR = 90;
        public static final int TEXTURE2_ENV_COLOR = 91;
        public static final int TEXTURE0_ENV_RGB_SCALE = 92;
        public static final int TEXTURE1_ENV_RGB_SCALE = 93;
        public static final int TEXTURE2_ENV_RGB_SCALE = 94;
        public static final int TEXTURE0_ENV_ALPHA_SCALE = 95;
        public static final int TEXTURE1_ENV_ALPHA_SCALE = 96;
        public static final int TEXTURE2_ENV_ALPHA_SCALE = 97;
        public static final int TEXTURE0_ENV_BLUR_AMOUNT = 98;
        public static final int TEXTURE1_ENV_BLUR_AMOUNT = 99;
        public static final int TEXTURE2_ENV_BLUR_AMOUNT = 100;
        public static final int RESCALE_NORMAL_FACTOR = 101;
        public static final int LIGHT0_AMBIENT = 102;
        public static final int LIGHT1_AMBIENT = 103;
        public static final int LIGHT2_AMBIENT = 104;
        public static final int LIGHT0_DIFFUSE = 105;
        public static final int LIGHT1_DIFFUSE = 106;
        public static final int LIGHT2_DIFFUSE = 107;
        public static final int LIGHT0_SPECULAR = 108;
        public static final int LIGHT1_SPECULAR = 109;
        public static final int LIGHT2_SPECULAR = 110;
        public static final int LIGHT0_POSITION = 111;
        public static final int LIGHT1_POSITION = 112;
        public static final int LIGHT2_POSITION = 113;
        public static final int LIGHT0_SPOT_DIRECTION = 114;
        public static final int LIGHT1_SPOT_DIRECTION = 115;
        public static final int LIGHT2_SPOT_DIRECTION = 116;
        public static final int LIGHT0_SPOT_EXPONENT = 117;
        public static final int LIGHT1_SPOT_EXPONENT = 118;
        public static final int LIGHT2_SPOT_EXPONENT = 119;
        public static final int LIGHT0_SPOT_CUTOFF_ANGLE_COS = 120;
        public static final int LIGHT1_SPOT_CUTOFF_ANGLE_COS = 121;
        public static final int LIGHT2_SPOT_CUTOFF_ANGLE_COS = 122;
        public static final int LIGHT0_CONSTANT_ATTENUATION = 123;
        public static final int LIGHT1_CONSTANT_ATTENUATION = 124;
        public static final int LIGHT2_CONSTANT_ATTENUATION = 125;
        public static final int LIGHT0_LINEAR_ATTENUATION = 126;
        public static final int LIGHT1_LINEAR_ATTENUATION = 127;
        public static final int LIGHT2_LINEAR_ATTENUATION = 128;
        public static final int LIGHT0_QUADRATIC_ATTENUATION = 129;
        public static final int LIGHT1_QUADRATIC_ATTENUATION = 130;
        public static final int LIGHT2_QUADRATIC_ATTENUATION = 131;
        public static final int MATERIAL_AMBIENT = 132;
        public static final int MATERIAL_DIFFUSE = 133;
        public static final int MATERIAL_SPECULAR = 134;
        public static final int MATERIAL_EMISSION = 135;
        public static final int MATERIAL_SHININESS = 136;
        public static final int FOG_COLOR = 137;
        public static final int FOG_DENSITY = 138;
        public static final int FOG_START = 139;
        public static final int FOG_END = 140;
        public static final int ALPHA_FUNC_VALUE = 141;
        public static final int CLIP_PLANE0_EQUATION = 142;
        public static final int CLIP_PLANE1_EQUATION = 143;
        public static final int CLIP_PLANE2_EQUATION = 144;
        public static final int CLIP_PLANE3_EQUATION = 145;
        public static final int CLIP_PLANE4_EQUATION = 146;
        public static final int CLIP_PLANE5_EQUATION = 147;
        public static final int GLOBAL_AMBIENT_COLOR = 148;
        
        public static final int FIRST_STATE_UNIFORM_BOOL = POSITION_ENABLED;
        public static final int LAST_STATE_UNIFORM_BOOL = NORMALIZE_ENABLED;
        public static final int FIRST_STATE_UNIFORM_INT = FOG_MODE;
        public static final int LAST_STATE_UNIFORM_INT = LIGHTING_HINT;
        public static final int FIRST_NORMAL_UNIFORM = MODELVIEW_PROJECTION_MATRIX;
        public static final int LAST_NORMAL_UNIFORM = GLOBAL_AMBIENT_COLOR;
        public static final int COUNT = LAST_NORMAL_UNIFORM + 1;
        public static final int STATE_UNIFORM_BOOL_COUNT = LAST_STATE_UNIFORM_BOOL - FIRST_STATE_UNIFORM_BOOL + 1;
        public static final int STATE_UNIFORM_INT_COUNT = LAST_STATE_UNIFORM_INT - FIRST_STATE_UNIFORM_INT + 1;
    };
    

	public OpenGLESState()
	{
		this.stateShaderPrograms = new java.util.ArrayList<StateShaderProgram >();
		this.currentStateShaderProgram = null;
		this.stateSize = 1 + (UniformId.STATE_UNIFORM_BOOL_COUNT / 32) + UniformId.STATE_UNIFORM_INT_COUNT;
		this.stateSizeBool = 1 + (UniformId.STATE_UNIFORM_BOOL_COUNT / 32);
		this.activeTexture = 0;
		this.clientActiveTexture = 0;
	}

	public void init(OpenGLES10Context context)
	{
	    Arrays.fill(currentState, 0);

		boundTextures = new int[context.maxTextureImageUnits];
		Arrays.fill(boundTextures, 0);


		// TODO: Read all the following from text file

		shaders[ShaderId.MAIN_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "main.vert");
		shaders[ShaderId.LIGHTING_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "lighting.vert");
		shaders[ShaderId.LIGHTING_PER_VERTEX_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "lightingPerVertex.vert");
		shaders[ShaderId.LIGHTING_PER_FRAGMENT_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "lightingPerFragment.vert");
		shaders[ShaderId.FOG_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "fog.glsl");
		shaders[ShaderId.CLIP_PLANE_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "clipPlane.vert");
		shaders[ShaderId.TEXTURE_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "texture.vert");
		shaders[ShaderId.TEXTURE0_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "texture0.vert");
		shaders[ShaderId.TEXTURE1_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "texture1.vert");
		shaders[ShaderId.TEXTURE2_VERTEX_SHADER] = new ShaderFile(GLES20.GL_VERTEX_SHADER, "texture2.vert");

		shaders[ShaderId.MAIN_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "main.frag");
		shaders[ShaderId.LIGHTING_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "lighting.frag");
		shaders[ShaderId.LIGHTING_PER_FRAGMENT_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "lightingPerFragment.frag");
		shaders[ShaderId.FOG_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "fog.glsl");
		shaders[ShaderId.ALPHA_TEST_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "alphaTest.frag");
		shaders[ShaderId.CLIP_PLANE_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "clipPlane.frag");
		shaders[ShaderId.TEXTURE_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "texture.frag");
		shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "texture0.frag");
		shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "texture1.frag");
		shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER] = new ShaderFile(GLES20.GL_FRAGMENT_SHADER, "texture2.frag");

		attributes[AttributeId.POSITION] = new Attribute();
		attributes[AttributeId.NORMAL] = new Attribute();
		attributes[AttributeId.COLOR] = new Attribute();
		attributes[AttributeId.TEXCOORD0] = new Attribute();
		attributes[AttributeId.TEXCOORD1] = new Attribute();
		attributes[AttributeId.TEXCOORD2] = new Attribute();


		// Bool uniforms with defines

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.POSITION_ENABLED && UniformId.POSITION_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.POSITION_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "POSITION_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.POSITION_ENABLED] = new Uniform<Boolean>(false);
		}

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.COLOR_ENABLED && UniformId.COLOR_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.COLOR_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "COLOR_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.COLOR_ENABLED] = new Uniform<Boolean>(false);
		}

		UniformState<Boolean> texcoord0Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXCOORD0_ENABLED && UniformId.TEXCOORD0_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			texcoord0Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "TEXCOORD0_ENABLED", false);
			texcoord0Enabled.addDefineShaderFile(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER]);
			uniforms[UniformId.TEXCOORD0_ENABLED] = texcoord0Enabled;
		}
		else
		{
			uniforms[UniformId.TEXCOORD0_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.TEXCOORD0_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE_VERTEX_SHADER]);
		uniforms[UniformId.TEXCOORD0_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE0_VERTEX_SHADER]);

		UniformState<Boolean> texcoord1Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXCOORD1_ENABLED && UniformId.TEXCOORD1_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			texcoord1Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "TEXCOORD1_ENABLED", false);
			texcoord1Enabled.addDefineShaderFile(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER]);
			uniforms[UniformId.TEXCOORD1_ENABLED] = texcoord1Enabled;
		}
		else
		{
			uniforms[UniformId.TEXCOORD1_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.TEXCOORD1_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE_VERTEX_SHADER]);
		uniforms[UniformId.TEXCOORD1_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE1_VERTEX_SHADER]);

		UniformState<Boolean> texcoord2Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXCOORD2_ENABLED && UniformId.TEXCOORD2_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			texcoord2Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "TEXCOORD2_ENABLED", false);
			texcoord2Enabled.addDefineShaderFile(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER]);
			uniforms[UniformId.TEXCOORD2_ENABLED] = texcoord2Enabled;
		}
		else
		{
			uniforms[UniformId.TEXCOORD2_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.TEXCOORD2_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE_VERTEX_SHADER]);
		uniforms[UniformId.TEXCOORD2_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE2_VERTEX_SHADER]);

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXTURE0_MATRIX_ENABLED && UniformId.TEXTURE0_MATRIX_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.TEXTURE0_MATRIX_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.TEXTURE_VERTEX_SHADER], "TEXTURE0_MATRIX_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_MATRIX_ENABLED] = new Uniform<Boolean>(false);
		}

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXTURE1_MATRIX_ENABLED && UniformId.TEXTURE1_MATRIX_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.TEXTURE1_MATRIX_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.TEXTURE_VERTEX_SHADER], "TEXTURE1_MATRIX_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_MATRIX_ENABLED] = new Uniform<Boolean>(false);
		}

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXTURE2_MATRIX_ENABLED && UniformId.TEXTURE2_MATRIX_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.TEXTURE2_MATRIX_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.TEXTURE_VERTEX_SHADER], "TEXTURE2_MATRIX_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_MATRIX_ENABLED] = new Uniform<Boolean>(false);
		}

		UniformState<Boolean> lightingEnabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.LIGHTING_ENABLED && UniformId.LIGHTING_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			lightingEnabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "LIGHTING_ENABLED", false);
			lightingEnabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.LIGHTING_ENABLED] = lightingEnabled;
		}
		else
		{
			uniforms[UniformId.LIGHTING_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.LIGHTING_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.LIGHTING_VERTEX_SHADER]);
		uniforms[UniformId.LIGHTING_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.LIGHTING_FRAGMENT_SHADER]);

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.LIGHT_MODEL_LOCAL_VIEWER_ENABLED && UniformId.LIGHT_MODEL_LOCAL_VIEWER_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.LIGHT_MODEL_LOCAL_VIEWER_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "LIGHT_MODEL_LOCAL_VIEWER_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.LIGHT_MODEL_LOCAL_VIEWER_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.LIGHT_MODEL_LOCAL_VIEWER_ENABLED].setFather(lightingEnabled);

		UniformState<Boolean> lightModelTwoSideEnabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.LIGHT_MODEL_TWO_SIDE_ENABLED && UniformId.LIGHT_MODEL_TWO_SIDE_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			lightModelTwoSideEnabled = new UniformState<Boolean>(shaders[ShaderId.LIGHTING_VERTEX_SHADER], "LIGHT_MODEL_TWO_SIDE_ENABLED", false);
			lightModelTwoSideEnabled.addDefineShaderFile(shaders[ShaderId.LIGHTING_FRAGMENT_SHADER]);
			uniforms[UniformId.LIGHT_MODEL_TWO_SIDE_ENABLED] = lightModelTwoSideEnabled;
		}
		else
		{
			uniforms[UniformId.LIGHT_MODEL_TWO_SIDE_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.LIGHT_MODEL_TWO_SIDE_ENABLED].setFather(lightingEnabled);

		UniformState<Boolean> light0Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.LIGHT0_ENABLED && UniformId.LIGHT0_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			light0Enabled = new UniformState<Boolean>(shaders[ShaderId.LIGHTING_VERTEX_SHADER], "LIGHT0_ENABLED", false);
			light0Enabled.addDefineShaderFile(shaders[ShaderId.LIGHTING_PER_FRAGMENT_FRAGMENT_SHADER]);
			uniforms[UniformId.LIGHT0_ENABLED] = light0Enabled;
		}
		else
		{
			uniforms[UniformId.LIGHT0_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.LIGHT0_ENABLED].setFather(lightingEnabled);

		UniformState<Boolean> light1Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.LIGHT1_ENABLED && UniformId.LIGHT1_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			light1Enabled = new UniformState<Boolean>(shaders[ShaderId.LIGHTING_VERTEX_SHADER], "LIGHT1_ENABLED", false);
			light1Enabled.addDefineShaderFile(shaders[ShaderId.LIGHTING_PER_FRAGMENT_FRAGMENT_SHADER]);
			uniforms[UniformId.LIGHT1_ENABLED] = light1Enabled;
		}
		else
		{
			uniforms[UniformId.LIGHT1_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.LIGHT1_ENABLED].setFather(lightingEnabled);

		UniformState<Boolean> light2Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.LIGHT2_ENABLED && UniformId.LIGHT2_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			light2Enabled = new UniformState<Boolean>(shaders[ShaderId.LIGHTING_VERTEX_SHADER], "LIGHT2_ENABLED", false);
			light2Enabled.addDefineShaderFile(shaders[ShaderId.LIGHTING_PER_FRAGMENT_FRAGMENT_SHADER]);
			uniforms[UniformId.LIGHT2_ENABLED] = light2Enabled;
		}
		else
		{
			uniforms[UniformId.LIGHT2_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.LIGHT2_ENABLED].setFather(lightingEnabled);

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.NORMAL_ENABLED && UniformId.NORMAL_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.NORMAL_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "NORMAL_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.NORMAL_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.NORMAL_ENABLED].setFather(lightingEnabled);

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.RESCALE_NORMAL_ENABLED && UniformId.RESCALE_NORMAL_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.RESCALE_NORMAL_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "RESCALE_NORMAL_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.RESCALE_NORMAL_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.RESCALE_NORMAL_ENABLED].setFather(lightingEnabled);

		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.NORMALIZE_ENABLED && UniformId.NORMALIZE_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			uniforms[UniformId.NORMALIZE_ENABLED] = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "NORMALIZE_ENABLED", false);
		}
		else
		{
			uniforms[UniformId.NORMALIZE_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.NORMALIZE_ENABLED].setFather(lightingEnabled);

		UniformState<Boolean> fogEnabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.FOG_ENABLED && UniformId.FOG_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			fogEnabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "FOG_ENABLED", false);
			fogEnabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.FOG_ENABLED] = fogEnabled;
		}
		else
		{
			uniforms[UniformId.FOG_ENABLED] = new Uniform<Boolean>(false);
		}

		UniformState<Boolean> texture0Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXTURE0_ENABLED && UniformId.TEXTURE0_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			texture0Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "TEXTURE0_ENABLED", false);
			texture0Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.TEXTURE0_ENABLED] = texture0Enabled;
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.TEXTURE0_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE_FRAGMENT_SHADER]);
		uniforms[UniformId.TEXTURE0_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER]);

		UniformState<Boolean> texture1Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXTURE1_ENABLED && UniformId.TEXTURE1_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			texture1Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "TEXTURE1_ENABLED", false);
			texture1Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.TEXTURE1_ENABLED] = texture1Enabled;
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.TEXTURE1_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE_FRAGMENT_SHADER]);
		uniforms[UniformId.TEXTURE1_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER]);

		UniformState<Boolean> texture2Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.TEXTURE2_ENABLED && UniformId.TEXTURE2_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			texture2Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "TEXTURE2_ENABLED", false);
			texture2Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.TEXTURE2_ENABLED] = texture2Enabled;
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.TEXTURE2_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE_FRAGMENT_SHADER]);
		uniforms[UniformId.TEXTURE2_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER]);

		UniformState<Boolean> alphaTestEnabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.ALPHA_TEST_ENABLED && UniformId.ALPHA_TEST_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			alphaTestEnabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_FRAGMENT_SHADER], "ALPHA_TEST_ENABLED", false);
			uniforms[UniformId.ALPHA_TEST_ENABLED] = alphaTestEnabled;
		}
		else
		{
			uniforms[UniformId.ALPHA_TEST_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.ALPHA_TEST_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.ALPHA_TEST_FRAGMENT_SHADER]);

		UniformState<Boolean> clipPlane0Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.CLIP_PLANE0_ENABLED && UniformId.CLIP_PLANE0_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			clipPlane0Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "CLIP_PLANE0_ENABLED", false);
			clipPlane0Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.CLIP_PLANE0_ENABLED] = clipPlane0Enabled;
		}
		else
		{
			uniforms[UniformId.CLIP_PLANE0_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.CLIP_PLANE0_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_VERTEX_SHADER]);
		uniforms[UniformId.CLIP_PLANE0_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_FRAGMENT_SHADER]);

		UniformState<Boolean> clipPlane1Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.CLIP_PLANE1_ENABLED && UniformId.CLIP_PLANE1_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			clipPlane1Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "CLIP_PLANE1_ENABLED", false);
			clipPlane1Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.CLIP_PLANE1_ENABLED] = clipPlane1Enabled;
		}
		else
		{
			uniforms[UniformId.CLIP_PLANE1_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.CLIP_PLANE1_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_VERTEX_SHADER]);
		uniforms[UniformId.CLIP_PLANE1_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_FRAGMENT_SHADER]);

		UniformState<Boolean> clipPlane2Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.CLIP_PLANE2_ENABLED && UniformId.CLIP_PLANE2_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			clipPlane2Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "CLIP_PLANE2_ENABLED", false);
			clipPlane2Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.CLIP_PLANE2_ENABLED] = clipPlane2Enabled;
		}
		else
		{
			uniforms[UniformId.CLIP_PLANE2_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.CLIP_PLANE2_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_VERTEX_SHADER]);
		uniforms[UniformId.CLIP_PLANE2_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_FRAGMENT_SHADER]);

		UniformState<Boolean> clipPlane3Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.CLIP_PLANE3_ENABLED && UniformId.CLIP_PLANE3_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			clipPlane3Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "CLIP_PLANE3_ENABLED", false);
			clipPlane3Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.CLIP_PLANE3_ENABLED] = clipPlane3Enabled;
		}
		else
		{
			uniforms[UniformId.CLIP_PLANE3_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.CLIP_PLANE3_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_VERTEX_SHADER]);
		uniforms[UniformId.CLIP_PLANE3_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_FRAGMENT_SHADER]);

		UniformState<Boolean> clipPlane4Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.CLIP_PLANE4_ENABLED && UniformId.CLIP_PLANE4_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			clipPlane4Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "CLIP_PLANE4_ENABLED", false);
			clipPlane4Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.CLIP_PLANE4_ENABLED] = clipPlane4Enabled;
		}
		else
		{
			uniforms[UniformId.CLIP_PLANE4_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.CLIP_PLANE4_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_VERTEX_SHADER]);
		uniforms[UniformId.CLIP_PLANE4_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_FRAGMENT_SHADER]);

		UniformState<Boolean> clipPlane5Enabled = null;
		if (UniformId.FIRST_STATE_UNIFORM_BOOL <= UniformId.CLIP_PLANE5_ENABLED && UniformId.CLIP_PLANE5_ENABLED <= UniformId.LAST_STATE_UNIFORM_BOOL)
		{
			clipPlane5Enabled = new UniformState<Boolean>(shaders[ShaderId.MAIN_VERTEX_SHADER], "CLIP_PLANE5_ENABLED", false);
			clipPlane5Enabled.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.CLIP_PLANE5_ENABLED] = clipPlane5Enabled;
		}
		else
		{
			uniforms[UniformId.CLIP_PLANE5_ENABLED] = new Uniform<Boolean>(false);
		}
		uniforms[UniformId.CLIP_PLANE5_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_VERTEX_SHADER]);
		uniforms[UniformId.CLIP_PLANE5_ENABLED].addAdditionalRequiredShaderFile(1, shaders[ShaderId.CLIP_PLANE_FRAGMENT_SHADER]);



		// Int uniforms with defines

		UniformState<Integer> fogMode = null;
		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.FOG_MODE && UniformId.FOG_MODE <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			fogMode = new UniformState<Integer>(shaders[ShaderId.MAIN_VERTEX_SHADER], "FOG_MODE", GLES10.GL_EXP);
			fogMode.addDefineShaderFile(shaders[ShaderId.FOG_FRAGMENT_SHADER]);
			uniforms[UniformId.FOG_MODE] = fogMode;
		}
		else
		{
			uniforms[UniformId.FOG_MODE] = new Uniform<Integer>(GLES10.GL_EXP);
		}
		uniforms[UniformId.FOG_MODE].setFather(fogEnabled);

		UniformState<Integer> fogHint = null;
		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.FOG_HINT && UniformId.FOG_HINT <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			fogHint = new UniformState<Integer>(shaders[ShaderId.MAIN_VERTEX_SHADER], "FOG_HINT", GLES10.GL_FASTEST);
			fogHint.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.FOG_HINT] = fogHint;
		}
		else
		{
			uniforms[UniformId.FOG_HINT] = new Uniform<Integer>(GLES10.GL_FASTEST);
		}
		uniforms[UniformId.FOG_HINT].addAdditionalRequiredShaderFile(GLES10.GL_FASTEST, shaders[ShaderId.FOG_VERTEX_SHADER]);
		uniforms[UniformId.FOG_HINT].addAdditionalRequiredShaderFile(GLES10.GL_NICEST, shaders[ShaderId.FOG_FRAGMENT_SHADER]);
		uniforms[UniformId.FOG_HINT].setFather(fogEnabled);

		UniformState<Integer> lightingHint = null;
		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.LIGHTING_HINT && UniformId.LIGHTING_HINT <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			lightingHint = new UniformState<Integer>(shaders[ShaderId.MAIN_VERTEX_SHADER], "LIGHTING_HINT", GLES10.GL_FASTEST);
			lightingHint.addDefineShaderFile(shaders[ShaderId.MAIN_FRAGMENT_SHADER]);
			uniforms[UniformId.LIGHTING_HINT] = lightingHint;
		}
		else
		{
			uniforms[UniformId.LIGHTING_HINT] = new Uniform<Integer>(GLES10.GL_FASTEST);
		}
		uniforms[UniformId.LIGHTING_HINT].addAdditionalRequiredShaderFile(GLES10.GL_FASTEST, shaders[ShaderId.LIGHTING_VERTEX_SHADER]);
		uniforms[UniformId.LIGHTING_HINT].addAdditionalRequiredShaderFile(GLES10.GL_FASTEST, shaders[ShaderId.LIGHTING_PER_VERTEX_VERTEX_SHADER]);
		uniforms[UniformId.LIGHTING_HINT].addAdditionalRequiredShaderFile(GLES10.GL_NICEST, shaders[ShaderId.LIGHTING_FRAGMENT_SHADER]);
		uniforms[UniformId.LIGHTING_HINT].addAdditionalRequiredShaderFile(GLES10.GL_NICEST, shaders[ShaderId.LIGHTING_PER_FRAGMENT_VERTEX_SHADER]);
		uniforms[UniformId.LIGHTING_HINT].addAdditionalRequiredShaderFile(GLES10.GL_NICEST, shaders[ShaderId.LIGHTING_PER_FRAGMENT_FRAGMENT_SHADER]);
		uniforms[UniformId.LIGHTING_HINT].setFather(lightingEnabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.ALPHA_FUNC && UniformId.ALPHA_FUNC <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.ALPHA_FUNC] = new UniformState<Integer>(shaders[ShaderId.ALPHA_TEST_FRAGMENT_SHADER], "ALPHA_FUNC", GLES10.GL_ALWAYS);
		}
		else
		{
			uniforms[UniformId.ALPHA_FUNC] = new Uniform<Integer>(GLES10.GL_ALWAYS);
		}
		uniforms[UniformId.ALPHA_FUNC].setFather(alphaTestEnabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_FORMAT && UniformId.TEXTURE0_FORMAT <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_FORMAT] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_FORMAT", GLES10.GL_RGBA);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_FORMAT] = new Uniform<Integer>(GLES10.GL_RGBA);
		}
		uniforms[UniformId.TEXTURE0_FORMAT].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_FORMAT && UniformId.TEXTURE1_FORMAT <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_FORMAT] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_FORMAT", GLES10.GL_RGBA);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_FORMAT] = new Uniform<Integer>(GLES10.GL_RGBA);
		}
		uniforms[UniformId.TEXTURE1_FORMAT].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_FORMAT && UniformId.TEXTURE2_FORMAT <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_FORMAT] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_FORMAT", GLES10.GL_RGBA);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_FORMAT] = new Uniform<Integer>(GLES10.GL_RGBA);
		}
		uniforms[UniformId.TEXTURE2_FORMAT].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_MODE && UniformId.TEXTURE0_ENV_MODE <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_MODE] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_MODE", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_MODE] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE0_ENV_MODE].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_MODE && UniformId.TEXTURE1_ENV_MODE <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_MODE] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_MODE", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_MODE] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE1_ENV_MODE].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_MODE && UniformId.TEXTURE2_ENV_MODE <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_MODE] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_MODE", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_MODE] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE2_ENV_MODE].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_COMBINE_RGB && UniformId.TEXTURE0_ENV_COMBINE_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_COMBINE_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_COMBINE_RGB", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_COMBINE_RGB] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE0_ENV_COMBINE_RGB].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_COMBINE_RGB && UniformId.TEXTURE1_ENV_COMBINE_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_COMBINE_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_COMBINE_RGB", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_COMBINE_RGB] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE1_ENV_COMBINE_RGB].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_COMBINE_RGB && UniformId.TEXTURE2_ENV_COMBINE_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_COMBINE_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_COMBINE_RGB", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_COMBINE_RGB] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE2_ENV_COMBINE_RGB].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_COMBINE_ALPHA && UniformId.TEXTURE0_ENV_COMBINE_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_COMBINE_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_COMBINE_ALPHA", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_COMBINE_ALPHA] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE0_ENV_COMBINE_ALPHA].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_COMBINE_ALPHA && UniformId.TEXTURE1_ENV_COMBINE_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_COMBINE_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_COMBINE_ALPHA", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_COMBINE_ALPHA] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE1_ENV_COMBINE_ALPHA].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_COMBINE_ALPHA && UniformId.TEXTURE2_ENV_COMBINE_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_COMBINE_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_COMBINE_ALPHA", GLES10.GL_MODULATE);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_COMBINE_ALPHA] = new Uniform<Integer>(GLES10.GL_MODULATE);
		}
		uniforms[UniformId.TEXTURE2_ENV_COMBINE_ALPHA].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_SRC0_RGB && UniformId.TEXTURE0_ENV_SRC0_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC0_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_SRC0_RGB", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC0_RGB] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE0_ENV_SRC0_RGB].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_SRC1_RGB && UniformId.TEXTURE0_ENV_SRC1_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC1_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_SRC1_RGB", GLES11.GL_PREVIOUS);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC1_RGB] = new Uniform<Integer>(GLES11.GL_PREVIOUS);
		}
		uniforms[UniformId.TEXTURE0_ENV_SRC1_RGB].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_SRC2_RGB && UniformId.TEXTURE0_ENV_SRC2_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC2_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_SRC2_RGB", GLES11.GL_CONSTANT);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC2_RGB] = new Uniform<Integer>(GLES11.GL_CONSTANT);
		}
		uniforms[UniformId.TEXTURE0_ENV_SRC2_RGB].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_SRC0_RGB && UniformId.TEXTURE1_ENV_SRC0_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC0_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_SRC0_RGB", 1);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC0_RGB] = new Uniform<Integer>(1);
		}
		uniforms[UniformId.TEXTURE1_ENV_SRC0_RGB].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_SRC1_RGB && UniformId.TEXTURE1_ENV_SRC1_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC1_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_SRC1_RGB", GLES11.GL_PREVIOUS);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC1_RGB] = new Uniform<Integer>(GLES11.GL_PREVIOUS);
		}
		uniforms[UniformId.TEXTURE1_ENV_SRC1_RGB].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_SRC2_RGB && UniformId.TEXTURE1_ENV_SRC2_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC2_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_SRC2_RGB", GLES11.GL_CONSTANT);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC2_RGB] = new Uniform<Integer>(GLES11.GL_CONSTANT);
		}
		uniforms[UniformId.TEXTURE1_ENV_SRC2_RGB].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_SRC0_RGB && UniformId.TEXTURE2_ENV_SRC0_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC0_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_SRC0_RGB", 2);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC0_RGB] = new Uniform<Integer>(2);
		}
		uniforms[UniformId.TEXTURE2_ENV_SRC0_RGB].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_SRC1_RGB && UniformId.TEXTURE2_ENV_SRC1_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC1_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_SRC1_RGB", GLES11.GL_PREVIOUS);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC1_RGB] = new Uniform<Integer>(GLES11.GL_PREVIOUS);
		}
		uniforms[UniformId.TEXTURE2_ENV_SRC1_RGB].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_SRC2_RGB && UniformId.TEXTURE2_ENV_SRC2_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC2_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_SRC2_RGB", GLES11.GL_CONSTANT);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC2_RGB] = new Uniform<Integer>(GLES11.GL_CONSTANT);
		}
		uniforms[UniformId.TEXTURE2_ENV_SRC2_RGB].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_OPERAND0_RGB && UniformId.TEXTURE0_ENV_OPERAND0_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND0_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_OPERAND0_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND0_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE0_ENV_OPERAND0_RGB].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_OPERAND1_RGB && UniformId.TEXTURE0_ENV_OPERAND1_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND1_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_OPERAND1_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND1_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE0_ENV_OPERAND1_RGB].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_OPERAND2_RGB && UniformId.TEXTURE0_ENV_OPERAND2_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND2_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_OPERAND2_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND2_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE0_ENV_OPERAND2_RGB].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_OPERAND0_RGB && UniformId.TEXTURE1_ENV_OPERAND0_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND0_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_OPERAND0_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND0_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE1_ENV_OPERAND0_RGB].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_OPERAND1_RGB && UniformId.TEXTURE1_ENV_OPERAND1_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND1_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_OPERAND1_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND1_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE1_ENV_OPERAND1_RGB].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_OPERAND2_RGB && UniformId.TEXTURE1_ENV_OPERAND2_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND2_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_OPERAND2_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND2_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE1_ENV_OPERAND2_RGB].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_OPERAND0_RGB && UniformId.TEXTURE2_ENV_OPERAND0_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND0_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_OPERAND0_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND0_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE2_ENV_OPERAND0_RGB].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_OPERAND1_RGB && UniformId.TEXTURE2_ENV_OPERAND1_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND1_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_OPERAND1_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND1_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE2_ENV_OPERAND1_RGB].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_OPERAND2_RGB && UniformId.TEXTURE2_ENV_OPERAND2_RGB <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND2_RGB] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_OPERAND2_RGB", GLES10.GL_SRC_COLOR);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND2_RGB] = new Uniform<Integer>(GLES10.GL_SRC_COLOR);
		}
		uniforms[UniformId.TEXTURE2_ENV_OPERAND2_RGB].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_SRC0_ALPHA && UniformId.TEXTURE0_ENV_SRC0_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC0_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_SRC0_ALPHA", 0); // TODO: default value
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC0_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE0_ENV_SRC0_ALPHA].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_SRC1_ALPHA && UniformId.TEXTURE0_ENV_SRC1_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC1_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_SRC1_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC1_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE0_ENV_SRC1_ALPHA].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_SRC2_ALPHA && UniformId.TEXTURE0_ENV_SRC2_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC2_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_SRC2_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_SRC2_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE0_ENV_SRC2_ALPHA].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_SRC0_ALPHA && UniformId.TEXTURE1_ENV_SRC0_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC0_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_SRC0_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC0_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE1_ENV_SRC0_ALPHA].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_SRC1_ALPHA && UniformId.TEXTURE1_ENV_SRC1_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC1_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_SRC1_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC1_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE1_ENV_SRC1_ALPHA].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_SRC2_ALPHA && UniformId.TEXTURE1_ENV_SRC2_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC2_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_SRC2_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_SRC2_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE1_ENV_SRC2_ALPHA].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_SRC0_ALPHA && UniformId.TEXTURE2_ENV_SRC0_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC0_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_SRC0_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC0_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE2_ENV_SRC0_ALPHA].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_SRC1_ALPHA && UniformId.TEXTURE2_ENV_SRC1_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC1_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_SRC1_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC1_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE2_ENV_SRC1_ALPHA].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_SRC2_ALPHA && UniformId.TEXTURE2_ENV_SRC2_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC2_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_SRC2_ALPHA", 0);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_SRC2_ALPHA] = new Uniform<Integer>(0);
		}
		uniforms[UniformId.TEXTURE2_ENV_SRC2_ALPHA].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_OPERAND0_ALPHA && UniformId.TEXTURE0_ENV_OPERAND0_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND0_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_OPERAND0_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND0_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE0_ENV_OPERAND0_ALPHA].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_OPERAND1_ALPHA && UniformId.TEXTURE0_ENV_OPERAND1_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND1_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_OPERAND1_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND1_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE0_ENV_OPERAND1_ALPHA].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE0_ENV_OPERAND2_ALPHA && UniformId.TEXTURE0_ENV_OPERAND2_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND2_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE0_FRAGMENT_SHADER], "TEXTURE0_ENV_OPERAND2_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE0_ENV_OPERAND2_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE0_ENV_OPERAND2_ALPHA].setFather(texture0Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_OPERAND0_ALPHA && UniformId.TEXTURE1_ENV_OPERAND0_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND0_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_OPERAND0_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND0_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE1_ENV_OPERAND0_ALPHA].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_OPERAND1_ALPHA && UniformId.TEXTURE1_ENV_OPERAND1_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND1_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_OPERAND1_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND1_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE1_ENV_OPERAND1_ALPHA].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE1_ENV_OPERAND2_ALPHA && UniformId.TEXTURE1_ENV_OPERAND2_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND2_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE1_FRAGMENT_SHADER], "TEXTURE1_ENV_OPERAND2_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE1_ENV_OPERAND2_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE1_ENV_OPERAND2_ALPHA].setFather(texture1Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_OPERAND0_ALPHA && UniformId.TEXTURE2_ENV_OPERAND0_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND0_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_OPERAND0_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND0_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE2_ENV_OPERAND0_ALPHA].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_OPERAND1_ALPHA && UniformId.TEXTURE2_ENV_OPERAND1_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND1_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_OPERAND1_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND1_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE2_ENV_OPERAND1_ALPHA].setFather(texture2Enabled);

		if (UniformId.FIRST_STATE_UNIFORM_INT <= UniformId.TEXTURE2_ENV_OPERAND2_ALPHA && UniformId.TEXTURE2_ENV_OPERAND2_ALPHA <= UniformId.LAST_STATE_UNIFORM_INT)
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND2_ALPHA] = new UniformState<Integer>(shaders[ShaderId.TEXTURE2_FRAGMENT_SHADER], "TEXTURE2_ENV_OPERAND2_ALPHA", GLES10.GL_SRC_ALPHA);
		}
		else
		{
			uniforms[UniformId.TEXTURE2_ENV_OPERAND2_ALPHA] = new Uniform<Integer>(GLES10.GL_SRC_ALPHA);
		}
		uniforms[UniformId.TEXTURE2_ENV_OPERAND2_ALPHA].setFather(texture2Enabled);


		// Non-state uniforms

		uniforms[UniformId.LIGHT0_AMBIENT] = new Uniform4f(0.0f, 0.0f, 0.0f, 1.0f);
		uniforms[UniformId.LIGHT1_AMBIENT] = new Uniform4f(0.0f, 0.0f, 0.0f, 1.0f);
		uniforms[UniformId.LIGHT2_AMBIENT] = new Uniform4f(0.0f, 0.0f, 0.0f, 1.0f);
		uniforms[UniformId.LIGHT0_DIFFUSE] = new Uniform4f(1.0f, 1.0f, 1.0f, 1.0f);
		uniforms[UniformId.LIGHT1_DIFFUSE] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.LIGHT2_DIFFUSE] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.LIGHT0_SPECULAR] = new Uniform4f(1.0f, 1.0f, 1.0f, 1.0f);
		uniforms[UniformId.LIGHT1_SPECULAR] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.LIGHT2_SPECULAR] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.LIGHT0_POSITION] = new Uniform4f(0.0f, 0.0f, 1.0f, 0.0f);
		uniforms[UniformId.LIGHT1_POSITION] = new Uniform4f(0.0f, 0.0f, 1.0f, 0.0f);
		uniforms[UniformId.LIGHT2_POSITION] = new Uniform4f(0.0f, 0.0f, 1.0f, 0.0f);
		uniforms[UniformId.LIGHT0_SPOT_DIRECTION] = new Uniform3f(0.0f, 0.0f, -1.0f);
		uniforms[UniformId.LIGHT1_SPOT_DIRECTION] = new Uniform3f(0.0f, 0.0f, -1.0f);
		uniforms[UniformId.LIGHT2_SPOT_DIRECTION] = new Uniform3f(0.0f, 0.0f, -1.0f);
		uniforms[UniformId.LIGHT0_SPOT_EXPONENT] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT1_SPOT_EXPONENT] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT2_SPOT_EXPONENT] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT0_SPOT_CUTOFF_ANGLE_COS] = new Uniform<Float>(-1.0f);
		uniforms[UniformId.LIGHT1_SPOT_CUTOFF_ANGLE_COS] = new Uniform<Float>(-1.0f);
		uniforms[UniformId.LIGHT2_SPOT_CUTOFF_ANGLE_COS] = new Uniform<Float>(-1.0f);
		uniforms[UniformId.LIGHT0_CONSTANT_ATTENUATION] = new Uniform<Float>(1.0f);
		uniforms[UniformId.LIGHT1_CONSTANT_ATTENUATION] = new Uniform<Float>(1.0f);
		uniforms[UniformId.LIGHT2_CONSTANT_ATTENUATION] = new Uniform<Float>(1.0f);
		uniforms[UniformId.LIGHT0_LINEAR_ATTENUATION] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT1_LINEAR_ATTENUATION] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT2_LINEAR_ATTENUATION] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT0_QUADRATIC_ATTENUATION] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT1_QUADRATIC_ATTENUATION] = new Uniform<Float>(0.0f);
		uniforms[UniformId.LIGHT2_QUADRATIC_ATTENUATION] = new Uniform<Float>(0.0f);
		uniforms[UniformId.MATERIAL_AMBIENT] = new Uniform4f(0.2f, 0.2f, 0.2f, 1.0f);
		uniforms[UniformId.MATERIAL_DIFFUSE] = new Uniform4f(0.8f, 0.8f, 0.8f, 1.0f);
		uniforms[UniformId.MATERIAL_SPECULAR] = new Uniform4f(0.0f, 0.0f, 0.0f, 1.0f);
		uniforms[UniformId.MATERIAL_EMISSION] = new Uniform4f(0.0f, 0.0f, 0.0f, 1.0f);
		uniforms[UniformId.MATERIAL_SHININESS] = new Uniform<Float>(0.0f);
		uniforms[UniformId.MODELVIEW_MATRIX] = new Uniform<Matrix4x4f>();
		uniforms[UniformId.MODELVIEW_PROJECTION_MATRIX] = new Uniform<Matrix4x4f>();
		uniforms[UniformId.TRANPOSE_ADJOINT_MODEL_VIEW_MATRIX] = new Uniform<Matrix3x3f>();
		uniforms[UniformId.RESCALE_NORMAL_FACTOR] = new Uniform<Float>(1.0f);
		uniforms[UniformId.FOG_COLOR] = new Uniform3f(0.0f, 0.0f, 0.0f);
		uniforms[UniformId.FOG_DENSITY] = new Uniform<Float>(1.0f);
		uniforms[UniformId.FOG_START] = new Uniform<Float>(1.0f);
		uniforms[UniformId.FOG_END] = new Uniform<Float>(0.0f);
		uniforms[UniformId.GLOBAL_AMBIENT_COLOR] = new Uniform4f(0.2f, 0.2f, 0.2f, 1.0f);
		uniforms[UniformId.ALPHA_FUNC_VALUE] = new Uniform<Float>(0.0f);
		uniforms[UniformId.CLIP_PLANE0_EQUATION] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.CLIP_PLANE1_EQUATION] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.CLIP_PLANE2_EQUATION] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.CLIP_PLANE3_EQUATION] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.CLIP_PLANE4_EQUATION] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.CLIP_PLANE5_EQUATION] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.TEXTURE0_ENV_COLOR] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.TEXTURE1_ENV_COLOR] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.TEXTURE2_ENV_COLOR] = new Uniform4f(0.0f, 0.0f, 0.0f, 0.0f);
		uniforms[UniformId.TEXTURE0_ENV_RGB_SCALE] = new Uniform<Float>(1.0f);
		uniforms[UniformId.TEXTURE1_ENV_RGB_SCALE] = new Uniform<Float>(1.0f);
		uniforms[UniformId.TEXTURE2_ENV_RGB_SCALE] = new Uniform<Float>(1.0f);
		uniforms[UniformId.TEXTURE0_ENV_ALPHA_SCALE] = new Uniform<Float>(1.0f);
		uniforms[UniformId.TEXTURE1_ENV_ALPHA_SCALE] = new Uniform<Float>(1.0f);
		uniforms[UniformId.TEXTURE2_ENV_ALPHA_SCALE] = new Uniform<Float>(1.0f);
		uniforms[UniformId.TEXTURE0_ENV_BLUR_AMOUNT] = new Uniform<Float>(0.0f);
		uniforms[UniformId.TEXTURE1_ENV_BLUR_AMOUNT] = new Uniform<Float>(0.0f);
		uniforms[UniformId.TEXTURE2_ENV_BLUR_AMOUNT] = new Uniform<Float>(0.0f);
		uniforms[UniformId.TEXTURE0_SAMPLER] = new Uniform<Integer>(0);
		uniforms[UniformId.TEXTURE1_SAMPLER] = new Uniform<Integer>(1);
		uniforms[UniformId.TEXTURE2_SAMPLER] = new Uniform<Integer>(2);
		uniforms[UniformId.TEXTURE0_MATRIX] = new Uniform<Matrix4x4f>();
		uniforms[UniformId.TEXTURE1_MATRIX] = new Uniform<Matrix4x4f>();
		uniforms[UniformId.TEXTURE2_MATRIX] = new Uniform<Matrix4x4f>();
	}
	private boolean setCurrentProgram_uberShaderCompiled = false;
	public void setCurrentProgram()
	{
		if (OpenGLESConfig.USE_ONLY_UBER_SHADER)
		{
//					static boolean uberShaderCompiled = false;
			if (!setCurrentProgram_uberShaderCompiled)
			{
				java.util.ArrayList<ShaderSource > vertexShaderSources = new java.util.ArrayList<ShaderSource >();
				for (int i = ShaderId.FIRST_VERTEX_SHADER; i <= ShaderId.LAST_VERTEX_SHADER; i++)
				{
					vertexShaderSources.add(new ShaderSource(shaders[i], ""));
				}
				java.util.ArrayList<ShaderSource  > fragmentShaderSources = new java.util.ArrayList<ShaderSource  >();
				for (int i = ShaderId.FIRST_FRAGMENT_SHADER; i <= ShaderId.LAST_FRAGMENT_SHADER; i++)
				{
					fragmentShaderSources.add(new ShaderSource(shaders[i], ""));
				}

				Shader vertexShader = new Shader(GLES20.GL_VERTEX_SHADER, vertexShaderSources);
				Shader fragmentShader = new Shader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSources);

				currentStateShaderProgram = new StateShaderProgram(getCopyOfCurrentState(), new ShaderProgram("UberShader", vertexShader, fragmentShader));
				stateShaderPrograms.add(currentStateShaderProgram);
				currentStateShaderProgram.shaderProgram.use();
				setActiveUniformLocations(currentStateShaderProgram.shaderProgram.getActiveUniforms());
				setActiveAttributeLocations(currentStateShaderProgram.shaderProgram.getActiveAttributes());

				setCurrentProgram_uberShaderCompiled = true;
			}

			uploadUniforms();
			uploadAttributes();
			return;
		}

		StateShaderProgram oldStateShaderProgram = currentStateShaderProgram;

		// Set current state to faster array
		int currentBit = 0;
		for (int i = UniformId.FIRST_STATE_UNIFORM_BOOL; i <= UniformId.LAST_STATE_UNIFORM_BOOL; i++)
		{
			if (currentBit % 32 == 0)
			{
				currentState[currentBit / 32] = 0;
			}

			boolean value = ((UniformState<Boolean>)(uniforms[i])).getValue();
			currentState[currentBit / 32] |=  (value ? 1 : 0) << (currentBit % 32);

			currentBit++;
		}

		int index = stateSizeBool;
		for (int i = UniformId.FIRST_STATE_UNIFORM_INT; i <= UniformId.LAST_STATE_UNIFORM_INT; i++)
		{
			int val = ((UniformState<Integer>)(uniforms[i])).getValue();

			currentState[index] = val;

			index++;
		}

		// Check if it matches to any existing state
		int stateIndex = -1;
		for (int i = 0; i < stateShaderPrograms.size(); i++)
		{
			boolean stateFound = true;
			for (int j = 0; j < stateSize; j++)
			{
				if (currentState[j] != stateShaderPrograms.get(i).state[j])
				{
					stateFound = false;
					break;
				}
			}
			if (stateFound)
			{
				stateIndex = i;
				break;
			}
		}

		// If matched, fetch shader program from cache
		if (stateIndex >= 0)
		{
			currentStateShaderProgram = stateShaderPrograms.get(stateIndex);
		}
		else
		{
			Log.d(TAG, "State binary presentation:");
			Log.d(TAG, "Bool states: ");
			for (int i = 0; i < stateSizeBool; i++)
			{
				Log.d(TAG, Integer.toBinaryString(currentState[i]));
			}
			Log.d(TAG, "Int states: ");
			for (int i = stateSizeBool; i < stateSize; i++)
			{
			    Log.d(TAG, Integer.toHexString(currentState[i]));
			}

			java.util.ArrayList<ShaderSource > vertexShaderSources = new java.util.ArrayList<ShaderSource >();
			java.util.ArrayList<ShaderSource  > fragmentShaderSources = new java.util.ArrayList<ShaderSource  >();

			addRequiredShaderSources(vertexShaderSources, fragmentShaderSources);

			addDefinesToShaderSources(vertexShaderSources, fragmentShaderSources);

			if (true)
			{
				Log.d(TAG, "Using shader files:");
				for (int i = 0; i < vertexShaderSources.size(); i++)
				{
					Log.d(TAG, vertexShaderSources.get(i).getFile().getName());
				}

				for (int i = 0; i < fragmentShaderSources.size(); i++)
				{
				    Log.d(TAG, fragmentShaderSources.get(i).getFile().getName());
				}
			}

			Shader vertexShader = new Shader(GLES20.GL_VERTEX_SHADER, vertexShaderSources);
			Shader fragmentShader = new Shader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSources);

			currentStateShaderProgram = new StateShaderProgram(getCopyOfCurrentState(), new ShaderProgram("Optimized Shader " + (stateShaderPrograms.size() + 1), vertexShader, fragmentShader));
			stateShaderPrograms.add(currentStateShaderProgram);
		}

		if (currentStateShaderProgram != oldStateShaderProgram)
		{
			currentStateShaderProgram.shaderProgram.use();
			setActiveUniformLocations(currentStateShaderProgram.shaderProgram.getActiveUniforms());
			setActiveAttributeLocations(currentStateShaderProgram.shaderProgram.getActiveAttributes());
		}

		uploadAttributes();
		uploadUniforms();
	}

	public void setActiveTexture(int a)
	{
		activeTexture = a;
	}
	
	public int getActiveTexture()
	{
		return activeTexture;
	}
	
	public void setClientActiveTexture(int a)
	{
		clientActiveTexture = a;
	}
	
	public int getClientActiveTexture()
	{
		return clientActiveTexture;
	}
	
	public void setBoundTextureFormat(int format)
	{
		boundTextureFormats.put(boundTextures[activeTexture], format);
	}
	
	public void setBoundTexture(int i)
	{
		boundTextures[activeTexture] = i;
	}

	public void setPosition(int size, int type, int stride, java.nio.Buffer pointer)
	{
		attributes[AttributeId.POSITION].setValues(size, type, stride, pointer);
	}
	
	public void setNormal(int size, int type, int stride, java.nio.Buffer pointer)
	{
		attributes[AttributeId.NORMAL].setValues(size, type, stride, pointer);
	}
	
	public void setColor(int size, int type, int stride, java.nio.Buffer pointer)
	{
		attributes[AttributeId.COLOR].setValues(size, type, stride, pointer);
	}
	
	public void setTexCoord(int size, int type, int stride, java.nio.Buffer pointer)
	{
		attributes[AttributeId.TEXCOORD0 + clientActiveTexture].setValues(size, type, stride, pointer);
	}

	public void setPosition(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.POSITION_ENABLED])).setValue(enabled);
		attributes[AttributeId.POSITION].setEnabled(enabled);
	}
	
	public void setNormal(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.NORMAL_ENABLED])).setValue(enabled);
		attributes[AttributeId.NORMAL].setEnabled(enabled);
	}
	
	public boolean isNormal()
	{
		return ((Uniform<Boolean>)(uniforms[UniformId.NORMAL_ENABLED])).getValue();
	}
	
	public void setColor(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.COLOR_ENABLED])).setValue(enabled);
		attributes[AttributeId.COLOR].setEnabled(enabled);
	}
	
	public void setTexCoord(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.TEXCOORD0_ENABLED + clientActiveTexture])).setValue(enabled);
		attributes[AttributeId.TEXCOORD0 + clientActiveTexture].setEnabled(enabled);
	}
	
	public boolean isTexCoord(int index)
	{
		return ((Uniform<Boolean>)(uniforms[UniformId.TEXCOORD0_ENABLED + index])).getValue();
	}
	
	public void setTexture(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.TEXTURE0_ENABLED + activeTexture])).setValue(enabled);
	}
	
	public void setTextureFormat()
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_FORMAT + activeTexture])).setValue(boundTextureFormats.get(boundTextures[activeTexture]));
	}
	
	public void setTextureEnvMode(int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_ENV_MODE + activeTexture])).setValue(val);
	}
	
	public void setTextureEnvColor(Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.TEXTURE0_ENV_COLOR + activeTexture])).setValue(vec);
	}
	
	public void setTextureEnvCombineRGB(int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_ENV_COMBINE_RGB + activeTexture])).setValue(val);
	}
	
	public void setTextureEnvCombineAlpha(int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_ENV_COMBINE_ALPHA + activeTexture])).setValue(val);
	}
	
	public void setTextureEnvSrcRGB(int index, int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_ENV_SRC0_RGB + index + 3 * activeTexture])).setValue(val);
	}
	
	public void setTextureEnvOperandRGB(int index, int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_ENV_OPERAND0_RGB + index + 3 * activeTexture])).setValue(val);
	}
	
	public void setTextureEnvSrcAlpha(int index, int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_ENV_SRC0_ALPHA + index + 3 * activeTexture])).setValue(val);
	}
	
	public void setTextureEnvOperandAlpha(int index, int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.TEXTURE0_ENV_OPERAND0_ALPHA + index + 3 * activeTexture])).setValue(val);
	}
	
	public void setTextureEnvRGBScale(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.TEXTURE0_ENV_RGB_SCALE + activeTexture])).setValue(val);
	}
	
	public void setTextureEnvAlphaScale(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.TEXTURE0_ENV_ALPHA_SCALE + activeTexture])).setValue(val);
	}
	
	public void setTextureEnvBlurAmount(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.TEXTURE0_ENV_BLUR_AMOUNT + activeTexture])).setValue(val);
	}
	
	public void setLightModelLocalViewer(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.LIGHT_MODEL_LOCAL_VIEWER_ENABLED])).setValue(enabled);
	}
	
	public void setLightModelTwoSide(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.LIGHT_MODEL_TWO_SIDE_ENABLED])).setValue(enabled);
	}
	
	public void setLighting(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.LIGHTING_ENABLED])).setValue(enabled);
	}
	
	public void setLightingHint(int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.LIGHTING_HINT])).setValue(val);
	}
	
	public void setLight(int light, boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.LIGHT0_ENABLED + light])).setValue(enabled);
	}
	
	public void setLightAmbient(int light, Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.LIGHT0_AMBIENT + light])).setValue(vec);
	}
	
	public void setLightDiffuse(int light, Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.LIGHT0_DIFFUSE + light])).setValue(vec);
	}
	
	public void setLightSpecular(int light, Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.LIGHT0_SPECULAR + light])).setValue(vec);
	}
	
	public void setLightPosition(int light, Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.LIGHT0_POSITION + light])).setValue(vec);
	}
	
	public void setLightSpotDirection(int light, Vector3f vec)
	{
		((Uniform<Vector3f>)(uniforms[UniformId.LIGHT0_SPOT_DIRECTION + light])).setValue(new Vector3f(vec));
	}
	
	public void setLightSpotExponent(int light, float val)
	{
		((Uniform<Float>)(uniforms[UniformId.LIGHT0_SPOT_EXPONENT + light])).setValue(val);
	}
	
	public void setLightSpotCutoffAngleCos(int light, float val)
	{
		((Uniform<Float>)(uniforms[UniformId.LIGHT0_SPOT_CUTOFF_ANGLE_COS + light])).setValue(val);
	}
	
	public void setLightConstantAttenuation(int light, float val)
	{
		((Uniform<Float>)(uniforms[UniformId.LIGHT0_CONSTANT_ATTENUATION + light])).setValue(val);
	}
	
	public void setLightLinearAttenuation(int light, float val)
	{
		((Uniform<Float>)(uniforms[UniformId.LIGHT0_LINEAR_ATTENUATION + light])).setValue(val);
	}
	
	public void setLightQuadraticAttenuation(int light, float val)
	{
		((Uniform<Float>)(uniforms[UniformId.LIGHT0_QUADRATIC_ATTENUATION + light])).setValue(val);
	}
	
	public void setMaterialAmbient(Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.MATERIAL_AMBIENT])).setValue(vec);
	}
	
	public void setMaterialDiffuse(Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.MATERIAL_DIFFUSE])).setValue(vec);
	}
	
	public void setMaterialSpecular(Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.MATERIAL_SPECULAR])).setValue(vec);
	}
	
	public void setMaterialEmission(Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.MATERIAL_EMISSION])).setValue(vec);
	}
	
	public void setMaterialShininess(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.MATERIAL_SHININESS])).setValue(val);
	}
	
	public void setModelViewMatrix(Matrix4x4f mat)
	{
		((Uniform<Matrix4x4f>)(uniforms[UniformId.MODELVIEW_MATRIX])).setValue(new Matrix4x4f(mat));
	}
	
	public void setModelViewProjectionMatrix(Matrix4x4f mat)
	{
		((Uniform<Matrix4x4f>)(uniforms[UniformId.MODELVIEW_PROJECTION_MATRIX])).setValue(new Matrix4x4f(mat));
	}
	
	public void setTransposeAdjointModelViewMatrix(Matrix3x3f mat)
	{
		((Uniform<Matrix3x3f>)(uniforms[UniformId.TRANPOSE_ADJOINT_MODEL_VIEW_MATRIX])).setValue(new Matrix3x3f(mat));
	}
	
	public void setNormalize(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.NORMALIZE_ENABLED])).setValue(enabled);
	}
	
	public void setRescaleNormal(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.RESCALE_NORMAL_ENABLED])).setValue(enabled);
	}
	
	public boolean isRescaleNormal()
	{
		return ((Uniform<Boolean>)(uniforms[UniformId.RESCALE_NORMAL_ENABLED])).getValue();
	}
	
	public void setRescaleNormalFactor(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.RESCALE_NORMAL_FACTOR])).setValue(val);
	}
	
	public void setGlobalAmbientColor(Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.GLOBAL_AMBIENT_COLOR])).setValue(vec);
	}
	
	public void setFog(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.FOG_ENABLED])).setValue(enabled);
	}
	
	public void setFogColor(Vector3f vec)
	{
		((Uniform<Vector3f>)(uniforms[UniformId.FOG_COLOR])).setValue(vec);
	}
	
	public void setFogMode(int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.FOG_MODE])).setValue(val);
	}
	
	public void setFogDensity(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.FOG_DENSITY])).setValue(val);
	}
	
	public void setFogStart(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.FOG_START])).setValue(val);
	}
	
	public void setFogEnd(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.FOG_END])).setValue(val);
	}
	
	public void setFogHint(int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.FOG_HINT])).setValue(val);
	}
	
	public void setAlphaTest(boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.ALPHA_TEST_ENABLED])).setValue(enabled);
	}
	
	public void setAlphaFunc(int val)
	{
		((Uniform<Integer>)(uniforms[UniformId.ALPHA_FUNC])).setValue(val);
	}
	
	public void setAlphaFuncValue(float val)
	{
		((Uniform<Float>)(uniforms[UniformId.ALPHA_FUNC_VALUE])).setValue(val);
	}
	
	public void setClipPlane(int clipPlaneIndex, boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.CLIP_PLANE0_ENABLED + clipPlaneIndex])).setValue(enabled);
	}
	
	public void setClipPlane(int clipPlaneIndex, Vector4f vec)
	{
		((Uniform<Vector4f>)(uniforms[UniformId.CLIP_PLANE0_EQUATION + clipPlaneIndex])).setValue(vec);
	}
	
	public void getClipPlane(int clipPlaneIndex, float[] eqn)
	{
		Vector4f vec = ((Uniform<Vector4f>)(uniforms[UniformId.CLIP_PLANE0_EQUATION + clipPlaneIndex])).getValue();
		eqn[0] = vec.getItem(0);
		eqn[1] = vec.getItem(1);
		eqn[2] = vec.getItem(2);
		eqn[3] = vec.getItem(3);
	}
	
	public void setTextureMatrix(int index, Matrix4x4f mat)
	{
		((Uniform<Matrix4x4f>)(uniforms[UniformId.TEXTURE0_MATRIX + index])).setValue(mat);
	}
	
	public void setTextureMatrix(int index, boolean enabled)
	{
		((Uniform<Boolean>)(uniforms[UniformId.TEXTURE0_MATRIX_ENABLED + index])).setValue(enabled);
	}

	public int getCachedShaderAmount()
	{
		return stateShaderPrograms.size();
	}

	private void setActiveUniformLocations(java.util.ArrayList<UniformSimple > activeUniforms)
	{
		for (int i = 0; i < activeUniforms.size(); i++)
		{
			UniformSimple activeUniform = activeUniforms.get(i);
			int index = activeUniform.getId();
			uniforms[index].setLocation(activeUniform.getLocation());
		}
	}
	
	private void setActiveAttributeLocations(java.util.ArrayList<AttributeSimple > activeAttributes)
	{
		for (int i = 0; i < activeAttributes.size(); i++)
		{
			AttributeSimple activeAttribute = activeAttributes.get(i);
			int index = activeAttribute.getId();
			attributes[index].setLocation(activeAttribute.getLocation());
		}
	}
	
	private void uploadAttributes()
	{
		java.util.ArrayList<AttributeSimple > activeAttributes = currentStateShaderProgram.shaderProgram.getActiveAttributes();

		for (int i = 0; i < activeAttributes.size(); i++)
		{
			AttributeSimple activeAttribute = activeAttributes.get(i);
			attributes[activeAttribute.getId()].upload(currentStateShaderProgram.shaderProgram);
		}
	}
	
	private void uploadUniforms()
	{
		java.util.ArrayList<UniformSimple > activeUniforms = currentStateShaderProgram.shaderProgram.getActiveUniforms();

		for (int i = 0; i < activeUniforms.size(); i++)
		{
			UniformSimple activeUniform = activeUniforms.get(i);
			uniforms[activeUniform.getId()].upload(currentStateShaderProgram.shaderProgram);
		}
	}
	
	private void addRequiredShaderSources(java.util.ArrayList<ShaderSource > vertexShaderSources, java.util.ArrayList<ShaderSource > fragmentShaderSources)
	{
		vertexShaderSources.add(new ShaderSource(shaders[ShaderId.MAIN_VERTEX_SHADER]));
		fragmentShaderSources.add(new ShaderSource(shaders[ShaderId.MAIN_FRAGMENT_SHADER]));

		for (int i = 0; i < UniformId.COUNT; i++)
		{
			java.util.ArrayList<ShaderFile > additionalRequiredShaderFiles = uniforms[i].getAdditionalRequiredShaderFiles();
			for (int j = 0; j < additionalRequiredShaderFiles.size(); j++)
			{
				boolean shaderFileFound = false;
				if (additionalRequiredShaderFiles.get(j).getType() == GLES20.GL_VERTEX_SHADER)
				{
					for (int k = 0; k < vertexShaderSources.size(); k++)
					{
						if (vertexShaderSources.get(k).getFile() == additionalRequiredShaderFiles.get(j))
						{
							shaderFileFound = true;
							break;
						}
					}
					if (!shaderFileFound)
					{
						vertexShaderSources.add(new ShaderSource(additionalRequiredShaderFiles.get(j)));
					}
				}
				else
				{
					for (int k = 0; k < fragmentShaderSources.size(); k++)
					{
						if (fragmentShaderSources.get(k).getFile() == additionalRequiredShaderFiles.get(j))
						{
							shaderFileFound = true;
							break;
						}
					}
					if (!shaderFileFound)
					{
						fragmentShaderSources.add(new ShaderSource(additionalRequiredShaderFiles.get(j)));
					}
				}
			}
		}
	}
	
	private void addDefinesToShaderSources(java.util.ArrayList<ShaderSource > vertexShaderSources, java.util.ArrayList<ShaderSource > fragmentShaderSources)
	{
		for (int i = UniformId.FIRST_STATE_UNIFORM_BOOL; i <= UniformId.LAST_STATE_UNIFORM_BOOL; i++)
		{
			UniformState<Boolean> uniformState = (UniformState<Boolean>)(uniforms[i]);

			java.util.ArrayList<ShaderFile > defineShaderFiles = uniformState.getDefineShaderFiles();
			String define = uniformState.getDefine();
			addDefineToShaderSources(define, defineShaderFiles, vertexShaderSources, fragmentShaderSources);
		}
		for (int i = UniformId.FIRST_STATE_UNIFORM_INT; i <= UniformId.LAST_STATE_UNIFORM_INT; i++)
		{
			UniformState<Integer> uniformState = (UniformState<Integer>)(uniforms[i]);
			java.util.ArrayList<ShaderFile > defineShaderFiles = uniformState.getDefineShaderFiles();
			String define = uniformState.getDefine();
			addDefineToShaderSources(define, defineShaderFiles, vertexShaderSources, fragmentShaderSources);
		}

		// Additional defines for shader optimization
		boolean light = uniforms[UniformId.LIGHTING_ENABLED] instanceof UniformState<?> ? true : false;
		boolean light0 = uniforms[UniformId.LIGHT0_ENABLED] instanceof UniformState<?> ? true : false;
		boolean light1 = uniforms[UniformId.LIGHT1_ENABLED] instanceof UniformState<?> ? true : false;
		boolean light2 = uniforms[UniformId.LIGHT2_ENABLED] instanceof UniformState<?> ? true : false;
		if (light && light0 && light1 && light2)
		{
			boolean nonDirectionalLightEnabled = false;
			if (((Uniform<Boolean>)(uniforms[UniformId.LIGHTING_ENABLED])).getValue())
			{
				for (int i = 0; i < 3; i++)
				{
					if (((Uniform<Boolean>)(uniforms[UniformId.LIGHT0_ENABLED + i])).getValue() && ((Uniform<Vector4f>)(uniforms[UniformId.LIGHT0_POSITION + i])).getValue().v[3] != 0.0f)
					{
						nonDirectionalLightEnabled = true;
						break;
					}
				}
			}
			String nonDirectionalLightEnabledString = "#define NON_DIRECTIONAL_LIGHT_ENABLED ";
			nonDirectionalLightEnabledString += nonDirectionalLightEnabled ? "1" : "0";
			nonDirectionalLightEnabledString += "\n";
			vertexShaderSources.get(ShaderId.MAIN_VERTEX_SHADER).appendAdditionalSource(nonDirectionalLightEnabledString);
		}
	}
	
	private void addDefineToShaderSources(String define, java.util.ArrayList<ShaderFile > shaderFiles, java.util.ArrayList<ShaderSource > vertexShaderSources, java.util.ArrayList<ShaderSource  > fragmentShaderSources)
	{
		for (int i = 0; i < shaderFiles.size(); i++)
		{
			if (shaderFiles.get(i).getType() == GLES20.GL_VERTEX_SHADER)
			{
				for (int j = 0; j < vertexShaderSources.size(); j++)
				{
					if (shaderFiles.get(i) == vertexShaderSources.get(j).getFile())
					{
						vertexShaderSources.get(j).appendAdditionalSource(define);
					}
				}
			}
			else
			{
				for (int j = 0; j < fragmentShaderSources.size(); j++)
				{
					if (shaderFiles.get(i) == fragmentShaderSources.get(j).getFile())
					{
						fragmentShaderSources.get(j).appendAdditionalSource(define);
					}
				}
			}
		}
	}
	
	private int[] getCopyOfCurrentState()
	{
		int[] state = new int[stateSize];

		for (int i = 0; i < stateSize; i++)
		{
			state[i] = currentState[i];
		}

		return state;
	}
}
