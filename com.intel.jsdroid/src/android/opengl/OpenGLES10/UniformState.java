package android.opengl.OpenGLES10;

import java.util.ArrayList;


public class UniformState<T> extends Uniform<T>
{
    private String defineName;
    private ArrayList<ShaderFile > defineShaderFiles = new ArrayList<ShaderFile >();
    
	public UniformState(ShaderFile defineShaderFile, String defineName, T value)
	{
		super(value);
		this.defineName = defineName;
		defineShaderFiles.add(defineShaderFile);
	}

	public void addDefineShaderFile(ShaderFile defineShaderFile)
	{
		defineShaderFiles.add(defineShaderFile);
	}
	
	public ArrayList<ShaderFile > getDefineShaderFiles()
	{
		return defineShaderFiles;
	}
	
	public ArrayList<ShaderFile > getAdditionalRequiredShaderFiles()
	{
		ArrayList<ShaderFile > shaderFiles = new ArrayList<ShaderFile >();
		for (int i = 0; i < this.additionalRequiredShaderFiles.size(); i++)
		{
			if (this.additionalRequiredShaderFiles.get(i).getKey().equals(this.value) && (this.father == null || ((Uniform<Boolean>)(this.father)).getValue() == true))
			{
				shaderFiles.add(this.additionalRequiredShaderFiles.get(i).getValue());
			}
		}

		return shaderFiles;
	}
	
	public T getValue()
	{
		if (this.father == null || ((Uniform<Boolean>)(this.father)).getValue() == true)
		{
			return this.value;
		}
		else
		{
			return null;
		}
	}

    public String getDefine() {
        if (value instanceof Boolean) {
            String define = "#define ";
            define += defineName;
            define += " ";
            define += (Boolean) value ? "1" : "0";
            define += "\n";
    
            return define;
        }   
        
        if (value instanceof Integer) {
            String define = "#define ";
            define += defineName;
            define += " ";
            define += value.toString();
            define += "\n";
    
            return define;
        }
        
        return null;
    }

}
