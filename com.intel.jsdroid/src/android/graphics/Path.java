/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.graphics;

import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.util.Log;

import java.util.ArrayList;

/**
 * The Path class encapsulates compound (multiple contour) geometric paths
 * consisting of straight line segments, quadratic curves, and cubic curves.
 * It can be drawn with canvas.drawPath(path, paint), either filled or stroked
 * (based on the paint's Style), or it can be used for clipping or to draw
 * text on a path.
 */
public class Path {

    /**
     * Create an empty path
     */
    public Path() {
        mNativePath = init1();
    }

    /**
     * Create a new path, copying the contents from the src path.
     *
     * @param src The path to copy from when initializing the new path
     */
    public Path(Path src) {
        SkPath valNative = null;
        if (src != null) {
            valNative = src.mNativePath;
        }
        mNativePath = init2(valNative);
    }
    
    /**
     * Clear any lines and curves from the path, making it empty.
     * This does NOT change the fill-type setting.
     */
    public void reset() {
        native_reset(mNativePath);
    }

    /**
     * Rewinds the path: clears any lines and curves from the path but
     * keeps the internal data structure for faster reuse.
     */
    public void rewind() {
        native_rewind(mNativePath);
    }

    /** Replace the contents of this with the contents of src.
    */
    public void set(Path src) {
        if (this != src) {
            native_set(mNativePath, src.mNativePath);
        }
    }

    /** Enum for the ways a path may be filled
    */
    public static class FillType {
        // these must match the values in SkPath.h
        public static final FillType WINDING = new FillType(0);
        public static final FillType EVEN_ODD = new FillType(1);
        public static final FillType INVERSE_WINDING = new FillType (2);
        public static final FillType INVERSE_EVEN_ODD = new FillType(3);

        FillType(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;

        public static FillType valueOf(String value) {
            if ("WINDING".equals(value)) {
                return FillType.WINDING;
            } else if ("EVEN_ODD".equals(value)) {
                return FillType.EVEN_ODD;
            } else if ("INVERSE_WINDING".equals(value)) {
                return FillType.INVERSE_WINDING;
            } else if ("INVERSE_EVEN_ODD".equals(value)) {
                return FillType.INVERSE_EVEN_ODD;
            } else {
                return null;
            }
        }

        public static FillType[] values() {
            FillType[] filltype = new FillType[4];
            filltype[0] = FillType.WINDING;
            filltype[1] = FillType.EVEN_ODD;
            filltype[2] = FillType.INVERSE_WINDING;
            filltype[3] = FillType.INVERSE_EVEN_ODD;
            return filltype;
        }
    }

    // these must be in the same order as their native values
    private static final FillType[] sFillTypeArray = {
        FillType.WINDING,
        FillType.EVEN_ODD,
        FillType.INVERSE_WINDING,
        FillType.INVERSE_EVEN_ODD
    };

    /**
     * Return the path's fill type. This defines how "inside" is
     * computed. The default value is WINDING.
     *
     * @return the path's fill type
     */
    public FillType getFillType() {
        return sFillTypeArray[native_getFillType(mNativePath)];
    }

    /**
     * Set the path's fill type. This defines how "inside" is computed.
     *
     * @param ft The new fill type for this path
     */
    public void setFillType(FillType ft) {
        native_setFillType(mNativePath, ft.nativeInt);
    }
    
    /**
     * Returns true if the filltype is one of the INVERSE variants
     *
     * @return true if the filltype is one of the INVERSE variants
     */
    public boolean isInverseFillType() {
        final int ft = native_getFillType(mNativePath);
        return (ft & 2) != 0;
    }
    
    /**
     * Toggles the INVERSE state of the filltype
     */
    public void toggleInverseFillType() {
        int ft = native_getFillType(mNativePath);
        ft ^= 2;
        native_setFillType(mNativePath, ft);
    }
    
    /**
     * Returns true if the path is empty (contains no lines or curves)
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    public boolean isEmpty() {
        return native_isEmpty(mNativePath);
    }

    /**
     * Returns true if the path specifies a rectangle. If so, and if rect is
     * not null, set rect to the bounds of the path. If the path does not
     * specify a rectangle, return false and ignore rect.
     *
     * @param rect If not null, returns the bounds of the path if it specifies
     *             a rectangle
     * @return     true if the path specifies a rectangle
     */
    public boolean isRect(RectF rect) {
        return native_isRect(mNativePath, rect);
    }

    /**
     * Compute the bounds of the control points of the path, and write the
     * answer into bounds. If the path contains 0 or 1 points, the bounds is
     * set to (0,0,0,0)
     *
     * @param bounds Returns the computed bounds of the path's control points.
     * @param exact This parameter is no longer used.
     */
    public void computeBounds(RectF bounds, boolean exact) {
        native_computeBounds(mNativePath, bounds);
    }

    /**
     * Hint to the path to prepare for adding more points. This can allow the
     * path to more efficiently allocate its storage.
     *
     * @param extraPtCount The number of extra points that may be added to this
     *                     path
     */
    public void incReserve(int extraPtCount) {
        // We can just ignore this.
        //native_incReserve(mNativePath, extraPtCount);
    }

    /**
     * Set the beginning of the next contour to the point (x,y).
     *
     * @param x The x-coordinate of the start of a new contour
     * @param y The y-coordinate of the start of a new contour
     */
    public void moveTo(float x, float y) {
        native_moveTo(mNativePath, x, y);
    }

    /**
     * Set the beginning of the next contour relative to the last point on the
     * previous contour. If there is no previous contour, this is treated the
     * same as moveTo().
     *
     * @param dx The amount to add to the x-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     * @param dy The amount to add to the y-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     */
    public void rMoveTo(float dx, float dy) {
        native_rMoveTo(mNativePath, dx, dy);
    }

    /**
     * Add a line from the last point to the specified point (x,y).
     * If no moveTo() call has been made for this contour, the first point is
     * automatically set to (0,0).
     *
     * @param x The x-coordinate of the end of a line
     * @param y The y-coordinate of the end of a line
     */
    public void lineTo(float x, float y) {
        native_lineTo(mNativePath, x, y);
    }

    /**
     * Same as lineTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx The amount to add to the x-coordinate of the previous point on
     *           this contour, to specify a line
     * @param dy The amount to add to the y-coordinate of the previous point on
     *           this contour, to specify a line
     */
    public void rLineTo(float dx, float dy) {
        native_rLineTo(mNativePath, dx, dy);
    }

    /**
     * Add a quadratic bezier from the last point, approaching control point
     * (x1,y1), and ending at (x2,y2). If no moveTo() call has been made for
     * this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
        native_quadTo(mNativePath, x1, y1, x2, y2);
    }

    /**
     * Same as quadTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     */
    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        native_rQuadTo(mNativePath, dx1, dy1, dx2, dy2);
    }

    /**
     * Add a cubic bezier from the last point, approaching control points
     * (x1,y1) and (x2,y2), and ending at (x3,y3). If no moveTo() call has been
     * made for this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the 1st control point on a cubic curve
     * @param y1 The y-coordinate of the 1st control point on a cubic curve
     * @param x2 The x-coordinate of the 2nd control point on a cubic curve
     * @param y2 The y-coordinate of the 2nd control point on a cubic curve
     * @param x3 The x-coordinate of the end point on a cubic curve
     * @param y3 The y-coordinate of the end point on a cubic curve
     */
    public void cubicTo(float x1, float y1, float x2, float y2,
                        float x3, float y3) {
        native_cubicTo(mNativePath, x1, y1, x2, y2, x3, y3);
    }

