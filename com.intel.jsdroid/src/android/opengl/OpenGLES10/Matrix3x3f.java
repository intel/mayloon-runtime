package android.opengl.OpenGLES10;

// Column-major order
public class Matrix3x3f
{
    public float[] m = new float[9];

	public Matrix3x3f()
	{
		for (int i = 0; i < 9; i++)
		{
			m[i] = 0;
		}
	}

	public Matrix3x3f(Matrix3x3f other)
	{
		for (int i = 0; i < 9; i++)
		{
			m[i] = other.m[i];
		}
	}

	public Matrix3x3f copyFrom (Matrix3x3f other)
	{
		for (int i = 0; i < 9; i++)
		{
			m[i] = other.m[i];
		}

		return this;
	}

	float getItem(int i, int j)
	{
		return m[i * 3 + j];
	}

	public boolean equalsTo (Matrix3x3f other)
	{
		for (int i = 0; i < 9; i++)
		{
			if (m[i] != other.m[i])
			{
				return false;
			}
		}

		return true;
	}

	public boolean notEqualsTo (Matrix3x3f other)
	{
		return !equalsTo(other);
	}
}