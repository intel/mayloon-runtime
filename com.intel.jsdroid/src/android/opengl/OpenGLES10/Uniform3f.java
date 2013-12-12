package android.opengl.OpenGLES10;

import java.util.ArrayList;


public class Uniform3f extends UniformBase
{
    protected Vector3f value = new Vector3f();
    
	public Uniform3f(float x, float y, float z)
	{
		super(-1);
		this.value = new Vector3f(x, y, z);
	}
	
	public Uniform3f(Vector3f value)
	{
		super(-1);
		this.value.copyFrom(value);
	}
	
	public Uniform3f()
	{
		super(-1);
	}

	public void setValue(Vector3f val)
	{
		uploaded = false;
		value.copyFrom(val);
	}

	public Vector3f getValue()
	{
		return value;
	}

	@Override
	public void upload(ShaderProgram program)
	{
		if (!uploaded)
		{
			program.setUniform3fv(location, 1, value.v);
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