    /**
     * Same as cubicTo, but the coordinates are considered relative to the
     * current point on this contour. If there is no previous point, then a
     * moveTo(0,0) is inserted automatically.
     */
    public void rCubicTo(float x1, float y1, float x2, float y2,
                         float x3, float y3) {
        native_rCubicTo(mNativePath, x1, y1, x2, y2, x3, y3);
    }

    /**
     * Append the specified arc to the path as a new contour. If the start of
     * the path is different from the path's current last point, then an
     * automatic lineTo() is added to connect the current contour to the
     * start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc. The sweep angle is tread mod 360.
     *
     * @param oval        The bounds of oval defining shape and size of the arc
     * @param startAngle  Starting angle (in degrees) where the arc begins
     * @param sweepAngle  Sweep angle (in degrees) measured clockwise, treated
     *                    mod 360.
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public void arcTo(RectF oval, float startAngle, float sweepAngle,
                      boolean forceMoveTo) {
        native_arcTo(mNativePath, oval, startAngle, sweepAngle, forceMoveTo);
    }
    
    /**
     * Append the specified arc to the path as a new contour. If the start of
     * the path is different from the path's current last point, then an
     * automatic lineTo() is added to connect the current contour to the
     * start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param oval        The bounds of oval defining shape and size of the arc
     * @param startAngle  Starting angle (in degrees) where the arc begins
     * @param sweepAngle  Sweep angle (in degrees) measured clockwise
     */
    public void arcTo(RectF oval, float startAngle, float sweepAngle) {
        native_arcTo(mNativePath, oval, startAngle, sweepAngle, false);
    }
    
    /**
     * Close the current contour. If the current point is not equal to the
     * first point of the contour, a line segment is automatically added.
     */
    public void close() {
        native_close(mNativePath);
    }

    /**
     * Specifies how closed shapes (e.g. rects, ovals) are oriented when they
     * are added to a path.
     */
    public static class Direction {
        /** clockwise */
        public static final Direction CW = new Direction(0); // must match enum in SkPath.h
        /** counter-clockwise */
        public static final Direction CCW = new Direction(1); // must match enum in SkPath.h

        Direction(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;

        public static Direction valueOf(String value) {
            if ("CW".equals(value)) {
                return Direction.CW;
            } else if ("CCW".equals(value)) {
                return Direction.CCW;
            } else {
                return null;
            }
        }

        public static Direction[] values() {
            Direction[] direction = new Direction[2];
            direction[0] = Direction.CW;
            direction[1] = Direction.CCW;
            return direction;
        }
    }

    /**
     * Add a closed rectangle contour to the path
     *
     * @param rect The rectangle to add as a closed contour to the path
     * @param dir  The direction to wind the rectangle's contour
     */
    public void addRect(RectF rect, Direction dir) {
        if (rect == null) {
            throw new NullPointerException("need rect parameter");
        }
        native_addRect(mNativePath, rect, dir.nativeInt);
    }

    /**
     * Add a closed rectangle contour to the path
     *
     * @param left   The left side of a rectangle to add to the path
     * @param top    The top of a rectangle to add to the path
     * @param right  The right side of a rectangle to add to the path
     * @param bottom The bottom of a rectangle to add to the path
     * @param dir    The direction to wind the rectangle's contour
     */
    public void addRect(float left, float top, float right, float bottom,
                        Direction dir) {
        native_addRect(mNativePath, left, top, right, bottom, dir.nativeInt);
    }

    /**
     * Add a closed oval contour to the path
     *
     * @param oval The bounds of the oval to add as a closed contour to the path
     * @param dir  The direction to wind the oval's contour
     */
    public void addOval(RectF oval, Direction dir) {
        if (oval == null) {
            throw new NullPointerException("need oval parameter");
        }
        native_addOval(mNativePath, oval, dir.nativeInt);
    }

    /**
     * Add a closed circle contour to the path
     *
     * @param x   The x-coordinate of the center of a circle to add to the path
     * @param y   The y-coordinate of the center of a circle to add to the path
     * @param radius The radius of a circle to add to the path
     * @param dir    The direction to wind the circle's contour
     */
    public void addCircle(float x, float y, float radius, Direction dir) {
        native_addCircle(mNativePath, x, y, radius, dir.nativeInt);
    }

    /**
     * Add the specified arc to the path as a new contour.
     *
     * @param oval The bounds of oval defining the shape and size of the arc
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     */
    public void addArc(RectF oval, float startAngle, float sweepAngle) {
        if (oval == null) {
            throw new NullPointerException("need oval parameter");
        }
        native_addArc(mNativePath, oval, startAngle, sweepAngle);
    }

    /**
        * Add a closed round-rectangle contour to the path
     *
     * @param rect The bounds of a round-rectangle to add to the path
     * @param rx   The x-radius of the rounded corners on the round-rectangle
     * @param ry   The y-radius of the rounded corners on the round-rectangle
     * @param dir  The direction to wind the round-rectangle's contour
     */
    public void addRoundRect(RectF rect, float rx, float ry, Direction dir) {
        if (rect == null) {
            throw new NullPointerException("need rect parameter");
        }
        native_addRoundRect(mNativePath, rect, rx, ry, dir.nativeInt);
    }
    
    /**
     * Add a closed round-rectangle contour to the path. Each corner receives
     * two radius values [X, Y]. The corners are ordered top-left, top-right,
     * bottom-right, bottom-left
     *
     * @param rect The bounds of a round-rectangle to add to the path
     * @param radii Array of 8 values, 4 pairs of [X,Y] radii
     * @param dir  The direction to wind the round-rectangle's contour
     */
    public void addRoundRect(RectF rect, float[] radii, Direction dir) {
        if (rect == null) {
            throw new NullPointerException("need rect parameter");
        }
        if (radii.length < 8) {
            throw new ArrayIndexOutOfBoundsException("radii[] needs 8 values");
        }
        native_addRoundRect(mNativePath, rect, radii, dir.nativeInt);
    }
    
    /**
     * Add a copy of src to the path, offset by (dx,dy)
     *
     * @param src The path to add as a new contour
     * @param dx  The amount to translate the path in X as it is added
     */
    public void addPath(Path src, float dx, float dy) {
        native_addPath(mNativePath, src.mNativePath, dx, dy);
    }

    /**
     * Add a copy of src to the path
     *
     * @param src The path that is appended to the current path
     */
    public void addPath(Path src) {
        native_addPath(mNativePath, src.mNativePath);
    }

    /**
     * Add a copy of src to the path, transformed by matrix
     *
     * @param src The path to add as a new contour
     */
    public void addPath(Path src, Matrix matrix) {
        native_addPath(mNativePath, src.mNativePath, matrix);
    }

    /**
     * Offset the path by (dx,dy), returning true on success
     *
     * @param dx  The amount in the X direction to offset the entire path
     * @param dy  The amount in the Y direction to offset the entire path
     * @param dst The translated path is written here. If this is null, then
     *            the original path is modified.
     */
    public void offset(float dx, float dy, Path dst) {
        SkPath dstNative = null;
        if (dst != null) {
            dstNative = dst.mNativePath;
        }
        native_offset(mNativePath, dx, dy, dstNative);
    }

