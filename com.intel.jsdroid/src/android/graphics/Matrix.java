package android.graphics;

import java.io.PrintWriter;
import java.util.Arrays;

import android.util.MathUtils;


/**
 * The Matrix class holds a 3x3 matrix for transforming coordinates.
 * Matrix does not have a constructor, so it must be explicitly initialized
 * using either reset() - to construct an identity matrix, or one of the set..()
 * functions (e.g. setTranslate, setRotate, etc.).
 */
public class Matrix {

    public static final int MSCALE_X = 0;   //!< use with getValues/setValues
    public static final int MSKEW_X  = 1;   //!< use with getValues/setValues
    public static final int MTRANS_X = 2;   //!< use with getValues/setValues
    public static final int MSKEW_Y  = 3;   //!< use with getValues/setValues
    public static final int MSCALE_Y = 4;   //!< use with getValues/setValues
    public static final int MTRANS_Y = 5;   //!< use with getValues/setValues
    public static final int MPERSP_0 = 6;   //!< use with getValues/setValues
    public static final int MPERSP_1 = 7;   //!< use with getValues/setValues
    public static final int MPERSP_2 = 8;   //!< use with getValues/setValues

    private SkMatrix mSkMatrix;

    /**
     * Create an identity matrix
     */
    public Matrix() {
        mSkMatrix = native_create(null);
    }

    /**
     * Create a matrix that is a (deep) copy of src
     * @param src The matrix to copy into this matrix
     */
    public Matrix(Matrix src) {
        mSkMatrix = native_create(src.mSkMatrix);
    }

    /**
     * Returns true if the matrix is identity.
     * This maybe faster than testing if (getType() == 0)
     */
    public boolean isIdentity() {
        return mSkMatrix.isIdentity();
    }

    /**
     * Returns true if will map a rectangle to another rectangle. This can be
     * true if the matrix is identity, scale-only, or rotates a multiple of 90
     * degrees.
     */
    public boolean rectStaysRect() {
        return mSkMatrix.rectStaysRect();
    }

    /**
     * (deep) copy the src matrix into this matrix. If src is null, reset this
     * matrix to the identity matrix.
     */
    public void set(Matrix src) {
        if (src == null) {
            reset();
        } else {
            mSkMatrix.fTypeMask = src.mSkMatrix.fTypeMask;
            System.arraycopy(src.mSkMatrix.fMat, 0, mSkMatrix.fMat, 0, src.mSkMatrix.fMat.length);
        }
    }
    
    /** Returns true iff obj is a Matrix and its values equal our values.
    */
    public boolean equals(Object obj) {
        return obj != null &&
               obj instanceof Matrix && Arrays.equals(mSkMatrix.fMat, ((Matrix)obj).mSkMatrix.fMat);
    }

    /** Set the matrix to identity */
    public void reset() {
        mSkMatrix.reset();
    }

    /** Set the matrix to translate by (dx, dy). */
    public void setTranslate(float dx, float dy) {
        mSkMatrix.setTranslate(dx, dy);
    }

    /**
     * Set the matrix to scale by sx and sy, with a pivot point at (px, py).
     * The pivot point is the coordinate that should remain unchanged by the
     * specified transformation.
     */
    public void setScale(float sx, float sy, float px, float py) {
        mSkMatrix.setScale(sx, sy, px, py);
    }

    /** Set the matrix to scale by sx and sy. */
    public void setScale(float sx, float sy) {
        mSkMatrix.setScale(sx, sy);
    }

    /**
     * Set the matrix to rotate by the specified number of degrees, with a pivot
     * point at (px, py). The pivot point is the coordinate that should remain
     * unchanged by the specified transformation.
     */
    public void setRotate(float degrees, float px, float py) {
        mSkMatrix.setRotate(degrees, px, py);
    }

    /**
     * Set the matrix to rotate about (0,0) by the specified number of degrees.
     */
    public void setRotate(float degrees) {
        mSkMatrix.setRotate(degrees);
    }

    /**
     * Set the matrix to rotate by the specified sine and cosine values, with a
     * pivot point at (px, py). The pivot point is the coordinate that should
     * remain unchanged by the specified transformation.
     */
    public void setSinCos(float sinValue, float cosValue, float px, float py) {
        mSkMatrix.setSinCos(sinValue, cosValue, px, py);
    }

    /** Set the matrix to rotate by the specified sine and cosine values. */
    public void setSinCos(float sinValue, float cosValue) {
        mSkMatrix.setSinCos(sinValue, cosValue);
    }

    /**
     * Set the matrix to skew by sx and sy, with a pivot point at (px, py).
     * The pivot point is the coordinate that should remain unchanged by the
     * specified transformation.
     */
    public void setSkew(float kx, float ky, float px, float py) {
        mSkMatrix.setSkew(kx, ky, px, py);
    }

    /** Set the matrix to skew by sx and sy. */
    public void setSkew(float kx, float ky) {
        mSkMatrix.setSkew(kx, ky);
    }

    /**
     * Set the matrix to the concatenation of the two specified matrices,
     * returning true if the the result can be represented. Either of the two
     * matrices may also be the target matrix. this = a * b
     */
    public boolean setConcat(Matrix a, Matrix b) {
        return mSkMatrix.setConcat(a.mSkMatrix, b.mSkMatrix);
    }

    /**
     * Preconcats the matrix with the specified translation.
     * M' = M * T(dx, dy)
     */
    public boolean preTranslate(float dx, float dy) {
        return mSkMatrix.preTranslate(dx, dy);
    }

    /**
     * Preconcats the matrix with the specified scale.
     * M' = M * S(sx, sy, px, py)
     */
    public boolean preScale(float sx, float sy, float px, float py) {
        return mSkMatrix.preScale(sx, sy, px, py);
    }

    /**
     * Preconcats the matrix with the specified scale.
     * M' = M * S(sx, sy)
     */
    public boolean preScale(float sx, float sy) {
        return mSkMatrix.preScale(sx, sy);
    }

    /**
     * Preconcats the matrix with the specified rotation.
     * M' = M * R(degrees, px, py)
     */
    public boolean preRotate(float degrees, float px, float py) {
        return mSkMatrix.preRotate(degrees, px, py);
    }

    /**
     * Preconcats the matrix with the specified rotation.
     * M' = M * R(degrees)
     */
    public boolean preRotate(float degrees) {
        return mSkMatrix.preRotate(degrees);
    }

    /**
     * Preconcats the matrix with the specified skew.
     * M' = M * K(kx, ky, px, py)
     */
    public boolean preSkew(float kx, float ky, float px, float py) {
        return mSkMatrix.preSkew(kx, ky, px, py);
    }

    /**
     * Preconcats the matrix with the specified skew.
     * M' = M * K(kx, ky)
     */
    public boolean preSkew(float kx, float ky) {
        return mSkMatrix.preSkew(kx, ky);
    }

    /**
     * Preconcats the matrix with the specified matrix.
     * M' = M * other
     */
    public boolean preConcat(Matrix other) {
        return mSkMatrix.preConcat(other.mSkMatrix);
    }

    /**
     * Postconcats the matrix with the specified translation.
     * M' = T(dx, dy) * M
     */
    public boolean postTranslate(float dx, float dy) {
        return mSkMatrix.postTranslate(dx, dy);
    }

    /**
     * Postconcats the matrix with the specified scale.
     * M' = S(sx, sy, px, py) * M
     */
    public boolean postScale(float sx, float sy, float px, float py) {
        return mSkMatrix.postScale(sx, sy, px, py);
    }

    /**
     * Postconcats the matrix with the specified scale.
     * M' = S(sx, sy) * M
     */
    public boolean postScale(float sx, float sy) {
        return mSkMatrix.postScale(sx, sy);
    }

    /**
     * Postconcats the matrix with the specified rotation.
     * M' = R(degrees, px, py) * M
     */
    public boolean postRotate(float degrees, float px, float py) {
        return mSkMatrix.postRotate(degrees, px, py);
    }

    /**
     * Postconcats the matrix with the specified rotation.
     * M' = R(degrees) * M
     */
    public boolean postRotate(float degrees) {
        return mSkMatrix.postRotate(degrees);
    }

