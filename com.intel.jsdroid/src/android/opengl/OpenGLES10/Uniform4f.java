package android.opengl.OpenGLES10;

import java.util.ArrayList;


public class Uniform4f extends UniformBase
{
    protected Vector4f value = new Vector4f();
    
	public Uniform4f(float x, float y, float z, float w)
	{
		super(-1);
		this.value = new Vector4f(x, y, z, w);
	}
	
	public Uniform4f(Vector4f value)
	{
		super(-1);
		this.value.copyFrom(value);
	}
	
	public Uniform4f()
	{
		super(-1);
	}

	public void setValue(Vector4f val)
	{
		uploaded = false;
		value.copyFrom(val);
	}

	public Vector4f getValue()
	{
		return value;
	}

	@Override
	public void upload(ShaderProgram program)
	{
		if (!uploaded)
		{
			program.setUniform4fv(location, 1, value.v);
			uploaded = true;
		}
	}

	@Override
	public ArrayList<ShaderFile > getAdditionalRequiredShaderFiles()
	{
		ArrayList<ShaderFile > shaderFiles = new ArrayList<ShaderFile >();
		for (int i = 0; i < this.additionalRequiredShaderFiles.size(); i++)
		{
			if (this.father == null || ((Uniform<Boolean>)(this.father)).getValue())
			{
				shaderFiles.add(this.additionalRequiredShaderFiles.get(i).getValue());
			}
		}

		return shaderFiles;
	}
}