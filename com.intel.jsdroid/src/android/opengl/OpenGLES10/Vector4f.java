package android.opengl.OpenGLES10;

public class Vector4f
{
    public float[] v = new float[4];
    
	public Vector4f()
	{
		v[0] = 0;
		v[1] = 0;
		v[2] = 0;
		v[3] = 0;
	}

	public Vector4f(float x, float y, float z, float w)
	{
		v[0] = x;
		v[1] = y;
		v[2] = z;
		v[3] = w;
	}

	public Vector4f(float[] a)
	{
		v[0] = a[0];
		v[1] = a[1];
		v[2] = a[2];
		v[3] = a[3];
	}

	public Vector4f(Vector4f other)
	{
		v[0] = other.v[0];
		v[1] = other.v[1];
		v[2] = other.v[2];
		v[3] = other.v[3];
	}

	public Vector4f copyFrom(Vector4f other)
	{
		v[0] = other.v[0];
		v[1] = other.v[1];
		v[2] = other.v[2];
		v[3] = other.v[3];

		return this;
	}

	public boolean equalsfloato (Vector4f other)
	{
		for (int i = 0; i < 4; i++)
		{
			if (v[i] != other.v[i])
			{
				return false;
			}
		}

		return true;
	}

	public boolean notEqualsfloato (Vector4f other)
	{

		return !equalsfloato(other);
	}

	public float getItem (int i)
	{
		return v[i];
	}
}