    /**
     * Postconcats the matrix with the specified skew.
     * M' = K(kx, ky, px, py) * M
     */
    public boolean postSkew(float kx, float ky, float px, float py) {
        return mSkMatrix.postSkew(kx, ky, px, py);
    }

    /**
     * Postconcats the matrix with the specified skew.
     * M' = K(kx, ky) * M
     */
    public boolean postSkew(float kx, float ky) {
        return mSkMatrix.postSkew(kx, ky);
    }

    /**
     * Postconcats the matrix with the specified matrix.
     * M' = other * M
     */
    public boolean postConcat(Matrix other) {
        return mSkMatrix.postConcat(other.mSkMatrix);
    }

    /**
     * Controlls how the src rect should align into the dst rect for
     * setRectToRect().
     */
    public enum ScaleToFit {
        /**
         * Scale in X and Y independently, so that src matches dst exactly. This
         * may change the aspect ratio of the src.
         */
        FILL(0),
        /**
         * Compute a scale that will maintain the original src aspect ratio, but
         * will also ensure that src fits entirely inside dst. At least one axis
         * (X or Y) will fit exactly. START aligns the result to the left and
         * top edges of dst.
         */
        START(1),
        /**
         * Compute a scale that will maintain the original src aspect ratio, but
         * will also ensure that src fits entirely inside dst. At least one axis
         * (X or Y) will fit exactly. The result is centered inside dst.
         */
        CENTER(2),
        /**
         * Compute a scale that will maintain the original src aspect ratio, but
         * will also ensure that src fits entirely inside dst. At least one axis
         * (X or Y) will fit exactly. END aligns the result to the right and
         * bottom edges of dst.
         */
        END(3);

        // the native values must match those in SkMatrix.h
        ScaleToFit(int nativeInt) {
            this.nativeInt = nativeInt;
        }

        final int nativeInt;
    }

    /**
     * Set the matrix to the scale and translate values that map the source
     * rectangle to the destination rectangle, returning true if the the result
     * can be represented.
     *
     * @param src the source rectangle to map from.
     * @param dst the destination rectangle to map to.
     * @param stf the ScaleToFit option
     * @return true if the matrix can be represented by the rectangle mapping.
     */
    public boolean setRectToRect(RectF src, RectF dst, ScaleToFit stf) {
        if (dst == null || src == null) {
            throw new NullPointerException();
        }
        return mSkMatrix.setRectToRect(src, dst, stf.nativeInt);
    }
    
