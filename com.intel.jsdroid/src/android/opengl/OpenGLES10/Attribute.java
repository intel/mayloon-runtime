package android.opengl.OpenGLES10;

import android.opengl.GLES20;


public class Attribute
{
    private int location;
    private boolean enabled;
    private boolean uploaded;

    private int size;

    private int type;
    private boolean normalized;
    private int stride;
    private java.nio.Buffer ptr;
    
	public Attribute()
	{
		this.location = -1;
		this.enabled = false;
		this.uploaded = false;

	}

	public int getLocation()
	{
		return location;
	}
	
	public void setEnabled(boolean e)
	{
		enabled = e;
	}
	
	public void setLocation(int loc)
	{
		location = loc;
	}
	
	public void upload(ShaderProgram program)
	{
		if (enabled)
		{
			GLES20.glEnableVertexAttribArray(location);
			if (!uploaded)
			{
				program.setAttributeVertexPointer(location, size, type, normalized, stride, ptr);
				uploaded = true;
			}
		}
		else
		{
			GLES20.glDisableVertexAttribArray(location);
		}
	}

	public void setValues(int s, int t, int st, java.nio.Buffer p)
	{
		size = s;
		type = t;
		stride = st;
		ptr = p;
		normalized = false;
		uploaded = false;
	}
	
	public void setSize(int s)
	{
		size = s;
	}

	public void setType(int t)
	{
		type = t;
	}

	public void setNormalized(boolean n)
	{
		normalized = n;
	}
	
	public void setStride(int s)
	{
		stride = s;
	}
	
	public void setPointer(java.nio.Buffer p)
	{
		ptr = p;
	}
}