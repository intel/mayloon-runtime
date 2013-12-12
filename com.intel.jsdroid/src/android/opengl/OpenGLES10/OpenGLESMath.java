package android.opengl.OpenGLES10;

import android.util.Log;
import java.util.Arrays;

public class OpenGLESMath
{

	public static Matrix4x4f scale(Matrix4x4f result, float sx, float sy, float sz)
	{
		result.m[0] *= sx;
		result.m[1] *= sx;
		result.m[2] *= sx;
		result.m[3] *= sx;

		result.m[4] *= sy;
		result.m[5] *= sy;
		result.m[6] *= sy;
		result.m[7] *= sy;

		result.m[8] *= sz;
		result.m[9] *= sz;
		result.m[10] *= sz;
		result.m[11] *= sz;
		
		return result;
	}

	public static Matrix4x4f translate(Matrix4x4f result, float tx, float ty, float tz)
	{
		result.m[12] += (result.m[0] * tx + result.m[4] * ty + result.m[8] * tz);
		result.m[13] += (result.m[1] * tx + result.m[5] * ty + result.m[9] * tz);
		result.m[14] += (result.m[2] * tx + result.m[6] * ty + result.m[10] * tz);
		result.m[15] += (result.m[3] * tx + result.m[7] * ty + result.m[11] * tz);
		
		return result;
	}

	public static Matrix4x4f rotate(Matrix4x4f result, float angle, float x, float y, float z)
	{
		float sinAngle = (float) Math.sin(angle * Math.PI / 180.0f);
		float cosAngle = (float) Math.cos(angle * Math.PI / 180.0f);
		float oneMinusCos = 1.0f - cosAngle;
		float mag = (float) Math.sqrt(x * x + y * y + z * z);

		if (mag != 0.0f && mag != 1.0f)
		{
			x /= mag;
			y /= mag;
			z /= mag;
		}

		float xx = x * x;
		float yy = y * y;
		float zz = z * z;
		float xy = x * y;
		float yz = y * z;
		float zx = z * x;
		float xs = x * sinAngle;
		float ys = y * sinAngle;
		float zs = z * sinAngle;

		Matrix4x4f rotationMatrix = new Matrix4x4f();

		rotationMatrix.m[0] = (oneMinusCos * xx) + cosAngle;
		rotationMatrix.m[1] = (oneMinusCos * xy) - zs;
		rotationMatrix.m[2] = (oneMinusCos * zx) + ys;
		rotationMatrix.m[3] = 0.0f;

		rotationMatrix.m[4] = (oneMinusCos * xy) + zs;
		rotationMatrix.m[5] = (oneMinusCos * yy) + cosAngle;
		rotationMatrix.m[6] = (oneMinusCos * yz) - xs;
		rotationMatrix.m[7] = 0.0f;

		rotationMatrix.m[8] = (oneMinusCos * zx) - ys;
		rotationMatrix.m[9] = (oneMinusCos * yz) + xs;
		rotationMatrix.m[10] = (oneMinusCos * zz) + cosAngle;
		rotationMatrix.m[11] = 0.0f;

		rotationMatrix.m[12] = 0.0f;
		rotationMatrix.m[13] = 0.0f;
		rotationMatrix.m[14] = 0.0f;
		rotationMatrix.m[15] = 1.0f;

		result = multiply(rotationMatrix, result);
		
		return result;
	}

	public static Matrix4x4f frustum(Matrix4x4f result, float left, float right, float bottom, float top, float nearZ, float farZ)
	{
		float deltaX = right - left;
		float deltaY = top - bottom;
		float deltaZ = farZ - nearZ;
		Matrix4x4f frust = new Matrix4x4f();

		if ((nearZ <= 0.0f) || (farZ <= 0.0f) || (deltaX <= 0.0f) || (deltaY <= 0.0f) || (deltaZ <= 0.0f))
		{
			Log.e("OpenGLESMath", "Invalid frustrum");
			return null;
		}

		frust.m[0] = 2.0f * nearZ / deltaX;
		frust.m[1] = frust.m[2] = frust.m[3] = 0.0f;

		frust.m[5] = 2.0f * nearZ / deltaY;
		frust.m[4] = frust.m[6] = frust.m[7] = 0.0f;

		frust.m[8] = (right + left) / deltaX;
		frust.m[9] = (top + bottom) / deltaY;
		frust.m[10] = -(nearZ + farZ) / deltaZ;
		frust.m[11] = -1.0f;

		frust.m[14] = -2.0f * nearZ * farZ / deltaZ;
		frust.m[12] = frust.m[13] = frust.m[15] = 0.0f;

		result = multiply(frust, result);
		return result;
	}