    // private helper to perform range checks on arrays of "points"
    private static void checkPointArrays(float[] src, int srcIndex,
                                         float[] dst, int dstIndex,
                                         int pointCount) {
        // check for too-small and too-big indices
        int srcStop = srcIndex + (pointCount << 1);
        int dstStop = dstIndex + (pointCount << 1);
        if ((pointCount | srcIndex | dstIndex | srcStop | dstStop) < 0 ||
                srcStop > src.length || dstStop > dst.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Set the matrix such that the specified src points would map to the
     * specified dst points. The "points" are represented as an array of floats,
     * order [x0, y0, x1, y1, ...], where each "point" is 2 float values.
     *
     * @param src   The array of src [x,y] pairs (points)
     * @param srcIndex Index of the first pair of src values
     * @param dst   The array of dst [x,y] pairs (points)
     * @param dstIndex Index of the first pair of dst values
     * @param pointCount The number of pairs/points to be used. Must be [0..4]
     * @return true if the matrix was set to the specified transformation
     */
    public boolean setPolyToPoly(float[] src, int srcIndex,
                                 float[] dst, int dstIndex,
                                 int pointCount) {
        if (pointCount > 4) {
            throw new IllegalArgumentException();
        }
        checkPointArrays(src, srcIndex, dst, dstIndex, pointCount);
        float[] tmpSrc = Arrays.copyOfRange(src, srcIndex, src.length);
        float[] tmpDst = Arrays.copyOfRange(dst, dstIndex, dst.length);
        boolean result = mSkMatrix.setPolyToPoly(tmpSrc, tmpDst, pointCount);
        System.arraycopy(tmpSrc, 0, src, srcIndex, tmpSrc.length);
        System.arraycopy(tmpDst, 0, dst, dstIndex, tmpDst.length);
        return result;
    }

    /**
     * If this matrix can be inverted, return true and if inverse is not null,
     * set inverse to be the inverse of this matrix. If this matrix cannot be
     * inverted, ignore inverse and return false.
     */
    public boolean invert(Matrix inverse) {
        return mSkMatrix.invert(inverse.mSkMatrix);
    }

    /**
    * Apply this matrix to the array of 2D points specified by src, and write
     * the transformed points into the array of points specified by dst. The
     * two arrays represent their "points" as pairs of floats [x, y].
     *
     * @param dst   The array of dst points (x,y pairs)
     * @param dstIndex The index of the first [x,y] pair of dst floats
     * @param src   The array of src points (x,y pairs)
     * @param srcIndex The index of the first [x,y] pair of src floats
     * @param pointCount The number of points (x,y pairs) to transform
     */
    public void mapPoints(float[] dst, int dstIndex, float[] src, int srcIndex,
                          int pointCount) {
        checkPointArrays(src, srcIndex, dst, dstIndex, pointCount);
        float[] tmpSrc = Arrays.copyOfRange(src, srcIndex, src.length - 1);
        float[] tmpDst = Arrays.copyOfRange(dst, dstIndex, dst.length - 1);
        mSkMatrix.mapPoints(tmpDst, tmpSrc, pointCount);

        System.arraycopy(tmpSrc, 0, src, srcIndex, tmpSrc.length);
        System.arraycopy(tmpDst, 0, dst, dstIndex, tmpDst.length); 
    }
    
    /**
    * Apply this matrix to the array of 2D vectors specified by src, and write
     * the transformed vectors into the array of vectors specified by dst. The
     * two arrays represent their "vectors" as pairs of floats [x, y].
     *
     * @param dst   The array of dst vectors (x,y pairs)
     * @param dstIndex The index of the first [x,y] pair of dst floats
     * @param src   The array of src vectors (x,y pairs)
     * @param srcIndex The index of the first [x,y] pair of src floats
     * @param vectorCount The number of vectors (x,y pairs) to transform
     */
    public void mapVectors(float[] dst, int dstIndex, float[] src, int srcIndex,
                          int vectorCount) {
        checkPointArrays(src, srcIndex, dst, dstIndex, vectorCount);
        float[] tmpSrc = Arrays.copyOfRange(src, srcIndex, src.length - 1);
        float[] tmpDst = Arrays.copyOfRange(dst, dstIndex, dst.length - 1);
        mSkMatrix.mapVectors(dst, src, vectorCount);

        System.arraycopy(tmpSrc, 0, src, srcIndex, tmpSrc.length);
        System.arraycopy(tmpDst, 0, dst, dstIndex, tmpDst.length);
    }
    
    /**
     * Apply this matrix to the array of 2D points specified by src, and write
     * the transformed points into the array of points specified by dst. The
     * two arrays represent their "points" as pairs of floats [x, y].
     *
     * @param dst   The array of dst points (x,y pairs)
     * @param src   The array of src points (x,y pairs)
     */
    public void mapPoints(float[] dst, float[] src) {
        if (dst.length != src.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mapPoints(dst, 0, src, 0, dst.length >> 1);
    }

    /**
     * Apply this matrix to the array of 2D vectors specified by src, and write
     * the transformed vectors into the array of vectors specified by dst. The
     * two arrays represent their "vectors" as pairs of floats [x, y].
     *
     * @param dst   The array of dst vectors (x,y pairs)
     * @param src   The array of src vectors (x,y pairs)
     */
    public void mapVectors(float[] dst, float[] src) {
        if (dst.length != src.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mapVectors(dst, 0, src, 0, dst.length >> 1);
    }

    /**
     * Apply this matrix to the array of 2D points, and write the transformed
     * points back into the array
     *
     * @param pts The array [x0, y0, x1, y1, ...] of points to transform.
     */
    public void mapPoints(float[] pts) {
        mapPoints(pts, 0, pts, 0, pts.length >> 1);
    }

    /**
     * Apply this matrix to the array of 2D vectors, and write the transformed
     * vectors back into the array.
     * @param vecs The array [x0, y0, x1, y1, ...] of vectors to transform.
     */
    public void mapVectors(float[] vecs) {
        mapVectors(vecs, 0, vecs, 0, vecs.length >> 1);
    }

    /**
     * Apply this matrix to the src rectangle, and write the transformed
     * rectangle into dst. This is accomplished by transforming the 4 corners of
     * src, and then setting dst to the bounds of those points.
     *
     * @param dst Where the transformed rectangle is written.
     * @param src The original rectangle to be transformed.
     * @return the result of calling rectStaysRect()
     */
    public boolean mapRect(RectF dst, RectF src) {
        if (dst == null || src == null) {
            throw new NullPointerException();
        }
        return mSkMatrix.mapRect(dst, src);
    }

    /**
     * Apply this matrix to the rectangle, and write the transformed rectangle
     * back into it. This is accomplished by transforming the 4 corners of rect,
     * and then setting it to the bounds of those points
     *
     * @param rect The rectangle to transform.
     * @return the result of calling rectStaysRect()
     */
    public boolean mapRect(RectF rect) {
        return mapRect(rect, rect);
    }

    /**
     * Return the mean radius of a circle after it has been mapped by
     * this matrix. NOTE: in perspective this value assumes the circle
     * has its center at the origin.
     */
    public float mapRadius(float radius) {
        return mSkMatrix.mapRadius(radius);
    }
    
    /** Copy 9 values from the matrix into the array.
    */
    public void getValues(float[] values) {
        if (values.length < 9) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mSkMatrix.getValues(values);
    }

    /** Copy 9 values from the array into the matrix.
        Depending on the implementation of Matrix, these may be
        transformed into 16.16 integers in the Matrix, such that
        a subsequent call to getValues() will not yield exactly
        the same values.
    */
    public void setValues(float[] values) {
        if (values.length < 9) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mSkMatrix.setValues(values);
    }
    
    public int getType() {
        return mSkMatrix.getType();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("Matrix{");
        toShortString(sb);
        sb.append('}');
        return sb.toString();
                
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder(64);
        toShortString(sb);
        return sb.toString();
    }

    /**
     * @hide
     */
    public void toShortString(StringBuilder sb) {
        float[] values = new float[9];
        getValues(values);
        sb.append('[');
        sb.append(values[0]); sb.append(", "); sb.append(values[1]); sb.append(", ");
        sb.append(values[2]); sb.append("][");
        sb.append(values[3]); sb.append(", "); sb.append(values[4]); sb.append(", ");
        sb.append(values[5]); sb.append("][");
        sb.append(values[6]); sb.append(", "); sb.append(values[7]); sb.append(", ");
        sb.append(values[8]); sb.append(']');
    }

    /**
     * Print short string, to optimize dumping.
     * @hide
     */
    public void printShortString(PrintWriter pw) {
        float[] values = new float[9];
        getValues(values);
        pw.print('[');
        pw.print(values[0]); pw.print(", "); pw.print(values[1]); pw.print(", ");
                pw.print(values[2]); pw.print("][");
        pw.print(values[3]); pw.print(", "); pw.print(values[4]); pw.print(", ");
                pw.print(values[5]); pw.print("][");
        pw.print(values[6]); pw.print(", "); pw.print(values[7]); pw.print(", ");
                pw.print(values[8]); pw.print(']');
                
    }

    protected void finalize() throws Throwable {
        finalizer(mSkMatrix);
    }
    
    /*package*/ final SkMatrix ni() {
        return mSkMatrix;
    }

    private static SkMatrix native_create(final SkMatrix matrix) {
        return new SkMatrix(matrix);
    }
    
    private static void finalizer(final SkMatrix matrix) {}
    
    private static class SkMatrix {
    	
        static final int kIdentity_Mask      = 0;
        static final int kTranslate_Mask     = 0x01;  //!< set if the matrix has translation
        static final int kScale_Mask         = 0x02;  //!< set if the matrix has X or Y scale
        static final int kAffine_Mask        = 0x04;  //!< set if the matrix skews or rotates
        static final int kPerspective_Mask   = 0x08;  //!< set if the matrix is in perspective
        static final int kRectStaysRect_Mask = 0x10;
        static final int kUnknown_Mask       = 0x80;
        static final int kAllMasks           = kTranslate_Mask | kScale_Mask | kAffine_Mask | 
        									   kPerspective_Mask | kRectStaysRect_Mask;
    	
        static final int kMScaleX            = 0;
        static final int kMSkewX             = 1;
        static final int kMTransX            = 2;
        static final int kMSkewY             = 3;
        static final int kMScaleY            = 4;
        static final int kMTransY            = 5;
        static final int kMPersp0            = 6;
        static final int kMPersp1            = 7;
        static final int kMPersp2            = 8;
        float[] fMat = new float[9];
        
        static final float SK_Scalar1        = 1.0f;
        static final float kMatrix22Elem     = SK_Scalar1;
        static final float tolerance         = SK_Scalar1 / (1<<12);
        static final float kPersp1Int          = 1.0f;
        static final float kScalar1Int          = 1.0f;
        
     // this guy aligns with the masks, so we can compute a mask from a varaible 0/1
        static final int kTranslate_Shift     = 0;
        static final int kScale_Shift         = 1;
        static final int kAffine_Shift        = 2;
        static final int kPerspective_Shift   = 3;
        static final int kRectStaysRect_Shift = 4;
 
        /**
         * Scale in X and Y independently, so that src matches dst exactly.
         * This may change the aspect ratio of the src.
         */
        static final int kFill_ScaleToFit	= 0;
        /**
         * Compute a scale that will maintain the original src aspect ratio,
         * but will also ensure that src fits entirely inside dst. At least one
         * axis (X or Y) will fit exactly. kStart aligns the result to the
         * left and top edges of dst.
         */
        static final int kStart_ScaleToFit 	= 1;
        /**
         * Compute a scale that will maintain the original src aspect ratio,
         * but will also ensure that src fits entirely inside dst. At least one
         * axis (X or Y) will fit exactly. The result is centered inside dst.
         */
		static final int kCenter_ScaleToFit	= 2;
        /**
         * Compute a scale that will maintain the original src aspect ratio,
         * but will also ensure that src fits entirely inside dst. At least one
         * axis (X or Y) will fit exactly. kEnd aligns the result to the
         * right and bottom edges of dst.
         */
	static final int kEnd_ScaleToFit		= 3;
        
    	int fTypeMask;
    	
        public int getType() {
            if ((fTypeMask & kUnknown_Mask) != 0) {
                fTypeMask = computeTypeMask();
            }
            return (fTypeMask & 0xF);
        }
    	
        public int computeTypeMask() {
            int mask = 0;

            if (fMat[kMPersp0] != 0  | fMat[kMPersp1] != 0 |
                    (fMat[kMPersp2] - kPersp1Int) != 0) {
                mask |= kPerspective_Mask;
            }
            
            if (fMat[kMTransX] != 0 | fMat[kMTransY] != 0) {
                mask |= kTranslate_Mask;
            }
            
            if (fMat[kMSkewX] != 0 | fMat[kMSkewY] != 0) {
                mask |= kAffine_Mask;
            }

            if ((fMat[kMScaleX] - kScalar1Int) != 0 | (fMat[kMScaleY] - kScalar1Int) != 0) {
                mask |= kScale_Mask;
            }
            
            if ((mask & kPerspective_Mask) == 0) {
                // map non-zero to 1
                int m00 = fMat[kMScaleX] != 0 ? 1 : 0;
                int m01 = fMat[kMSkewX] != 0 ? 1 : 0;
                int m10 = fMat[kMSkewY] != 0 ? 1 : 0;
                int m11 = fMat[kMScaleY] != 0 ? 1 : 0;
                
                // record if the (p)rimary and (s)econdary diagonals are all 0 or
                // all non-zero (answer is 0 or 1)
                int dp0 = (m00 | m11) ^ 1;  // true if both are 0
                int dp1 = m00 & m11;        // true if both are 1
                int ds0 = (m01 | m10) ^ 1;  // true if both are 0
                int ds1 = m01 & m10;        // true if both are 1
                
                // return 1 if primary is 1 and secondary is 0 or
                // primary is 0 and secondary is 1
                mask |= ((dp0 & ds1) | (dp1 & ds0)) << kRectStaysRect_Shift;
            }

            return mask;
        }
        
    	public SkMatrix(final SkMatrix src) {
    		if (src != null) {
    			this.fTypeMask = src.fTypeMask;
    			System.arraycopy(src.fMat, 0, this.fMat, 0, this.fMat.length);
    		} else {
    		    this.reset();
    		}
    	}
    	
    	public boolean isIdentity() {
    		return this.getType() == 0;
    	}
    	
        public boolean rectStaysRect() {
            if ((fTypeMask & kUnknown_Mask) != 0) {
                fTypeMask = computeTypeMask();
            }
            return (fTypeMask & kRectStaysRect_Mask) != 0;
        }
    	
        public void reset() {
            fMat[kMScaleX] = fMat[kMScaleY] = SK_Scalar1;
            fMat[kMSkewX]  = fMat[kMSkewY] = 
            fMat[kMTransX] = fMat[kMTransY] =
            fMat[kMPersp0] = fMat[kMPersp1] = 0;
            fMat[kMPersp2] = kMatrix22Elem;
            setTypeMask(kIdentity_Mask | kRectStaysRect_Mask);
        }
        
        public boolean hasPerspective() {
            return (getType() & kPerspective_Mask) != 0;
        }
        
        public float getScaleX() { return fMat[kMScaleX]; }
        public float getScaleY() { return fMat[kMScaleY]; }
        public float getSkewY() { return fMat[kMSkewY]; }
        public float getSkewX() { return fMat[kMSkewX]; }
        public float getTranslateX() { return fMat[kMTransX]; }
        public float getTranslateY() { return fMat[kMTransY]; }
        public float getPerspX() { return fMat[kMPersp0]; }
        public float getPerspY() { return fMat[kMPersp1]; }
        
        public void set(int index, float value) {
            fMat[index] = value;
            setTypeMask(kUnknown_Mask);
        }
        
        public float get(int index) {
            return fMat[index];
        }

        public void setScaleX(float v) { set(kMScaleX, v); }
        public void setScaleY(float v) { set(kMScaleY, v); }
        public void setSkewY(float v) { set(kMSkewY, v); }
        public void setSkewX(float v) { set(kMSkewX, v); }
        public void setTranslateX(float v) { set(kMTransX, v); }
        public void setTranslateY(float v) { set(kMTransY, v); }
        public void setPerspX(float v) { set(kMPersp0, v); }
        public void setPerspY(float v) { set(kMPersp1, v); }
        
        public void setTranslate(float dx, float dy) {
        	if (dx != 0 || dy != 0) {
				fMat[kMTransX] = dx;
				fMat[kMTransY] = dy;
				
				fMat[kMScaleX] = fMat[kMScaleY] = SK_Scalar1;
			    fMat[kMSkewX]  = fMat[kMSkewY] = 
			    fMat[kMPersp0] = fMat[kMPersp1] = 0;
			    fMat[kMPersp2] = kMatrix22Elem;
			
			    setTypeMask(kTranslate_Mask | kRectStaysRect_Mask);
			} else {
				reset();
			}
        }
        
        public void setScale(float sx, float sy, float px, float py) {
            fMat[kMScaleX] = sx;
            fMat[kMScaleY] = sy;
            fMat[kMTransX] = px - sx * px;
            fMat[kMTransY] = py - sy * py;
            fMat[kMPersp2] = kMatrix22Elem;

            fMat[kMSkewX]  = fMat[kMSkewY] = 
            fMat[kMPersp0] = fMat[kMPersp1] = 0;
            
            setTypeMask(kScale_Mask | kTranslate_Mask | kRectStaysRect_Mask);
        }
        
        public void setScale(float sx, float sy) {
            fMat[kMScaleX] = sx;
            fMat[kMScaleY] = sy;
            fMat[kMPersp2] = kMatrix22Elem;

            fMat[kMTransX] = fMat[kMTransY] =
            fMat[kMSkewX]  = fMat[kMSkewY] = 
            fMat[kMPersp0] = fMat[kMPersp1] = 0;

            setTypeMask(kScale_Mask | kRectStaysRect_Mask);
        }
        
        public void setRotate(float degrees, float px, float py) {
            float sinV, cosV;
            float radians = MathUtils.radians(degrees);
            sinV = (float) Math.sin(radians);
            cosV = (float) Math.cos(radians);
            setSinCos(sinV, cosV, px, py);
        }
        
        public void setRotate(float degrees) {
            float sinV, cosV;
            float radians = MathUtils.radians(degrees);
            sinV = (float) Math.sin(radians);
            cosV = (float) Math.cos(radians);
            setSinCos(sinV, cosV);
        }
        
        private static float rowcol3(float row[], int r, float col[], int c) {
            return row[r + 0] * col[c + 0] + row[r + 1] * col[c + 3] + row[r + 2] * col[c + 6];
        }
        
        private static void normalize_perspective(float mat[]) {
            if (Math.abs(mat[kMPersp2]) > kMatrix22Elem) {
                for (int i = 0; i < 9; i++) {
                    mat[i] = 0.5f * mat[i];
                }
            }
        }

        public boolean setConcat(SkMatrix a, SkMatrix b) {
            int aType = a.getType();
            int bType = b.getType();

            if (0 == aType) {
                this.fTypeMask = b.fTypeMask;
                System.arraycopy(b.fMat, 0, this.fMat, 0, this.fMat.length);
            } else if (0 == bType) {
                this.fTypeMask = a.fTypeMask;
                System.arraycopy(a.fMat, 0, this.fMat, 0, this.fMat.length);
            } else {
                SkMatrix tmp = new SkMatrix(null);

                if (((aType | bType) & kPerspective_Mask) != 0) {
                    tmp.fMat[kMScaleX] = rowcol3(a.fMat, 0, b.fMat, 0);
                    tmp.fMat[kMSkewX] = rowcol3(a.fMat, 0, b.fMat, 1);
                    tmp.fMat[kMTransX] = rowcol3(a.fMat, 0, b.fMat, 2);
                    tmp.fMat[kMSkewY] = rowcol3(a.fMat, 3, b.fMat, 0);
                    tmp.fMat[kMScaleY] = rowcol3(a.fMat, 3, b.fMat, 1);
                    tmp.fMat[kMTransY] = rowcol3(a.fMat, 3, b.fMat, 2);
                    tmp.fMat[kMPersp0] = rowcol3(a.fMat, 6, b.fMat, 0);
                    tmp.fMat[kMPersp1] = rowcol3(a.fMat, 6, b.fMat, 1);
                    tmp.fMat[kMPersp2] = rowcol3(a.fMat, 6, b.fMat, 2);

                    normalize_perspective(tmp.fMat);
                } else { // not perspective
                    tmp.fMat[kMScaleX] = a.fMat[kMScaleX] * b.fMat[kMScaleX] + a.fMat[kMSkewX]
                            * b.fMat[kMSkewY];
                    tmp.fMat[kMSkewX] = a.fMat[kMScaleX] * b.fMat[kMSkewX] + a.fMat[kMSkewX]
                            * b.fMat[kMScaleY];
                    tmp.fMat[kMTransX] = a.fMat[kMScaleX] * b.fMat[kMTransX] + a.fMat[kMSkewX]
                            * b.fMat[kMTransY];
                    tmp.fMat[kMTransX] = tmp.fMat[kMTransX] + a.fMat[kMTransX];

                    tmp.fMat[kMSkewY] = a.fMat[kMSkewY] * b.fMat[kMScaleX] + a.fMat[kMScaleY]
                            * b.fMat[kMSkewY];
                    tmp.fMat[kMScaleY] = a.fMat[kMSkewY] * b.fMat[kMSkewX] + a.fMat[kMScaleY]
                            * b.fMat[kMScaleY];
                    tmp.fMat[kMTransY] = a.fMat[kMSkewY] * b.fMat[kMTransX] + a.fMat[kMScaleY]
                            * b.fMat[kMTransY];
                    tmp.fMat[kMTransY] = tmp.fMat[kMTransY] + a.fMat[kMTransY];

                    tmp.fMat[kMPersp0] = tmp.fMat[kMPersp1] = 0;
                    tmp.fMat[kMPersp2] = kMatrix22Elem;
                }

                this.fTypeMask = tmp.fTypeMask;
                System.arraycopy(tmp.fMat, 0, this.fMat, 0, this.fMat.length);
            }
            
            this.setTypeMask(kUnknown_Mask);
            return true;
        }
        
        void setTypeMask(int mask) {
            fTypeMask = mask;
        }
        
        void clearTypeMask(int mask) {
            fTypeMask &= ~mask;
        }
        
        void setSinCos(float sinV, float cosV, float px, float py) {
            // Workaround: We can't get accurate float in JavaScript, for
            // example, cos(90), we will get 6.123031769111886e-17, very close to 0, but accutually
            // the result is 0. Here we will call toFixed(15) to get the near value so we can get 0.
            /**
             * @j2sNative
             * sinV = sinV.toFixed(15);
             * cosV = cosV.toFixed(15);
             */{}

            float oneMinusCosV = SK_Scalar1 - cosV;

            fMat[kMScaleX]  = cosV;
            fMat[kMSkewX]   = -sinV;
            fMat[kMTransX]  = sinV * py + oneMinusCosV * px;

            fMat[kMSkewY]   = sinV;
            fMat[kMScaleY]  = cosV;
            fMat[kMTransY]  = -sinV * px + oneMinusCosV * py;

            fMat[kMPersp0] = fMat[kMPersp1] = 0;
            fMat[kMPersp2] = kMatrix22Elem;
            
            setTypeMask(kUnknown_Mask);
        }
        
        void setSinCos(float sinV, float cosV) {
            // Workaround: We can't get accurate float in JavaScript, for
            // example, cos(90), we will get 6.123031769111886e-17, very close to 0, but accutually
            // the result is 0. Here we will call toFixed(15) to get the near value so we can get 0.
            /**
             * @j2sNative
             * sinV = sinV.toFixed(15);
             * cosV = cosV.toFixed(15);
             */{}

            fMat[kMScaleX]  = cosV;
            fMat[kMSkewX]   = -sinV;
            fMat[kMTransX]  = 0;

            fMat[kMSkewY]   = sinV;
            fMat[kMScaleY]  = cosV;
            fMat[kMTransY]  = 0;

            fMat[kMPersp0] = fMat[kMPersp1] = 0;
            fMat[kMPersp2] = kMatrix22Elem;

            setTypeMask(kUnknown_Mask);
        }
        
        boolean invert(SkMatrix inv) {
            boolean isPersp = this.hasPerspective();
            double scale = sk_inv_determinant(fMat, isPersp);
            
            if (scale == 0) { // underflow
                return false;
            }
            
            if (inv != null) {
                SkMatrix tmp = new SkMatrix(null);
                if (inv == this) {
                    inv.fTypeMask = this.fTypeMask;
                    System.arraycopy(this.fMat, 0, inv.fMat, 0, this.fMat.length);
                }
                
                if (isPersp) {
                    tmp.fMat[kMScaleX] = (float) ((fMat[kMScaleY] * fMat[kMPersp2] - fMat[kMTransY] * fMat[kMPersp1]) * scale);
                    tmp.fMat[kMSkewX]  = (float) ((fMat[kMTransX] * fMat[kMPersp1] - fMat[kMSkewX] * fMat[kMPersp2]) * scale);
                    tmp.fMat[kMTransX] = (float) ((fMat[kMSkewX] * fMat[kMTransY] - fMat[kMTransX] * fMat[kMScaleY]) * scale);

                    tmp.fMat[kMSkewY]  = (float) ((fMat[kMTransY] * fMat[kMPersp0] - fMat[kMSkewY] * fMat[kMPersp2]) * scale);
                    tmp.fMat[kMScaleY] = (float) ((fMat[kMScaleX] * fMat[kMPersp2] - fMat[kMTransX] * fMat[kMPersp0]) * scale);
                    tmp.fMat[kMTransY] = (float) ((fMat[kMTransX] * fMat[kMSkewY] - fMat[kMScaleX] * fMat[kMTransY]) * scale);

                    tmp.fMat[kMPersp0] = (float) ((fMat[kMSkewY] * fMat[kMPersp1] - fMat[kMScaleY] * fMat[kMPersp0]) * scale);             
                    tmp.fMat[kMPersp1] = (float) ((fMat[kMSkewX] * fMat[kMPersp0] - fMat[kMScaleX] * fMat[kMPersp1]) * scale);
                    tmp.fMat[kMPersp2] = (float) ((fMat[kMScaleX] * fMat[kMScaleY] - fMat[kMSkewX] * fMat[kMSkewY]) * scale);
                } else {
                    tmp.fMat[kMScaleX] = (float)(fMat[kMScaleY] * scale);
                    tmp.fMat[kMSkewX] = (float)(-fMat[kMSkewX] * scale);
                    tmp.fMat[kMTransX] = mul_diff_scale(fMat[kMSkewX], fMat[kMTransY], fMat[kMScaleY], fMat[kMTransX], scale);

                    tmp.fMat[kMSkewY] = (float)(-fMat[kMSkewY] * scale);
                    tmp.fMat[kMScaleY] = (float)(fMat[kMScaleX] * scale);
                    tmp.fMat[kMTransY] = mul_diff_scale(fMat[kMSkewY], fMat[kMTransX], fMat[kMScaleX], fMat[kMTransY], scale);

                    tmp.fMat[kMPersp0] = 0;
                    tmp.fMat[kMPersp1] = 0;
                    tmp.fMat[kMPersp2] = kMatrix22Elem;
                }
        
                System.arraycopy(tmp.fMat, 0, inv.fMat, 0, tmp.fMat.length); 
                inv.setTypeMask(kUnknown_Mask);
            }
            
            return true;
        }
        
        private float mul_diff_scale(double a, double b, double c, double d,
                double scale) {
            return (float)((a * b - c * d) * scale);
        }
        
        boolean setRectToRect(RectF src, RectF dst, int stf) {
            if (src.isEmpty()) {
                reset();
                return false;
            }

            if (dst.isEmpty()) {
                Arrays.fill(fMat, 0);
                setTypeMask(kScale_Mask | kRectStaysRect_Mask);
            } else {
                float tx, sx = dst.width() / src.width();
                float ty, sy = dst.height() / src.height();
                boolean xLarger = false;

                if (stf != kFill_ScaleToFit) {
                    if (sx > sy) {
                        xLarger = true;
                        sx = sy;
                    } else {
                        sy = sx;
                    }
                }

                tx = dst.left - src.left * sx;
                ty = dst.top - src.top * sy;
                if (stf == kCenter_ScaleToFit || stf == kEnd_ScaleToFit) {
                    float diff;

                    if (xLarger) {
                        diff = dst.width() - src.width() * sy;
                    } else {
                        diff = dst.height() - src.height() * sy;
                    }

                    if (stf == kCenter_ScaleToFit) {
                        diff = diff * 0.5f;
                    }

                    if (xLarger) {
                        tx += diff;
                    } else {
                        ty += diff;
                    }
                }

                fMat[kMScaleX] = sx;
                fMat[kMScaleY] = sy;
                fMat[kMTransX] = tx;
                fMat[kMTransY] = ty;
                fMat[kMSkewX] = fMat[kMSkewY] =
                        fMat[kMPersp0] = fMat[kMPersp1] = 0;

                setTypeMask(kScale_Mask | kTranslate_Mask | kRectStaysRect_Mask);
            }

            // shared cleanup
            fMat[kMPersp2] = kMatrix22Elem;
            return true;
        }
        
        void setSkew(float sx, float sy, float px, float py) {
            fMat[kMScaleX] = SK_Scalar1;
            fMat[kMSkewX] = sx;
            fMat[kMTransX] = -sx * py;

            fMat[kMSkewY] = sy;
            fMat[kMScaleY] = SK_Scalar1;
            fMat[kMTransY] = -sy * px;

            fMat[kMPersp0] = fMat[kMPersp1] = 0;
            fMat[kMPersp2] = kMatrix22Elem;

            setTypeMask(kUnknown_Mask);
        }

        void setSkew(float sx, float sy) {
            fMat[kMScaleX] = SK_Scalar1;
            fMat[kMSkewX] = sx;
            fMat[kMTransX] = 0;

            fMat[kMSkewY] = sy;
            fMat[kMScaleY] = SK_Scalar1;
            fMat[kMTransY] = 0;

            fMat[kMPersp0] = fMat[kMPersp1] = 0;
            fMat[kMPersp2] = kMatrix22Elem;

            setTypeMask(kUnknown_Mask);
        }
        
        boolean preTranslate(float dx, float dy) {
            if (hasPerspective()) {
                SkMatrix m = new SkMatrix(null);
                m.setTranslate(dx, dy);
                return this.preConcat(m);
            }
            
            if (dx != 0 || dy != 0) {
                fMat[kMTransX] += fMat[kMScaleX] * dx + fMat[kMSkewX] * dy;
                fMat[kMTransY] += fMat[kMSkewY] * dx + fMat[kMScaleY] * dy;

                setTypeMask(kUnknown_Mask);
            }
            
            return true;
        }
        
        boolean preScale(float sx, float sy, float px, float py) {
            SkMatrix m = new SkMatrix(null);
            m.setScale(sx, sy, px, py);
            return this.preConcat(m);
        }

        boolean preScale(float sx, float sy) {
            SkMatrix m = new SkMatrix(null);
            m.setScale(sx, sy);
            return this.preConcat(m);
        }
        
        boolean preRotate(float degrees, float px, float py) {
            SkMatrix m = new SkMatrix(null);
            m.setRotate(degrees, px, py);
            return this.preConcat(m);
        }

        boolean preRotate(float degrees) {
            SkMatrix m = new SkMatrix(null);
            m.setRotate(degrees);
            return this.preConcat(m);
        }
        
        boolean preSkew(float sx, float sy, float px, float py) {
            SkMatrix m  = new SkMatrix(null);
            m.setSkew(sx, sy, px, py);
            return this.preConcat(m);
        }
        
        boolean preSkew(float sx, float sy) {
            SkMatrix m  = new SkMatrix(null);
            m.setSkew(sx, sy);
            return this.preConcat(m);
        }
        
        boolean preConcat(SkMatrix mat) {
            // check for identity first, so we don't do a needless copy of ourselves
            // to ourselves inside setConcat()
            return mat.isIdentity() || this.setConcat(this, mat);
        }
        
        boolean postConcat(SkMatrix mat) {
            // check for identity first, so we don't do a needless copy of ourselves
            // to ourselves inside setConcat()
            return mat.isIdentity() || this.setConcat(mat, this);
        }
        boolean postTranslate(float dx, float dy) {
            if (hasPerspective()) {
                SkMatrix m = new SkMatrix(null);
                m.setTranslate(dx, dy);
                return this.postConcat(m);
            }

            if (dx != 0 || dy != 0) {
                fMat[kMTransX] += dx;
                fMat[kMTransY] += dy;
                setTypeMask(kUnknown_Mask);
            }
            return true;
        }
        
        boolean postScale(float sx, float sy, float px, float py) {
            SkMatrix m = new SkMatrix(null);
            m.setScale(sx, sy, px, py);
            return this.postConcat(m);
        }
        
        boolean postScale(float sx, float sy) {
            SkMatrix m = new SkMatrix(null);
            m.setScale(sx, sy);
            return this.postConcat(m);
        }
        
        boolean postRotate(float degrees, float px, float py) {
            SkMatrix m = new SkMatrix(null);
            m.setRotate(degrees, px, py);
            return this.postConcat(m);
        }

        boolean postRotate(float degrees) {
            SkMatrix m = new SkMatrix(null);
            m.setRotate(degrees);
            return this.postConcat(m);
        }

        boolean postSkew(float sx, float sy, float px, float py) {
            SkMatrix m = new SkMatrix(null);
            m.setSkew(sx, sy, px, py);
            return this.postConcat(m);
        }
        
        boolean postSkew(float sx, float sy) {
            SkMatrix m = new SkMatrix(null);
            m.setSkew(sx, sy);
            return this.postConcat(m);
        }
        
        static double sk_inv_determinant(float mat[], boolean isPerspective) {
            double det;

            if (isPerspective) {
                det = mat[SkMatrix.kMScaleX]
                        * ((double) mat[SkMatrix.kMScaleY] * mat[SkMatrix.kMPersp2] - (double) mat[SkMatrix.kMTransY]
                                * mat[SkMatrix.kMPersp1])
                        +
                        mat[SkMatrix.kMSkewX]
                        * ((double) mat[SkMatrix.kMTransY] * mat[SkMatrix.kMPersp0] - (double) mat[SkMatrix.kMSkewY]
                                * mat[SkMatrix.kMPersp2])
                        +
                        mat[SkMatrix.kMTransX]
                        * ((double) mat[SkMatrix.kMSkewY] * mat[SkMatrix.kMPersp1] - (double) mat[SkMatrix.kMScaleY]
                                * mat[SkMatrix.kMPersp0]);
            } else {
                det = (double) mat[SkMatrix.kMScaleX] * mat[SkMatrix.kMScaleY]
                        - (double) mat[SkMatrix.kMSkewX] * mat[SkMatrix.kMSkewY];
            }

            // Since the determinant is on the order of the cube of the matrix
            // members,
            // compare to the cube of the default nearly-zero constant (although
            // an
            // estimate of the condition number would be better if it wasn't so
            // expensive).
            if (SkScalarNearlyZero((float) det, tolerance * tolerance * tolerance)) {
                return 0;
            }
            return 1.0 / det;
        }
        
        static boolean polyToPoint(float pt[], float poly[], int count) {
            float x = 1, y = 1;
            float[] pt1 = new float[2];
            float[] pt2 = new float[2];

            if (count > 1) {
                pt1[0] = poly[2] - poly[0];
                pt1[1] = poly[3] - poly[1];
                y = (float) Math.sqrt(pt1[0] * pt1[0] + pt1[1] * pt1[1]);
                if (y * y == 0) {
                    return false;
                }
                switch (count) {
                    case 2:
                        break;
                    case 3:
                        pt2[0] = poly[1] - poly[5];
                        pt2[1] = poly[4] - poly[0];
                        x = (pt1[0] * pt2[0] + pt1[1] * pt2[1]) / y;
                        break;
                    default:
                        pt2[0] = poly[1] - poly[7];
                        pt2[1] = poly[6] - poly[0];
                        x = (pt1[0] * pt2[0] + pt1[1] * pt2[1]) / y;
                        break;
                }
            }
            pt[0] = x;
            pt[1] = y;
            return true;
        }
        
        static boolean SkScalarNearlyZero(float x, float f) {
            return Math.abs(x) < f;
        }
        
        static boolean Poly2Proc(float srcPt[], SkMatrix dst, float scale[]) {
            float invScale = 1 / scale[1];

            dst.fMat[kMScaleX] = (srcPt[3] - srcPt[1]) * invScale;
            dst.fMat[kMSkewY] = (srcPt[0] - srcPt[2]) * invScale;
            dst.fMat[kMPersp0] = 0;
            dst.fMat[kMSkewX] = (srcPt[2] - srcPt[0]) * invScale;
            dst.fMat[kMScaleY] = (srcPt[3] - srcPt[1]) * invScale;
            dst.fMat[kMPersp1] = 0;
            dst.fMat[kMTransX] = srcPt[0];
            dst.fMat[kMTransY] = srcPt[1];
            dst.fMat[kMPersp2] = 1;
            dst.setTypeMask(kUnknown_Mask);
            return true;
        }
        
        static boolean Poly3Proc(float srcPt[], SkMatrix dst, float scale[]) {
            float invScale = 1 / scale[0];
            dst.fMat[kMScaleX] = (srcPt[4] - srcPt[0]) * invScale;
            dst.fMat[kMSkewY] = (srcPt[5] - srcPt[1]) * invScale;
            dst.fMat[kMPersp0] = 0;

            invScale = 1 / scale[1];
            dst.fMat[kMSkewX] = (srcPt[2] - srcPt[0]) * invScale;
            dst.fMat[kMScaleY] = (srcPt[3] - srcPt[1]) * invScale;
            dst.fMat[kMPersp1] = 0;

            dst.fMat[kMTransX] = srcPt[0];
            dst.fMat[kMTransY] = srcPt[1];
            dst.fMat[kMPersp2] = 1;
            dst.setTypeMask(kUnknown_Mask);
            return true;
        }
        
        static boolean Poly4Proc(float srcPt[], SkMatrix dst, float scale[]) {
            float   a1, a2;
            float   x0, y0, x1, y1, x2, y2;

            x0 = srcPt[4] - srcPt[0];
            y0 = srcPt[5] - srcPt[1];
            x1 = srcPt[4] - srcPt[2];
            y1 = srcPt[5] - srcPt[3];
            x2 = srcPt[4] - srcPt[6];
            y2 = srcPt[5] - srcPt[7];

            /* check if abs(x2) > abs(y2) */
            if ( x2 > 0 ? y2 > 0 ? x2 > y2 : x2 > -y2 : y2 > 0 ? -x2 > y2 : x2 < y2) {
                float denom = x1 * y2 / x2 - y1;
                if (denom * denom == 0) {
                    return false;
                }
                a1 = ((x0 - x1) * y2 / x2 - y0 + y1) / denom;
            } else {
                float denom = x1 - y1 * x2 / y2;
                if (denom * denom == 0) {
                    return false;
                }
                a1 = (x0 - x1 - ((y0 - y1) * x2 / y2)) / denom;
            }

            /* check if abs(x1) > abs(y1) */
            if ( x1 > 0 ? y1 > 0 ? x1 > y1 : x1 > -y1 : y1 > 0 ? -x1 > y1 : x1 < y1) {
                float denom = y2 - (x2 * y1 / x1);
                if (denom * denom == 0) {
                    return false;
                }
                a2 = (y0 - y2 - (x0 - x2) * y1 / x1) / denom;
            } else {
                float denom = y2 * x1 / y1 - x2;
                if (denom * denom == 0) {
                    return false;
                }
                a2 = ((y0 - y2) * x1 / y1 - x0 + x2) / denom;
            }

            float invScale = 1 / scale[0];
            dst.fMat[kMScaleX] = (a2 * srcPt[6] + srcPt[6] - srcPt[0]) * invScale;
            dst.fMat[kMSkewY] = (a2 * srcPt[7] + srcPt[7] - srcPt[1]) * invScale;
            dst.fMat[kMPersp0] = a2 * invScale;
            invScale = 1 / scale[1];
            dst.fMat[kMSkewX] = (a1 * srcPt[2] + srcPt[2] - srcPt[0]) * invScale;
            dst.fMat[kMScaleY] = (a1 * srcPt[3] + srcPt[3] - srcPt[1]) * invScale;
            dst.fMat[kMPersp1] = a1 * invScale;
            dst.fMat[kMTransX] = srcPt[0];
            dst.fMat[kMTransY] = srcPt[1];
            dst.fMat[kMPersp2] = 1;
            dst.setTypeMask(kUnknown_Mask);
            return true;
        }
        
        /*  Taken from Rob Johnson's original sample code in QuickDraw GX
        */
        boolean setPolyToPoly(float src[], float dst[], int count) {
            if (count > 4) {
                return false;
            }
            
            if (0 == count) {
                this.reset();
                return true;
            }
            
            if (1 == count) {
                this.setTranslate(dst[0] - src[0], dst[1] - src[1]);
                return true;
            }
            
            float[] scale = new float[2];
            if (!polyToPoint(scale, src, count) || SkScalarNearlyZero(scale[0], tolerance) || SkScalarNearlyZero(scale[1], tolerance)) {
                return false;
            }
            
            SkMatrix tempMap = new SkMatrix(null);
            SkMatrix result = new SkMatrix(null);
            tempMap.setTypeMask(kUnknown_Mask);
            
            boolean flag = true;
           switch (count - 2) {
               case 0:
                   flag = Poly2Proc(src, tempMap, scale);
                   break;
               case 1:
                   flag = Poly3Proc(src, tempMap, scale);
                   break;
               case 2:
                   flag = Poly4Proc(src, tempMap, scale);
                   break;
           }
           
           if (!flag) return false;
           
           if (!tempMap.invert(result)) {
               return false;
           }
           
           switch (count - 2) {
               case 0:
                   flag = Poly2Proc(dst, tempMap, scale);
                   break;
               case 1:
                   flag = Poly3Proc(dst, tempMap, scale);
                   break;
               case 2:
                   flag = Poly4Proc(dst, tempMap, scale);
                   break;
           } 
           
           if (!result.setConcat(tempMap, result)) {
               return false;
           }
           
           this.fTypeMask = result.fTypeMask;
           System.arraycopy(result.fMat, 0, this.fMat, 0, this.fMat.length);
           return true;
        }
   
 ////////////////////////////////////////////////////////////////////////////////
        
        void Identity_pts(SkMatrix m, float dst[], float src[], int count) {
            if (dst != src && count > 0) {
                System.arraycopy(src, 0, dst, 0, count * 2);
            }
        }
        
        void Trans_pts(SkMatrix m, float dst[], float src[], int count) {
            if (count > 0) {
                float tx = m.fMat[kMTransX];
                float ty = m.fMat[kMTransY];

                int i = 0;
                do {
                    dst[i + 1] = src[i + 1] + ty;
                    dst[i + 0] = src[i + 0] + tx;
                    i += 2;
                } while (--count != 0);
            }
        }
        
        void Scale_pts(SkMatrix m, float dst[], float src[], int count) {
            if (count > 0) {
                float mx = m.fMat[kMScaleX];
                float my = m.fMat[kMScaleY];

                int i = 0;
                do {
                    dst[i + 1] = src[i + 1] * my;
                    dst[i + 0] = src[i + 0] * mx;
                    i += 2;
                } while (--count != 0);
            }
        }
        
        void ScaleTrans_pts(SkMatrix m, float dst[], float src[], int count) {
            if (count > 0) {
                float mx = m.fMat[kMScaleX];
                float my = m.fMat[kMScaleY];
                float tx = m.fMat[kMTransX];
                float ty = m.fMat[kMTransY];
                int i = 0;
                do {
                    dst[i + 1] = src[i + 1] * my + ty;
                    dst[i + 0] = src[i + 0] * mx + tx;
                    i += 2;
                } while (--count != 0);
            }
        }
        
        void Rot_pts(SkMatrix m, float dst[], float src[], int count) {
            if (count > 0) {
                float mx = m.fMat[kMScaleX];
                float my = m.fMat[kMScaleY];
                float kx = m.fMat[kMSkewX];
                float ky = m.fMat[kMSkewY];

                int i = 0;
                do {
                    float sy = src[i + 1];
                    float sx = src[i + 0];
                    dst[i + 1] = sx * ky + sy * my;
                    dst[i + 0] = sx * mx + sy * kx;
                    i += 2;
                } while (--count != 0);
            }
        }
        
        void RotTrans_pts(SkMatrix m, float dst[], float src[], int count) {
            if (count > 0) {
                float mx = m.fMat[kMScaleX];
                float my = m.fMat[kMScaleY];
                float kx = m.fMat[kMSkewX];
                float ky = m.fMat[kMSkewY];
                float tx = m.fMat[kMTransX];
                float ty = m.fMat[kMTransY];

                int i = 0;
                do {
                    float sy = src[i + 1];
                    float sx = src[i + 0];
                    dst[i + 1] = sx * ky + sy * my + ty;
                    dst[i + 0] = sx * mx + sy * kx + tx;
                    i += 2;
                } while (--count != 0);
            }
        }
        
        void Persp_pts(SkMatrix m, float dst[], float src[], int count) {
            if (count > 0) {
                int i = 0;
                do {
                    float sy = src[i + 1];
                    float sx = src[i + 0];

                    float x = sx * m.fMat[kMScaleX] +
                            sy * m.fMat[kMSkewX] + m.fMat[kMTransX];
                    float y = sx * m.fMat[kMSkewY] +
                            sy * m.fMat[kMScaleY] + m.fMat[kMTransY];
                    float z = sx * m.fMat[kMPersp0] +
                            sy * m.fMat[kMPersp1] + m.fMat[kMPersp2];
                    if (z != 0) {
                        z = SK_Scalar1 / z;
                    }

                    dst[i + 1] = y * z;
                    dst[i + 0] = x * z;
                    i += 2;
                } while (--count != 0);
            }
        }
        
        
        void mapPoints(float dst[], float src[], int count) {
            switch (this.getType() & kAllMasks) {
                case 0:
                    Identity_pts(this, dst, src, count);
                    break;
                case 1:
                    Trans_pts(this, dst, src, count);
                    break;
                case 2:
                    Scale_pts(this, dst, src, count);
                    break;
                case 3:
                    ScaleTrans_pts(this, dst, src, count);
                    break;
                case 4:
                    Rot_pts(this, dst, src, count);
                    break;
                case 5:
                    RotTrans_pts(this, dst, src, count);
                    break;
                case 6:
                    RotTrans_pts(this, dst, src, count);
                    break;
                case 7:
                    RotTrans_pts(this, dst, src, count);
                    break;
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                    Persp_pts(this, dst, src, count);
                    break;
            }
        }
        
        void Identity_xy(SkMatrix m, float sx, float sy, float pt[]) {
            pt[0] = sx;
            pt[1] = sy;
        }
        
        void Trans_xy(SkMatrix m, float sx, float sy, float pt[]) {
            pt[0] = sx + m.fMat[kMTransX];
            pt[1] = sy + m.fMat[kMTransY];
        }
        
        void Scale_xy(SkMatrix m, float sx, float sy, float pt[]) {
            pt[0] = sx * m.fMat[kMScaleX];
            pt[1] = sy * m.fMat[kMScaleY];
        }
        
        void ScaleTrans_xy(SkMatrix m, float sx, float sy, float pt[]) {
            pt[0] = sx * m.fMat[kMScaleX] + m.fMat[kMTransX];
            pt[1] = sy * m.fMat[kMScaleY] + m.fMat[kMTransY];
        }
        
        void Rot_xy(SkMatrix m, float sx, float sy, float pt[]) {
            pt[0] = sx * m.fMat[kMScaleX] + sy * m.fMat[kMSkewX] + m.fMat[kMTransX];
            pt[1] = sx * m.fMat[kMSkewY] + sy * m.fMat[kMScaleY] + m.fMat[kMTransY];
        }
        
        void RotTrans_xy(SkMatrix m, float sx, float sy, float pt[]) {
            pt[0] = sx * m.fMat[kMScaleX] + sy * m.fMat[kMSkewX] + m.fMat[kMTransX];
            pt[1] = sx * m.fMat[kMSkewY] + sy * m.fMat[kMScaleY] + m.fMat[kMTransY];
        }
        
        void Persp_xy(SkMatrix m, float sx, float sy, float pt[]) {
            float x = sx * m.fMat[kMScaleX] + sy * m.fMat[kMSkewX] + m.fMat[kMTransX];
            float y = sx * m.fMat[kMSkewY] + sy * m.fMat[kMScaleY] + m.fMat[kMTransY];
            float z = sx * m.fMat[kMPersp0] + sy * m.fMat[kMPersp1] + m.fMat[kMPersp2];
            if (z != 0) {
                z = SK_Scalar1 / z;
            }
            
            pt[0] = x * z;
            pt[1] = y * z;
        }
        
        void mapXY(int masks, SkMatrix m, float x, float y, float result[]) {
            switch (masks & kAllMasks) {
                case 0:
                    Identity_xy(m, x, y, result);
                    break;
                case 1:
                    Trans_xy(m, x, y, result);
                    break;
                case 2:
                    Scale_xy(m, x, y, result);
                    break;
                case 3:
                    ScaleTrans_xy(m, x, y, result);
                    break;          
                case 4:
                    Rot_xy(m, x, y, result);
                    break; 
                case 5:
                    RotTrans_xy(m, x, y, result);
                    break;  
                case 6:
                    Rot_xy(m, x, y, result);
                    break; 
                case 7:
                    RotTrans_xy(m, x, y, result);
                    break;  
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                    Persp_xy(m, x, y, result);
                    break;  
            }
        }
        
        void mapVectors(float dst[], float src[], int count) {
            if ((this.fTypeMask & kPerspective_Mask) != 0) {
                float[] origin = new float[2];

                mapXY(this.fTypeMask, this, 0, 0, origin);

                int index = 0;
                for (int i = 0; i < count; i++) {
                    float[] tmp = new float[2];

                    mapXY(this.fTypeMask, this, src[index+0], src[index+1], tmp);
                    dst[index+0] = tmp[0] - origin[0];
                    dst[index+1] = tmp[1] - origin[1];
                    index += 2;
                    
                }
            } else {
                SkMatrix tmp = new SkMatrix(null);
                tmp.fTypeMask = this.fTypeMask;
                System.arraycopy(this.fMat, 0, tmp.fMat, 0, this.fMat.length);

                tmp.fMat[kMTransX] = tmp.fMat[kMTransY] = 0;
                tmp.clearTypeMask(kTranslate_Mask);
                tmp.mapPoints(dst, src, count);
            }
        }
        
        boolean mapRect(RectF dst, RectF src) {
            if (this.rectStaysRect()) {
                float[] tmpSrc = new float[4];
                float[] tmpDst = new float[4];
                tmpSrc[0] = src.left;
                tmpSrc[1] = src.top;
                tmpSrc[2] = src.right;
                tmpSrc[3] = src.bottom;
                this.mapPoints(tmpDst, tmpSrc, 2);
                dst.left = (int) tmpDst[0];
                dst.top = (int) tmpDst[1];
                dst.right = (int) tmpDst[2];
                dst.bottom = (int) tmpDst[3];

                dst.sort();
                return true;
            } else {
                float[] quad = new float[8];
                quad[0] = src.left;
                quad[1] = src.top;
                quad[2] = src.right;
                quad[3] = src.top;
                quad[4] = src.right;
                quad[5] = src.bottom;
                quad[6] = src.left;
                quad[7] = src.bottom;

                this.mapPoints(quad, quad, 4);

                float l, t, r, b;
                l = r = quad[0];
                t = b = quad[1];

                for (int i = 2; i < 8; i += 2) {
                    float x = quad[i + 0];
                    float y = quad[i + 1];

                    if (x < l)
                        l = x;
                    else if (x > r)
                        r = x;
                    if (y < t)
                        t = y;
                    else if (y > b)
                        b = y;
                }
                dst.set((int) l, (int) t, (int) r, (int) b);
                return false;
            }
        }
        
        float mapRadius(float radius) {
            float[] points = new float[4];
            points[0] = radius;
            points[1] = 0;
            points[2] = 0;
            points[3] = radius;
            this.mapVectors(points, points, 2);
            
            float d0 = (float) Math.sqrt(points[0] * points[0] + points[1] * points[1]);
            float d1 = (float) Math.sqrt(points[2] * points[2] + points[3] * points[3]);
            if (radius == Float.MAX_VALUE) {
                return Float.POSITIVE_INFINITY;
            } else if(radius == Float.MIN_VALUE) {
                return 0f;
            }
            return (float) Math.sqrt(d0 * d1);
        }
        
        void setValues(float values[]) {
            for (int i = 0; i < 9; i++) {
                this.set(i, values[i]);
            }
        }
        
        void getValues(float values[]) {
            for (int i = 0; i < 9; i++) {
                values[i] = this.get(i);
            }
        }
    }
}