    /**
     * Offset the path by (dx,dy), returning true on success
     *
     * @param dx The amount in the X direction to offset the entire path
     * @param dy The amount in the Y direction to offset the entire path
     */
    public void offset(float dx, float dy) {
        native_offset(mNativePath, dx, dy);
    }

    /**
     * Sets the last point of the path.
     *
     * @param dx The new X coordinate for the last point
     * @param dy The new Y coordinate for the last point
     */
    public void setLastPoint(float dx, float dy) {
        native_setLastPoint(mNativePath, dx, dy);
    }

    /**
     * Transform the points in this path by matrix, and write the answer
     * into dst. If dst is null, then the the original path is modified.
     *
     * @param matrix The matrix to apply to the path
     * @param dst    The transformed path is written here. If dst is null,
     *               then the the original path is modified
     */
    public void transform(Matrix matrix, Path dst) {
        SkPath dstNative = null;
        if (dst != null) {
            dstNative = dst.mNativePath;
        }
        native_transform(mNativePath, matrix, dstNative);
    }

    /**
     * Transform the points in this path by matrix.
     *
     * @param matrix The matrix to apply to the path
     */
    public void transform(Matrix matrix) {
        native_transform(mNativePath, matrix);
    }
    
    /**
     * draw the current path into a HTML5 canvas
     * @param activeCanvas
     */
    public void drawOnCanvas(String activeCanvas, Bitmap bitmap, Paint paint) {
        native_drawOnCanvas(mNativePath, activeCanvas, bitmap, paint);
    }

    protected void finalize() throws Throwable {
        try {
            //finalizer(mNativePath);
        } finally {
            super.finalize();
        }
    }
    
    /*package*/ final int ni() {
        return /*mNativePath*/ 0;
    }
    
    private class SkPath {


        /** Specifies that "inside" is computed by a non-zero sum of signed
            edge crossings
        */
        public static final int kWinding_FillType = 0;
        /** Specifies that "inside" is computed by an odd number of edge
            crossings
        */
        public static final int kEvenOdd_FillType = 1;
        /** Same as Winding, but draws outside of the path, rather than inside
        */
        public static final int kInverseWinding_FillType = 2;
        /** Same as EvenOdd, but draws outside of the path, rather than inside
         */
        public static final int kInverseEvenOdd_FillType = 3;
        
        public static final int kMove_Verb = 0;     //!< iter.next returns 1 point
        public static final int kLine_Verb = 1;     //!< iter.next returns 2 points
        public static final int kQuad_Verb = 2;     //!< iter.next returns 3 points
        public static final int kCubic_Verb = 3;    //!< iter.next returns 4 points
        public static final int kClose_Verb = 4;    //!< iter.next returns 1 point (the last point)
        public static final int kDone_Verb = 5;     //!< iter.next returns 0 points
        
        /** clockwise direction for adding closed contours */
        public static final int kCW_Direction = 0;
        /** counter-clockwise direction for adding closed contours */
        public static final int kCCW_Direction = 1;
        
        public static final float SK_PATH_KAPPA = 0.5522847498f;
        
        private ArrayList<PointF> fPts;
        private ArrayList<Integer> fVerbs;
        private boolean fBoundsIsDirty;
        private RectF fBounds;
        private int fFillType;
        
        // Workaround: J2S will rename the passed parameter, use this member to pass the 
        // canvas id to j2s native code.
        private String mCanvasId;
        private Bitmap mBitmap;

        private PointF[] mPts;
        
        SkPath() {
            fPts = new ArrayList<PointF>();
            fVerbs = new ArrayList<Integer>();
            
            fBoundsIsDirty = true;
            fFillType = kWinding_FillType;
        }
        
        SkPath(SkPath src) {
            //this = src;
        }

        public void reset() {
            fPts.clear();
            fVerbs.clear();
            
            fBoundsIsDirty = true;
        }

        public void rewind() {
            fPts.clear();
            fVerbs.clear();
            
            fBoundsIsDirty = true;
        }
        
        public void set(SkPath src) {
            if (src != this) {
                fPts = src.fPts;
                fVerbs = src.fVerbs;
                
                fFillType = src.fFillType;
                fBoundsIsDirty = src.fBoundsIsDirty;
            }
        }
        
        public boolean isEmpty() {
            int count = fVerbs.size();
            return count == 0 || (count == 1 && fVerbs.get(0) == kMove_Verb);
        }
        
        public boolean isRect(RectF rect) {
            Log.e("Path", "unimplemented");
            return false;
        }
        
        public PointF getLastPt() {
            int count = fPts.size();
            if (count == 0) {
                return new PointF(0, 0);
            } else {
                return fPts.get(count - 1);
            }
        }
        
        public void computeBounds(RectF rect) {
            // TODO: Implement
        }

        public void setLastPt(float x, float y) {
            int count = fPts.size();
            if (count == 0) {
                this.moveTo(x, y);
            } else {
                fPts.set(count - 1, new PointF(x, y));
            }
        }

        public int getFillType() {
            return fFillType;
        }

        public void setFillType(int ft) {
            if (ft != kWinding_FillType) {
                Log.e("Path", "Only Winding FillType is supported in HTML5");
            }
            this.fFillType = ft; 
        }

        public void moveTo(float x, float y) {
            int vc = fVerbs.size();
            PointF pt = new PointF(x, y);
            
            if (vc > 0 && fVerbs.get(vc - 1) == kMove_Verb) {
                fPts.set(fPts.size() - 1, pt);    
            } else {
                fPts.add(pt);
                fVerbs.add(kMove_Verb);
            }
            
            fBoundsIsDirty = true;
        }
        
        public void rMoveTo(float x, float y) {
            PointF pt = this.getLastPt();
            this.moveTo(pt.x + x, pt.y + y);
        }

        public void lineTo(float x, float y) {
            if (fVerbs.size() == 0) {
                fPts.add(new PointF(0, 0));
                fVerbs.add(kMove_Verb);
            }
            fPts.add(new PointF(x, y));
            fVerbs.add(kLine_Verb);
            
            fBoundsIsDirty = true;
        }
        
        public void rLineTo(float x, float y) {
            PointF pt = this.getLastPt();
            this.lineTo(pt.x + x, pt.y + y);
        }

        public void quadTo(float x1, float y1, float x2, float y2) {
            if (fVerbs.size() == 0) {
                fPts.add(new PointF(0, 0));
                fVerbs.add(kMove_Verb);
            }
            
            fPts.add(new PointF(x1, y1));
            fPts.add(new PointF(x2, y2));
            fVerbs.add(kQuad_Verb);
            
            fBoundsIsDirty = true;
        }
        
        public void rQuadTo(float x1, float y1, float x2, float y2) {
            PointF pt = this.getLastPt();
            this.quadTo(pt.x + x1, pt.y + y1, pt.x + x2, pt.y + y2);
        }

        public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            if (fVerbs.size() == 0) {
                fPts.add(new PointF(0, 0));
                fVerbs.add(kMove_Verb);
            }
            
            fPts.add(new PointF(x1, y1));
            fPts.add(new PointF(x2, y2));
            fPts.add(new PointF(x3, y3));
            fVerbs.add(kCubic_Verb);
            
            fBoundsIsDirty = true;
        }
        
        public void rCubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            PointF pt = this.getLastPt();
            this.cubicTo(pt.x + x1, pt.y + y1, pt.x + x2, pt.y + y2, pt.x + x3, pt.y + y3);
        }