	public static Matrix4x4f perspective(Matrix4x4f result, float fovy, float aspect, float nearZ, float farZ)
	{
		float frustumHeight = (float) (Math.tan(fovy / 360 * Math.PI) * nearZ);
		float frustumWidth = frustumHeight * aspect;

		result = frustum(result, -frustumWidth, frustumWidth, -frustumHeight, frustumHeight, nearZ, farZ);
		return result;
	}

	public static Matrix4x4f ortho(Matrix4x4f result, float left, float right, float bottom, float top, float nearZ, float farZ)
	{
		float deltaX = right - left;
		float deltaY = top - bottom;
		float deltaZ = farZ - nearZ;
		Matrix4x4f ortho = new Matrix4x4f();

		if ((deltaX == 0) || (deltaY == 0) || (deltaZ == 0))
		{
			Log.e("OpenGLESMath", "Invalid ortho");
			return null;
		}

		ortho = loadIdentity(ortho);
		ortho.m[0] = 2 / deltaX;
		ortho.m[12] = -(right + left) / deltaX;
		ortho.m[5] = 2 / deltaY;
		ortho.m[13] = -(top + bottom) / deltaY;
		ortho.m[10] = -2 / deltaZ;
		ortho.m[14] = -(nearZ + farZ) / deltaZ;

		result = multiply(ortho, result);
		return result;
	}

	public static Matrix4x4f multiply(Matrix4x4f srcA, final Matrix4x4f srcB)
	{
		Matrix4x4f tmp = new Matrix4x4f();

		for (int i = 0; i < 4; i++)
		{
			int a = 4 * i;
			int b = a + 1;
			int c = a + 2;
			int d = a + 3;

			tmp.m[a] = srcA.m[a] * srcB.m[0] + srcA.m[b] * srcB.m[4] + srcA.m[c] * srcB.m[8] + srcA.m[d] * srcB.m[12];

			tmp.m[b] = srcA.m[a] * srcB.m[1] + srcA.m[b] * srcB.m[5] + srcA.m[c] * srcB.m[9] + srcA.m[d] * srcB.m[13];

			tmp.m[c] = srcA.m[a] * srcB.m[2] + srcA.m[b] * srcB.m[6] + srcA.m[c] * srcB.m[10] + srcA.m[d] * srcB.m[14];

			tmp.m[d] = srcA.m[a] * srcB.m[3] + srcA.m[b] * srcB.m[7] + srcA.m[c] * srcB.m[11] + srcA.m[d] * srcB.m[15];
		}

		return tmp;
	}

	public static Matrix4x4f multiply(Matrix4x4f srcA, float[] srcB)
	{
		Matrix4x4f result = new Matrix4x4f();

		for (int i = 0; i < 4; i++)
		{
			int a = 4 * i;
			int b = a + 1;
			int c = a + 2;
			int d = a + 3;

			result.m[a] = srcA.m[a] * srcB[0] + srcA.m[b] * srcB[4] + srcA.m[c] * srcB[8] + srcA.m[d] * srcB[12];

			result.m[b] = srcA.m[a] * srcB[1] + srcA.m[b] * srcB[5] + srcA.m[c] * srcB[9] + srcA.m[d] * srcB[13];

			result.m[c] = srcA.m[a] * srcB[2] + srcA.m[b] * srcB[6] + srcA.m[c] * srcB[10] + srcA.m[d] * srcB[14];

			result.m[d] = srcA.m[a] * srcB[3] + srcA.m[b] * srcB[7] + srcA.m[c] * srcB[11] + srcA.m[d] * srcB[15];
		}

		return result;
	}

