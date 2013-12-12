package android.opengl.OpenGLES10;

import java.util.AbstractMap;
import java.util.ArrayList;

public abstract class UniformBase
{
    protected int location;
    protected boolean uploaded;
    protected ArrayList<AbstractMap.SimpleEntry<Integer, ShaderFile> > additionalRequiredShaderFiles = null;
    protected UniformBase father;
    
	public UniformBase(int location)
	{
		this.additionalRequiredShaderFiles = new ArrayList<AbstractMap.SimpleEntry<Integer, ShaderFile> >();
		this.location = location;
		this.uploaded = false;
		this.father = null;

	}

	public void setLocation(int loc)
	{
		location = loc;
		uploaded = false;
	}
	
	public int getLocation()
	{
		return location;
	}
	
	public abstract void upload(ShaderProgram program);
	
	public void addAdditionalRequiredShaderFile(int key, ShaderFile additionalRequiredShaderFile)
	{
		additionalRequiredShaderFiles.add(new AbstractMap.SimpleEntry<Integer, ShaderFile>(key, additionalRequiredShaderFile));
	}
	
	public abstract ArrayList<ShaderFile > getAdditionalRequiredShaderFiles();
	
	public void setFather(UniformBase f)
	{
		father = f;
	}
}