        public void arcTo(RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo) {
            if (oval.width() < 0 || oval.height() < 0) {
                return;
            }
            
            if (fVerbs.size() == 0) {
                forceMoveTo = true;
            }
            
            // build_cubics_points build points CCW, Android use CW by default.
            ArrayList<PointF> points = build_cubics_points(oval, startAngle, -sweepAngle);
            if (forceMoveTo) {
                this.moveTo(points.get(0).x, points.get(0).y);
            } else {
                this.lineTo(points.get(0).x, points.get(0).y);
            }
            
            for (int i = 1; (i + 3) <= points.size(); i += 3) {
                this.cubicTo(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y,
                             points.get(i + 2).x, points.get(i + 2).y);
            }
            
        }
        
        /**
         * create cubic points for a defined arc
         * Reference from QT:: QStroker.cpp
         * @param rect
         * @param startAngle
         * @param sweepAngle
         * @return the point array
         */
        private ArrayList<PointF> build_cubics_points(RectF rect, float startAngle, float sweepAngle) {
            ArrayList<PointF> result = new ArrayList<PointF>();
            float x = rect.left;
            float y = rect.top;

            float w = rect.width();
            float w2 = rect.width() / 2;
            float w2k = w2 * SK_PATH_KAPPA;

            float h = rect.height();
            float h2 = rect.height() / 2;
            float h2k = h2 * SK_PATH_KAPPA;

            PointF[] points =
            {
                // start point
                new PointF(x + w, y + h2),

                // 0 -> 270 degrees
                new PointF(x + w, y + h2 + h2k),
                new PointF(x + w2 + w2k, y + h),
                new PointF(x + w2, y + h),

                // 270 -> 180 degrees
                new PointF(x + w2 - w2k, y + h),
                new PointF(x, y + h2 + h2k),
                new PointF(x, y + h2),

                // 180 -> 90 degrees
                new PointF(x, y + h2 - h2k),
                new PointF(x + w2 - w2k, y),
                new PointF(x + w2, y),

                // 90 -> 0 degrees
                new PointF(x + w2 + w2k, y),
                new PointF(x + w, y + h2 - h2k),
                new PointF(x + w, y + h2)
            };

            if (sweepAngle > 360.0f) {
                sweepAngle = 360.0f;
            } else if (sweepAngle < -360.0f) {
                sweepAngle = -360.0f;
            }

            // Special case fast paths
            if (startAngle == 0.0) {
                if (sweepAngle == 360.0) {
                    result.add(points[12]);
                    for (int i = 11; i >= 0; --i)
                        //curves[(*point_count)++] = points[i];
                        result.add(points[i]);
                    return result;
                } else if (sweepAngle == -360.0) {
                    result.add(points[0]);
                    for (int i = 1; i <= 12; ++i)
                        result.add(points[i]);
                    return result;
                }
            }

            int startSegment = (int) Math.floor(startAngle / 90);
            int endSegment = (int) Math.floor((startAngle + sweepAngle) / 90);

            float startT = (startAngle - startSegment * 90) / 90;
            float endT = (startAngle + sweepAngle - endSegment * 90) / 90;

            int delta = sweepAngle > 0 ? 1 : -1;
            if (delta < 0) {
                startT = 1 - startT;
                endT = 1 - endT;
            }

            // avoid empty start segment
            if (fuzzyIsNull(startT - 1.0f)) {
                startT = 0;
                startSegment += delta;
            }

            // avoid empty end segment
            if (fuzzyIsNull(endT)) {
                endT = 1;
                endSegment -= delta;
            }

            startT = for_arc_angle(startT * 90);
            endT = for_arc_angle(endT * 90);

            boolean splitAtStart = !fuzzyIsNull(startT);
            boolean splitAtEnd = !fuzzyIsNull(endT - 1.0f);

            int end = endSegment + delta;

            // empty arc?
            if (startSegment == end) {
                int quadrant = 3 - ((startSegment % 4) + 4) % 4;
                int j = 3 * quadrant;
                if (delta > 0) {
                    result.add(points[j + 3]);
                } else {
                    result.add(points[j]);
                }
                return result;
            }

            PointF startPoint = new PointF();
            PointF endPoint = new PointF();
            find_ellipse_coords(rect, startAngle, sweepAngle, startPoint, endPoint);
            
            result.add(startPoint);

            for (int i = startSegment; i != end; i += delta) {
                int quadrant = 3 - ((i % 4) + 4) % 4;
                int j = 3 * quadrant;

                Bezier b = new Bezier();
                if (delta > 0)
                    b.fromPoints(points[j + 3], points[j + 2], points[j + 1], points[j]);
                else
                    b.fromPoints(points[j], points[j + 1], points[j + 2], points[j + 3]);

                // empty arc?
                if (startSegment == endSegment && fuzzyCompare(startT, endT)) {
                    return result;
                }

                if (i == startSegment) {
                    if (i == endSegment && splitAtEnd)
                        b = b.bezierOnInterval(startT, endT);
                    else if (splitAtStart)
                        b = b.bezierOnInterval(startT, 1);
                } else if (i == endSegment && splitAtEnd) {
                    b = b.bezierOnInterval(0, endT);
                }

                // push control points
                result.add(new PointF(b.x2, b.y2));
                result.add(new PointF(b.x3, b.y3));
                result.add(new PointF(b.x4, b.y4));
            }

            result.set(result.size() - 1, endPoint);

            return result;

        }
        
        private class Bezier {
            private float x1, y1, x2, y2, x3, y3, x4, y4;
            Bezier() {
                
            }
            
            public void setValue(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
                this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.x3 = x3; this.y3 = y3; this.x4 = x4; this.y4 = y4;
            }
            
            public void fromPoints(PointF p1, PointF p2, PointF p3, PointF p4) {
                x1 = p1.x;
                y1 = p1.y;
                x2 = p2.x;
                y2 = p2.y;
                x3 = p3.x;
                y3 = p3.y;
                x4 = p4.x;
                y4 = p4.y;
            }
            
            public Bezier bezierOnInterval(float t0, float t1) {
                if (t0 == 0 && t1 == 1)
                    return this;
                
                Bezier bezier = new Bezier();
                bezier.setValue(x1, y1, x2, y2, x3, y3, x4, y4);
                
                Bezier result = new Bezier();
                bezier.parameterSplitLeft(t0, result);
                float trueT = (t1-t0)/(1-t0);
                bezier.parameterSplitLeft(trueT, result);
                
                return result;
            }
            
            public void parameterSplitLeft(float t, Bezier result) {
                result.x1 = x1;
                result.y1 = y1;

                result.x2 = x1 + t * ( x2 - x1 );
                result.y2 = y1 + t * ( y2 - y1 );

                result.x3 = x2 + t * ( x3 - x2 ); // temporary holding spot
                result.y3 = y2 + t * ( y3 - y2 ); // temporary holding spot

                x3 = x3 + t * ( x4 - x3 );
                y3 = y3 + t * ( y4 - y3 );

                x2 = result.x3 + t * ( x3 - result.x3);
                y2 = result.y3 + t * ( y3 - result.y3);

                result.x3 = result.x2 + t * ( result.x3 - result.x2 );
                result.y3 = result.y2 + t * ( result.y3 - result.y2 );

                result.x4 = x1 = result.x3 + t * (x2 - result.x3);
                result.y4 = y1 = result.y3 + t * (y2 - result.y3);
            }
        }
        
        
        private boolean fuzzyIsNull(float p) {
            return Math.abs(p) < 0.00001f;
        }
        
