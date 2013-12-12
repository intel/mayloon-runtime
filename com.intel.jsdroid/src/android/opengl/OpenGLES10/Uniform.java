package android.opengl.OpenGLES10;

import java.util.ArrayList;


public class Uniform<T> extends UniformBase
{
    protected T value = null;
    
	public Uniform(T value)
	{
		super(-1);
		this.value = value;

	}
	
	public Uniform()
	{
		super(-1);

	}

	public void setValue(T val)
	{
		// TODO: Profile whether this if clause is an optimization
		if (value != val) {
    		uploaded = false;
    		value = val;
		}
	}
	
	public T getValue()
	{
		return value;
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

    @Override
    public void upload(ShaderProgram program) {
        if (uploaded) return;

        if (value instanceof Boolean) {
            program.setUniform1i(location, (Boolean) value ? 1 : 0);
            uploaded = true;
            return;
        } 
        
        if (value instanceof Integer) {
            program.setUniform1i(location, (Integer) value);
            uploaded = true;
            return;
        } 
        
        if (value instanceof Float) {
            program.setUniform1f(location, (Float) value);
            uploaded = true;
            return;
        }  
        
        if (value instanceof Matrix3x3f) {
            Matrix3x3f tmp = (Matrix3x3f)value;
            program.setUniformMatrix3fv(location, tmp.m);
            uploaded = true;
            return;
        }  
        
        if (value instanceof Matrix4x4f) {
            Matrix4x4f tmp = (Matrix4x4f)value;
            program.setUniformMatrix4fv(location, tmp.m);
            uploaded = true;
            return;
        } 
    }
}