	public static Matrix3x3f multiply(Matrix3x3f srcA, Matrix3x3f srcB)
	{
	    Matrix3x3f result = new Matrix3x3f();

		for (int i = 0; i < 3; i++)
		{
			int a = 3 * i;
			int b = a + 1;
			int c = a + 2;

			result.m[a] = srcA.m[a] * srcB.m[0] + srcA.m[b] * srcB.m[3] + srcA.m[c] * srcB.m[6];

			result.m[b] = srcA.m[a] * srcB.m[1] + srcA.m[b] * srcB.m[4] + srcA.m[c] * srcB.m[7];

			result.m[c] = srcA.m[a] * srcB.m[2] + srcA.m[b] * srcB.m[5] + srcA.m[c] * srcB.m[8];
		}

		return result;
	}

	public static Vector3f multiply(Matrix3x3f srcA, Vector3f srcB)
	{
		Vector3f tmp = new Vector3f();
		for (int i = 0; i < 3; i++)
		{
			tmp.v[i] = srcA.m[i] * srcB.v[0] + srcA.m[i + 3] * srcB.v[1] + srcA.m[i + 6] * srcB.v[2];
		}
		return tmp;
	}

	public static Vector4f multiply(Matrix4x4f srcA, Vector4f srcB)
	{
		Vector4f tmp = new Vector4f();
		for (int i = 0; i < 4; i++)
		{
			tmp.v[i] = srcA.m[i] * srcB.v[0] + srcA.m[i + 4] * srcB.v[1] + srcA.m[i + 8] * srcB.v[2] + srcA.m[i + 12] * srcB.v[3];
		}
		return tmp;
	}

	public static Matrix3x3f loadIdentity(Matrix3x3f src)
	{
	    Matrix3x3f result = new Matrix3x3f();
		result.m[0] = 1;
		result.m[4] = 1;
		result.m[8] = 1;
		return result;
	}

	public static Matrix4x4f loadIdentity(Matrix4x4f src)
	{
	    Matrix4x4f result = new Matrix4x4f();
		result.m[0] = 1;
		result.m[5] = 1;
		result.m[10] = 1;
		result.m[15] = 1;
		return result;
	}

	public static Matrix3x3f transpose(Matrix3x3f result)
	{
		Matrix3x3f tmp = new Matrix3x3f();

		tmp.m[0] = result.m[0];
		tmp.m[1] = result.m[3];
		tmp.m[2] = result.m[6];
		tmp.m[3] = result.m[1];
		tmp.m[4] = result.m[4];
		tmp.m[5] = result.m[7];
		tmp.m[6] = result.m[2];
		tmp.m[7] = result.m[5];
		tmp.m[8] = result.m[8];

		return tmp;
	}

	public static Matrix4x4f transpose(Matrix4x4f result)
	{
		Matrix4x4f tmp = new Matrix4x4f();

		tmp.m[0] = result.m[0];
		tmp.m[1] = result.m[4];
		tmp.m[2] = result.m[8];
		tmp.m[3] = result.m[12];
		tmp.m[4] = result.m[1];
		tmp.m[5] = result.m[5];
		tmp.m[6] = result.m[9];
		tmp.m[7] = result.m[13];
		tmp.m[8] = result.m[2];
		tmp.m[9] = result.m[6];
		tmp.m[10] = result.m[10];
		tmp.m[11] = result.m[14];
		tmp.m[12] = result.m[3];
		tmp.m[13] = result.m[7];
		tmp.m[14] = result.m[11];
		tmp.m[15] = result.m[15];

		return tmp;
	}

	public static Matrix3x3f adjoint(Matrix3x3f src)
	{
	    Matrix3x3f result = new Matrix3x3f();
		float a1 = src.m[0];
		float a2 = src.m[3];
		float a3 = src.m[6];

		float b1 = src.m[1];
		float b2 = src.m[4];
		float b3 = src.m[7];

		float c1 = src.m[2];
		float c2 = src.m[5];
		float c3 = src.m[8];

		result.m[0] = (b2 * c3 - b3 * c2);
		result.m[3] = (a3 * c2 - a2 * c3);
		result.m[6] = (a2 * b3 - a3 * b2);

		result.m[1] = (b3 * c1 - b1 * c3);
		result.m[4] = (a1 * c3 - a3 * c1);
		result.m[7] = (a3 * b1 - a1 * b3);

		result.m[2] = (b1 * c2 - b2 * c1);
		result.m[5] = (a2 * c1 - a1 * c2);
		result.m[8] = (a1 * b2 - a2 * b1);
		
		return result;
	}