        private boolean fuzzyCompare(float p1, float p2) {
            return Math.abs(p1 - p2) < 0.00001f * Math.min(Math.abs(p1), Math.abs(p2));
        }
        private float for_arc_angle(float angle)
        {
            if (fuzzyIsNull(angle))
                return 0;

            if (fuzzyCompare(angle, 90.0f))
                return 1;

            float radians = (float) (Math.PI * angle / 180);
            float cosAngle = (float) Math.cos(radians);
            float sinAngle = (float) Math.sin(radians);

            // initial guess
            float tc = angle / 90;
            // do some iterations of newton's method to approximate cosAngle
            // finds the zero of the function b.pointAt(tc).x() - cosAngle
            tc -= ((((2-3*SK_PATH_KAPPA) * tc + 3*(SK_PATH_KAPPA-1)) * tc) * tc + 1 - cosAngle) // value
                 / (((6-9*SK_PATH_KAPPA) * tc + 6*(SK_PATH_KAPPA-1)) * tc); // derivative
            tc -= ((((2-3*SK_PATH_KAPPA) * tc + 3*(SK_PATH_KAPPA-1)) * tc) * tc + 1 - cosAngle) // value
                 / (((6-9*SK_PATH_KAPPA) * tc + 6*(SK_PATH_KAPPA-1)) * tc); // derivative

            // initial guess
            float ts = tc;
            // do some iterations of newton's method to approximate sinAngle
            // finds the zero of the function b.pointAt(tc).y() - sinAngle
            ts -= ((((3*SK_PATH_KAPPA-2) * ts -  6*SK_PATH_KAPPA + 3) * ts + 3*SK_PATH_KAPPA) * ts - sinAngle)
                 / (((9*SK_PATH_KAPPA-6) * ts + 12*SK_PATH_KAPPA - 6) * ts + 3*SK_PATH_KAPPA);
            ts -= ((((3*SK_PATH_KAPPA-2) * ts -  6*SK_PATH_KAPPA + 3) * ts + 3*SK_PATH_KAPPA) * ts - sinAngle)
                 / (((9*SK_PATH_KAPPA-6) * ts + 12*SK_PATH_KAPPA - 6) * ts + 3*SK_PATH_KAPPA);

            // use the average of the t that best approximates cosAngle
            // and the t that best approximates sinAngle
            float t = 0.5f * (tc + ts);

            return t;
        }

        private void find_ellipse_coords(RectF r, float angle, float length, PointF startPoint, PointF endPoint) {
            if (r.isEmpty()) {
                return;
            }

            float w2 = r.width() / 2;
            float h2 = r.height() / 2;

            float[] angles = { angle, angle + length };

            for (int i = 0; i < 2; ++i) {
                float theta = (float) (angles[i] - 360 * Math.floor(angles[i] / 360));
                float t = theta / 90;
                // truncate
                int quadrant = (int) (t >= 0 ? Math.floor(t) : Math.ceil(t));
                t -= quadrant;

                t = for_arc_angle(90 * t);

                // swap x and y?
                if ((quadrant & 1) != 0)
                    t = 1 - t;

                float[] coe = new float[4];
                BezierCoefficient(t, coe);
                PointF p = new PointF(coe[0] + coe[1] + coe[2]*SK_PATH_KAPPA, coe[3] + coe[2] + coe[1]*SK_PATH_KAPPA);

                // left quadrants
                if (quadrant == 1 || quadrant == 2)
                    p.x = -p.x;

                // top quadrants
                if (quadrant == 0 || quadrant == 1)
                    p.y = -p.y;

                if (i == 0) {
                    startPoint.x = r.centerX() + w2 * p.x;
                    startPoint.y = r.centerY() + h2 * p.y;
                } else {
                    endPoint.x = r.centerX() + w2 * p.x;
                    endPoint.y = r.centerY() + h2 * p.y; 
                }
            }

        }
        
        private void BezierCoefficient(float t, float[] coe) {
            float m_t = 1.0f - t;
            coe[1] = m_t * m_t;
            coe[2] = t * t;
            coe[3] = coe[2] * t;
            coe[0] = coe[1] * m_t;
            coe[1] *= 3.0f * t;
            coe[2] *= 3.0f * m_t;
        }

        public void close() {
            int count = fVerbs.size();
            if (count > 0) {
                switch (fVerbs.get(count - 1).intValue()) {
                    case kLine_Verb:
                    case kQuad_Verb:
                    case kCubic_Verb:
                        fVerbs.add(kClose_Verb);
                        break;
                    default:
                        // don't add a close if the prev wasn't a primitive
                        break;
                }
            }
        }
        
        public void addRect(float left, float top, float right, float bottom, int dir) {
            // TODO SkAutoPathBoundsUpdate apbu(this, left, top, right, bottom);
            this.moveTo(left, top);
            if (dir == kCCW_Direction) {
                this.lineTo(left, bottom);
                this.lineTo(right, bottom);
                this.lineTo(right, top);
            } else {
                this.lineTo(right, top);
                this.lineTo(right, bottom);
                this.lineTo(left, bottom);
            }
            this.close();
        }

        public void addOval(RectF oval, int dir) {
            // TODO autoPathBoundsUpdate
            float    cx = oval.centerX();
            float    cy = oval.centerY();
            float    rx = oval.width() / 2;
            float    ry = oval.height() / 2;
            float    sx = rx * SK_PATH_KAPPA;
            float    sy = ry * SK_PATH_KAPPA;

            this.moveTo(cx + rx, cy);
            if (dir == kCCW_Direction) {
                this.cubicTo(cx + rx, cy - sy, cx + sx, cy - ry, cx, cy - ry);
                this.cubicTo(cx - sx, cy - ry, cx - rx, cy - sy, cx - rx, cy);
                this.cubicTo(cx - rx, cy + sy, cx - sx, cy + ry, cx, cy + ry);
                this.cubicTo(cx + sx, cy + ry, cx + rx, cy + sy, cx + rx, cy);
            } else {
                this.cubicTo(cx + rx, cy + sy, cx + sx, cy + ry, cx, cy + ry);
                this.cubicTo(cx - sx, cy + ry, cx - rx, cy + sy, cx - rx, cy);
                this.cubicTo(cx - rx, cy - sy, cx - sx, cy - ry, cx, cy - ry);
                this.cubicTo(cx + sx, cy - ry, cx + rx, cy - sy, cx + rx, cy);
            }
            
            this.close();
        }

        public void addCircle(float x, float y, float radius, int dir) {
            if (radius > 0) {
                RectF rect = new RectF(x - radius, y - radius, x + radius, y + radius);
                this.addOval(rect, dir);
            }
        }

        public void addArc(RectF oval, float startAngle, float sweepAngle) {
            if (oval.isEmpty() || 0 == sweepAngle) {
                return;
            }
            
            if (sweepAngle >= 360f || sweepAngle <= -360f) {
                this.addOval(oval, sweepAngle > 0 ? kCW_Direction : kCCW_Direction);
            }
            
            ArrayList<PointF> points = build_cubics_points(oval, startAngle, sweepAngle);
            this.moveTo(points.get(0).x, points.get(0).y);
            
            for (int i = 1; i < points.size(); i+=3) {
                this.cubicTo(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y,
                             points.get(i + 2).x, points.get(i + 2).y);
            }
            
            this.close();
        }

