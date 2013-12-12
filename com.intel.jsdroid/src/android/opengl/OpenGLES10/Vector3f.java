package android.opengl.OpenGLES10;

	public class Vector3f
	{
        public float[] v = new float[9];
		public Vector3f()
		{
			v[0] = 0;
			v[1] = 0;
			v[2] = 0;
		}

		public Vector3f(float x, float y, float z)
		{
			v[0] = x;
			v[1] = y;
			v[2] = z;
		}

		public Vector3f(float[] a)
		{
			v[0] = a[0];
			v[1] = a[1];
			v[2] = a[2];
		}

		public Vector3f(Vector3f other)
		{
			v[0] = other.v[0];
			v[1] = other.v[1];
			v[2] = other.v[2];
		}

		public Vector3f copyFrom(Vector3f other)
		{
			v[0] = other.v[0];
			v[1] = other.v[1];
			v[2] = other.v[2];

			return this;
		}

		public boolean equalsfloato (Vector3f other)
		{
			for (int i = 0; i < 3; i++)
			{
				if (v[i] != other.v[i])
				{
					return false;
				}
			}

			return true;
		}

		public boolean notEqualsfloato (Vector3f other)
		{

			return !equalsfloato(other);
		}

		public float getItem (int i)
		{
			return v[i];
		}

		public Vector3f unaryNegation()
		{
			return new Vector3f(-v[0], -v[1], -v[2]);
		}

		public Vector3f subtract(Vector3f vec)
		{
			return new Vector3f(v[0] - vec.v[0], v[1] - vec.v[1], v[2] - vec.v[2]);
		}

		public Vector3f add(Vector3f vec)
		{
			return new Vector3f(v[0] + vec.v[0], v[1] + vec.v[1], v[2] + vec.v[2]);
		}

		public Vector3f multiply(float s)
		{
			return new Vector3f(v[0] * s, v[1] * s, v[2] * s);
		}

	}