	public static Matrix3x3f inverse(Matrix3x3f src)
	{
	    Matrix3x3f result = new Matrix3x3f();
		float a1 = src.m[0];
		float a2 = src.m[3];
		float a3 = src.m[6];

		float b1 = src.m[1];
		float b2 = src.m[4];
		float b3 = src.m[7];

		float c1 = src.m[2];
		float c2 = src.m[5];
		float c3 = src.m[8];

		float det = (a1 * (b2 * c3 - b3 * c2) + a2 * (b3 * c1 - b1 * c3) + a3 * (b1 * c2 - b2 * c1));

		result.m[0] = (b2 * c3 - b3 * c2) / det;
		result.m[3] = (a3 * c2 - a2 * c3) / det;
		result.m[6] = (a2 * b3 - a3 * b2) / det;

		result.m[1] = (b3 * c1 - b1 * c3) / det;
		result.m[4] = (a1 * c3 - a3 * c1) / det;
		result.m[7] = (a3 * b1 - a1 * b3) / det;

		result.m[2] = (b1 * c2 - b2 * c1) / det;
		result.m[5] = (a2 * c1 - a1 * c2) / det;
		result.m[8] = (a1 * b2 - a2 * b1) / det;

		return result;
	}

	public static Matrix4x4f inverse(Matrix4x4f src)
	{
		int swap;
		float t = 0;
		float[][] temp = new float[4][4];
		Matrix4x4f result = new Matrix4x4f();

		for (int i = 0; i < 4; i++)
		{
			for (int j = 0; j < 4; j++)
			{
			    temp[i][j] = src.getItem(i, j);
			}
		}

		result = loadIdentity(result);

		for (int i = 0; i < 4; i++)
		{
			swap = i;
			for (int j = i + 1; j < 4; j++)
			{
				if (Math.abs(temp[j][i]) > Math.abs(temp[i][i]))
				{
					swap = j;
				}
			}

			if (swap != i)
			{
				for (int k = 0; k < 4; k++)
				{
					t = temp[i][k];
					temp[i][k] = temp[swap][k];
					temp[swap][k] = t;

					t = result.getItem(i, k);
					result.setItem(i, k, result.getItem(swap, k));
					result.setItem(swap, k, t);
				}
			}
			
			if (temp[i][i] == 0)
			{
				Log.d("OpenGLESMath",  "ERROR: Matrix is singular, cannot invert.");
				return null;
			}
			
			t = temp[i][i];
			for (int k = 0; k < 4; k++)
			{
				temp[i][k] /= t;
			    result.setItem(i, k, result.getItem(i, k)/t);
			}
			
			for (int j = 0; j < 4; j++)
			{
				if (j != i)
				{
					t = temp[j][i];
					for (int k = 0; k < 4; k++)
					{
						temp[j][k] -= temp[i][k] * t;
						float tmp = result.getItem(j, k) - result.getItem(i, k) * t;
					    result.setItem(j, k, tmp);
					}
				}
			}
		}
		
		return result;
	}

	public static Matrix3x3f copyMatrix4x4UpperLeftToMatrix3x3(Matrix4x4f mat)
	{
	    Matrix3x3f result = new Matrix3x3f();
		result.m[0] = mat.m[0];
		result.m[1] = mat.m[1];
		result.m[2] = mat.m[2];

		result.m[3] = mat.m[4];
		result.m[4] = mat.m[5];
		result.m[5] = mat.m[6];

		result.m[6] = mat.m[8];
		result.m[7] = mat.m[9];
		result.m[8] = mat.m[10];
		return result;
	}

	public static boolean isUnitVector(Vector4f vec)
	{
		double length = Math.sqrt(vec.v[0] * vec.v[0] + vec.v[1] * vec.v[1] + vec.v[2] * vec.v[2] + vec.v[3] * vec.v[3]);
		float epsilon = 0.01f;
		return 1.0f - epsilon <= length && length <= 1.0f + epsilon;
	}

	public static float clamp(float t, float minV, float maxV)
	{
		return Math.max(Math.min(maxV, t), minV);
	}
}