        public void addRoundRect(RectF rect, float rx, float ry, int dir) {
            float    w = rect.width();
            float    halfW = w / 2;
            float    h = rect.height();
            float    halfH = h / 2;

            if (halfW <= 0 || halfH <= 0) {
                return;
            }

            boolean skip_hori = rx >= halfW;
            boolean skip_vert = ry >= halfH;

            if (skip_hori && skip_vert) {
                this.addOval(rect, dir);
                return;
            }

            // TODO :SkAutoPathBoundsUpdate apbu(this, rect);

            if (skip_hori) {
                rx = halfW;
            } else if (skip_vert) {
                ry = halfH;
            }

            float    sx = rx * 0.5522848f;
            float    sy = ry * 0.5522848f;

            this.moveTo(rect.right - rx, rect.top);
            if (dir == kCCW_Direction) {
                if (!skip_hori) {
                    this.lineTo(rect.left + rx, rect.top);       // top
                }
                this.cubicTo(rect.left + rx - sx, rect.top,
                              rect.left, rect.top + ry - sy,
                              rect.left, rect.top + ry);          // top-left
                if (!skip_vert) {
                    this.lineTo(rect.left, rect.bottom - ry);        // left
                }
                this.cubicTo(rect.left, rect.bottom - ry + sy,
                              rect.left + rx - sx, rect.bottom,
                              rect.left + rx, rect.bottom);       // bot-left
                if (!skip_hori) {
                    this.lineTo(rect.right - rx, rect.bottom);   // bottom
                }
                this.cubicTo(rect.right - rx + sx, rect.bottom,
                              rect.right, rect.bottom - ry + sy,
                              rect.right, rect.bottom - ry);      // bot-right
                if (!skip_vert) {
                    this.lineTo(rect.right, rect.top + ry);
                }
                this.cubicTo(rect.right, rect.top + ry - sy,
                              rect.right - rx + sx, rect.top,
                              rect.right - rx, rect.top);         // top-right
            } else {
                this.cubicTo(rect.right - rx + sx, rect.top,
                              rect.right, rect.top + ry - sy,
                              rect.right, rect.top + ry);         // top-right
                if (!skip_vert) {
                    this.lineTo(rect.right, rect.bottom - ry);
                }
                this.cubicTo(rect.right, rect.bottom - ry + sy,
                              rect.right - rx + sx, rect.bottom,
                              rect.right - rx, rect.bottom);      // bot-right
                if (!skip_hori) {
                    this.lineTo(rect.left + rx, rect.bottom);    // bottom
                }
                this.cubicTo(rect.left + rx - sx, rect.bottom,
                              rect.left, rect.bottom - ry + sy,
                              rect.left, rect.bottom - ry);       // bot-left
                if (!skip_vert) {
                    this.lineTo(rect.left, rect.top + ry);       // left
                }
                this.cubicTo(rect.left, rect.top + ry - sy,
                              rect.left + rx - sx, rect.top,
                              rect.left + rx, rect.top);          // top-left
                if (!skip_hori) {
                    this.lineTo(rect.right - rx, rect.top);      // top
                }
            }
            this.close();
        }

        public void addRoundRect(RectF rect, float[] rad, int dir) {
            // abort before we invoke SkAutoPathBoundsUpdate()
            if (rect.isEmpty()) {
                return;
            }

            // TODO: SkAutoPathBoundsUpdate apbu(this, rect);

            if (kCW_Direction == dir) {
                this.add_corner_arc(rect, rad[0], rad[1], 180, dir, true);
                this.add_corner_arc(rect, rad[2], rad[3], 270, dir, false);
                this.add_corner_arc(rect, rad[4], rad[5],   0, dir, false);
                this.add_corner_arc(rect, rad[6], rad[7],  90, dir, false);
            } else {
                this.add_corner_arc(rect, rad[0], rad[1], 180, dir, true);
                this.add_corner_arc(rect, rad[6], rad[7],  90, dir, false);
                this.add_corner_arc(rect, rad[4], rad[5],   0, dir, false);
                this.add_corner_arc(rect, rad[2], rad[3], 270, dir, false);
            }
            this.close();
        }

        private void add_corner_arc(RectF rect, float rx, float ry, int startAngle,
                                    int dir, boolean forceMoveTo) {
            rx = Math.min(rect.width() / 2, rx);
            ry = Math.min(rect.height() / 2, ry);

            RectF   r = new RectF(-rx, -ry, rx, ry);
            r.set(-rx, -ry, rx, ry);

            switch (startAngle) {
                case   0:
                    r.offset(rect.right - r.right, rect.bottom - r.bottom);
                    break;
                case  90:
                    r.offset(rect.left - r.left,   rect.bottom - r.bottom);
                    break;
                case 180: r.offset(rect.left - r.left,   rect.top - r.top); break;
                case 270: r.offset(rect.right - r.right, rect.top - r.top); break;
                default: Log.e("Path", "unexpected startAngle in add_corner_arc");
            }

            float start = startAngle;
            float sweep = 90;
            if (kCCW_Direction == dir) {
                start += sweep;
                sweep = -sweep;
            }

            this.arcTo(r, start, sweep, forceMoveTo);
        }

        public void addPath(SkPath src, float dx, float dy) {
            Matrix matrix = new Matrix();

            matrix.setTranslate(dx, dy);
            this.addPath(src, matrix);
        }
        
        public void addPath(SkPath src) {
            Matrix matrix = new Matrix();
            matrix.reset();
            this.addPath(src, matrix);
        }

        private void addPath(SkPath src, Matrix matrix) {
            PointF[] pts = new PointF[4];
            int verb;
            PathIter iter = new PathIter(src, false);

            while ((verb = iter.next(pts)) != kDone_Verb) {
                switch (verb) {
                    case kMove_Verb:
                        float[] mapPts0 = new float[2];
                        mapPts0[0] = pts[0].x; mapPts0[1] = pts[0].y;
                        matrix.mapPoints(mapPts0);
                        this.moveTo(mapPts0[0], mapPts0[1]);
                        break;
                    case kLine_Verb:
                        float[] mapPts1 = new float[2];
                        mapPts1[0] = pts[1].x; mapPts1[1] = pts[1].y;
                        matrix.mapPoints(mapPts1);
                        this.lineTo(mapPts1[0], mapPts1[1]);
                        break;
                    case kQuad_Verb:
                        float[] mapPts2 = new float[4];
                        mapPts2[0] = pts[1].x; mapPts2[1] = pts[1].y;
                        mapPts2[2] = pts[2].x; mapPts2[3] = pts[2].y;
                        matrix.mapPoints(mapPts2);
                        this.quadTo(mapPts2[0], mapPts2[1], mapPts2[2], mapPts2[3]);
                        break;
                    case kCubic_Verb:
                        float[] mapPts3 = new float[6];
                        mapPts3[0] = pts[1].x; mapPts3[1] = pts[1].y;
                        mapPts3[2] = pts[2].x; mapPts3[3] = pts[2].y;
                        mapPts3[4] = pts[3].x; mapPts3[5] = pts[3].y;
                        matrix.mapPoints(mapPts3);
                        this.cubicTo(mapPts3[0], mapPts3[1], mapPts3[2], mapPts3[3], mapPts3[4], mapPts3[5]);
                        break;
                    case kClose_Verb:
                        this.close();
                        break;
                    default:
                        Log.e("Path", "unknown verb");
                }
            }
        }
        
        public void transform(Matrix matrix, SkPath dst) {
            if (dst == null) {
                dst = this;
            }
            
            if ((matrix.getType() & 0x08 /*kPerspective_Mask*/) != 0) {
                Log.e("Path", "transform path with perspective has not been implemented yet!");
//                SkPath  tmp = new SkPath();
//                tmp.fFillType = fFillType;
//
//                SkPath::Iter    iter(*this, false);
//                SkPoint         pts[4];
//                SkPath::Verb    verb;
//
//                while ((verb = iter.next(pts)) != kDone_Verb) {
//                    switch (verb) {
//                        case kMove_Verb:
//                            tmp.moveTo(pts[0]);
//                            break;
//                        case kLine_Verb:
//                            tmp.lineTo(pts[1]);
//                            break;
//                        case kQuad_Verb:
//                            subdivide_quad_to(&tmp, pts);
//                            break;
//                        case kCubic_Verb:
//                            subdivide_cubic_to(&tmp, pts);
//                            break;
//                        case kClose_Verb:
//                            tmp.close();
//                            break;
//                        default:
//                            SkASSERT(!"unknown verb");
//                            break;
//                    }
//                }
//
//                dst->swap(tmp);
//                matrix.mapPoints(dst->fPts.begin(), dst->fPts.count());
            } else {
                // remember that dst might == this, so be sure to check
                // fBoundsIsDirty before we set it
                if (!fBoundsIsDirty && matrix.rectStaysRect() && fPts.size() > 1) {
                    // if we're empty, fastbounds should not be mapped
                    matrix.mapRect(dst.fBounds, fBounds);
                    dst.fBoundsIsDirty = false;
                } else {
                    dst.fBoundsIsDirty = true;
                }

                if (this != dst) {
                    dst.fVerbs = fVerbs;
                    //dst->fPts.setCount(fPts.count());
                    dst.fFillType = fFillType;
                }
                float[] dstPts = new float[fPts.size() * 2];
                float[] srcPts = new float[fPts.size() * 2];
                for (int i = 0; i < fPts.size(); i ++) {
                    srcPts[i * 2 + 0] = fPts.get(i).x;
                    srcPts[i * 2 + 1] = fPts.get(i).y;
                }
                matrix.mapPoints(dstPts, srcPts);
            }
        }

        public void drawOnCanvas(String canvas, Bitmap bitmap, Paint paint) {
            // Workaround
            this.mCanvasId = canvas;
            this.mBitmap = bitmap;
            /**
             * @j2sNative
             * var _canvas = null;
             * if (this.mCanvasId != null) {
             *     _canvas = document.getElementById(this.mCanvasId);
             * } else {
             *     _canvas = this.mBitmap.mCachedCanvas;
             *     // Update the bitmap cached canvas dirty flag
             *     this.mBitmap.mIsCachedCanvasDirty = true;
             * }
             * if (_canvas == null) {
             *     throw "Can't get canvas for this path!";
             * }
             * var ctx = _canvas.getContext("2d");
             * ctx.beginPath();
             */{}
             
             PathIter iter = new PathIter(this, false);
             this.mPts = new PointF[4];
             int verb;
             
             while ((verb = iter.next(this.mPts)) != kDone_Verb) {
                 switch (verb) {
                     case kMove_Verb:
                         /**
                          * @j2sNative
                          * ctx.moveTo(this.mPts[0].x, this.mPts[0].y);
                          */{}
                         break;
                     case kLine_Verb:
                         /**
                          * @j2sNative
                          * ctx.lineTo(this.mPts[1].x, this.mPts[1].y);
                          */{}
                         break;
                     case kQuad_Verb:
                         /**
                          * @j2sNative
                          * ctx.quadraticCurveTo(this.mPts[1].x, this.mPts[1].y, this.mPts[2].x, this.mPts[2].y);
                          */{}
                          break;
                     case kCubic_Verb:
                         /**
                          * @j2sNative
                          * ctx.bezierCurveTo(this.mPts[1].x, this.mPts[1].y, this.mPts[2].x, this.mPts[2].y, this.mPts[3].x, this.mPts[3].y);
                          */{}
                          break;
                     case kClose_Verb:
                         /**
                          * @j2sNative
                          * ctx.closePath();
                          */{}
                          break;
                      default:
                          break;
                 }
             } // while

            Style sty = paint.getStyle();
            if (Style.FILL.equals(sty)) {
                /**
                 * @j2sNative
                 * ctx.fill();
                 */{}
            } else if (Style.STROKE.equals(sty)) {
                /**
                 * @j2sNative
                 * ctx.stroke();
                 */{}
            } else if (Style.FILL_AND_STROKE.equals(sty)) {
                /**
                 * @j2sNative
                 * ctx.fill();
                 * ctx.stroke();
                 */{}
            }
        }
        
        private class PathIter {
            public static final int kAfterClose_NeedMoveToState = 0;
            public static final int kAfterCons_NeedMoveToState = 1;
            public static final int kAfterPrefix_NeedMoveToState = 2;
            
            private SkPath fPath;
            private int fPtsIndex;
            private int fVerbIndex;
            private int fVerbStop;
            private PointF fMoveTo;
            private PointF fLastPt;
            private boolean fForceClose;
            private boolean fNeedClose;
            private int fNeedMoveTo = kAfterPrefix_NeedMoveToState;
            private boolean fCloseLine;

            

            public PathIter(SkPath path, boolean forceClose) {
                this.setPath(path, forceClose);
            }
            
            private void setPath(SkPath path, boolean forceClose) {
                this.fPath = path;
                this.fPtsIndex = 0;
                this.fVerbIndex = 0;
                this.fVerbStop = path.fVerbs.size();
                this.fForceClose = forceClose;
                this.fNeedClose = false;
            }
            
            public int next(PointF[] pts) {
                if (fVerbIndex == this.fVerbStop) {
                    if (fNeedClose) {
                        if (kLine_Verb == this.autoClose(pts)) {
                            return kLine_Verb;
                        }
                        fNeedClose = false;
                        return kDone_Verb;
                    }
                    return kDone_Verb;
                }
                
                int verb = fPath.fVerbs.get(fVerbIndex++);

                switch (verb) {
                    case kMove_Verb:
                        if (fNeedClose) {
                            fVerbIndex -= 1;
                            verb = this.autoClose(pts);
                            if (verb == kClose_Verb) {
                                fNeedClose = false;
                            }
                            return verb;
                        }
                        if (fVerbIndex == fVerbStop) {    // might be a trailing moveto
                            return kDone_Verb;
                        }
                        fMoveTo = fPath.fPts.get(fPtsIndex);
                        if (pts != null) {
                            pts[0] = fMoveTo;
                        }
                        fPtsIndex += 1;
                        fNeedMoveTo = kAfterCons_NeedMoveToState;
                        fNeedClose = fForceClose;
                        break;
                    case kLine_Verb:
                        if (this.cons_moveTo(pts)) {
                            return kMove_Verb;
                        }
                        if (pts != null) {
                            pts[1] = fPath.fPts.get(fPtsIndex);
                        }
                        fLastPt = fPath.fPts.get(fPtsIndex);
                        fCloseLine = false;
                        fPtsIndex += 1;
                        break;
                    case kQuad_Verb:
                        if (this.cons_moveTo(pts)) {
                            return kMove_Verb;
                        }
                        if (pts != null) {
                            pts[1] = fPath.fPts.get(fPtsIndex);
                            pts[2] = fPath.fPts.get(fPtsIndex + 1);
                        }
                        fLastPt = fPath.fPts.get(fPtsIndex + 1);
                        fPtsIndex += 2;
                        break;
                    case kCubic_Verb:
                        if (this.cons_moveTo(pts)) {
                            return kMove_Verb;
                        }
                        if (pts != null) {
                            pts[1] = fPath.fPts.get(fPtsIndex);
                            pts[2] = fPath.fPts.get(fPtsIndex + 1);
                            pts[3] = fPath.fPts.get(fPtsIndex + 2);
                        }
                        fLastPt = fPath.fPts.get(fPtsIndex + 2);;
                        fPtsIndex += 3;
                        break;
                    case kClose_Verb:
                        verb = this.autoClose(pts);
                        if (verb == kLine_Verb) {
                            fVerbIndex -= 1;
                        } else {
                            fNeedClose = false;
                        }
                        fNeedMoveTo = kAfterClose_NeedMoveToState;
                        break;
                }
                return verb;
            }

            private boolean cons_moveTo(PointF[] pts) {
                if (fNeedMoveTo == kAfterClose_NeedMoveToState) {
                    if (pts != null) {
                        pts[0] = fMoveTo;
                    }
                    fNeedClose = fForceClose;
                    fNeedMoveTo = kAfterCons_NeedMoveToState;
                    fVerbIndex -= 1;
                    return true;
                }

                if (fNeedMoveTo == kAfterCons_NeedMoveToState) {
                    if (pts != null) {
                        pts[0] = fMoveTo;
                    }
                    fNeedMoveTo = kAfterPrefix_NeedMoveToState;
                } else {
                    if (pts != null) {
                        pts[0] = fPath.fPts.get(fPtsIndex - 1);
                    }
                }
                return false;
            }

            private int autoClose(PointF[] pts) {
                if (fLastPt.x != fMoveTo.x || fLastPt.y != fMoveTo.y) {
                    // A special case: if both points are NaN, SkPoint::operation== returns
                    // false, but the iterator expects that they are treated as the same.
                    // (consider SkPoint is a 2-dimension float point).
                    if (Float.isNaN(fLastPt.x) || Float.isNaN(fLastPt.y) ||
                        Float.isNaN(fMoveTo.x) || Float.isNaN(fMoveTo.y)) {
                        return kClose_Verb;
                    }

                    if (pts != null) {
                        pts[0] = fLastPt;
                        pts[1] = fMoveTo;
                    }
                    fLastPt = fMoveTo;
                    fCloseLine = true;
                    return kLine_Verb;
                }
                return kClose_Verb;
            }
        } // PathIter

    } // SkPath

    
    private SkPath init1() {
        return new SkPath();
    }
    
    private SkPath init2(SkPath src) {
        return new SkPath(src);
    }
    
    private static void native_reset(SkPath path) {
        path.reset();
    }
    
    private static void native_rewind(SkPath path) {
        path.rewind();
    }
    private static void native_set(SkPath path, SkPath src) {
        path.set(src);
    }
    
    private static int native_getFillType(SkPath path) {
        return path.getFillType();
    }
    
    private static void native_setFillType(SkPath path, int ft) {
        path.setFillType(ft);
    }
    
    private static boolean native_isEmpty(SkPath path) {
        return path.isEmpty();
    }
    
    private static boolean native_isRect(SkPath path, RectF rect) {
        return path.isRect(rect);
    }
    
    private static void native_computeBounds(SkPath path, RectF bounds) {
        path.computeBounds(bounds);
    }
    
    private static native void native_incReserve(int nPath, int extraPtCount);
    private static void native_moveTo(SkPath path, float x, float y) {
        path.moveTo(x, y);
    }
    private static void native_rMoveTo(SkPath path, float dx, float dy) {
        path.rMoveTo(dx, dy);
    }
    
    private static void native_lineTo(SkPath path, float x, float y) {
        path.lineTo(x, y);
    }
    
    private static void native_rLineTo(SkPath path, float dx, float dy) {
        path.rLineTo(dx, dy);
    }
    private static void native_quadTo(SkPath path, float x1, float y1,
                                             float x2, float y2) {
        path.quadTo(x1, y1, x2, y2);
    }
    private static void native_rQuadTo(SkPath path, float dx1, float dy1,
                                              float dx2, float dy2) {
        path.rQuadTo(dx1, dy1, dx2, dy2);
    }
    
    private static void native_cubicTo(SkPath path, float x1, float y1,
                                        float x2, float y2, float x3, float y3) {
        path.cubicTo(x1, y1, x2, y2, x3, y3);
    }
    
    private static void native_rCubicTo(SkPath path, float x1, float y1,
                                        float x2, float y2, float x3, float y3) {
        path.rCubicTo(x1, y1, x2, y2, x3, y3);    
    }
    
    private static void native_arcTo(SkPath path, RectF oval,
                    float startAngle, float sweepAngle, boolean forceMoveTo) {
        path.arcTo(oval, startAngle, sweepAngle, forceMoveTo);
    }
    
    private static void native_close(SkPath path) {
        path.close();
    }
    
    private static void native_addRect(SkPath path, RectF rect, int dir) {
        path.addRect(rect.left, rect.top, rect.right, rect.bottom, dir);
    }
    
    private static void native_addRect(SkPath path, float left, float top,
                                            float right, float bottom, int dir) {
        path.addRect(left, top, right, bottom, dir);
    }
    
    private static void native_addOval(SkPath path, RectF oval, int dir) {
        path.addOval(oval, dir);
    }
    
    private static void native_addCircle(SkPath path, float x, float y,
                                                float radius, int dir) {
        path.addCircle(x, y, radius, dir);
    }
    
    private static void native_addArc(SkPath path, RectF oval,
                                            float startAngle, float sweepAngle) {
        path.addArc(oval, startAngle, sweepAngle);
    }
    
    private static void native_addRoundRect(SkPath path, RectF rect,
                                                   float rx, float ry, int dir) {
        path.addRoundRect(rect, rx, ry, dir);
    }
    
    private static void native_addRoundRect(SkPath path, RectF r,
                                                   float[] radii, int dir) {
        path.addRoundRect(r, radii, dir);
    }
    
    private static void native_addPath(SkPath path, SkPath src, float dx,
                                              float dy) {
        path.addPath(src, dx, dy);
    }
    
    private static void native_addPath(SkPath path, SkPath src) {
        path.addPath(src);
    }
    
    private static void native_addPath(SkPath path, SkPath src, Matrix matrix) {
        path.addPath(src, matrix);
    }
    
    private static void native_offset(SkPath path, float dx, float dy,
                                             SkPath dst_path) {
        Matrix matrix = new Matrix();

        matrix.setTranslate(dx, dy);
        path.transform(matrix, dst_path);
    }
    
    private static void native_offset(SkPath path, float dx, float dy) {
        native_offset(path, dx, dy, path);
    }
    
    private static void native_setLastPoint(SkPath path, float dx, float dy) {
        path.setLastPt(dx, dy);
    }
    
    private static void native_transform(SkPath path, Matrix matrix,
                                         SkPath dst_path) {
        path.transform(matrix, dst_path);
    }
    
    private static void native_transform(SkPath path, Matrix matrix) {
        path.transform(matrix, path);
    }
    
    private static void native_drawOnCanvas(SkPath path, String canvas, Bitmap bitmap, Paint paint) {
        path.drawOnCanvas(canvas, bitmap, paint);
    }
    
    private static native void finalizer(int nPath);

    private final SkPath mNativePath;
}
