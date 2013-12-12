/*
 * Copyright (C) 2007 The Android Open Source Project
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

public class LinearGradient extends Shader {
    public PointF m_pts0, m_pts1;
    public int[] m_colors;
    public float[] m_positions;
    public TileMode m_tileMode;

    /** Create a shader that draws a linear gradient along a line.
        @param x0           The x-coordinate for the start of the gradient line
        @param y0           The y-coordinate for the start of the gradient line
        @param x1           The x-coordinate for the end of the gradient line
        @param y1           The y-coordinate for the end of the gradient line
        @param  colors      The colors to be distributed along the gradient line
        @param  positions   May be null. The relative positions [0..1] of
                            each corresponding color in the colors array. If this is null,
                            the the colors are distributed evenly along the gradient line.
        @param  tile        The Shader tiling mode
    */
    public LinearGradient(float x0, float y0, float x1, float y1,
                          int colors[], float positions[], TileMode tile) {
        if (colors.length < 2) {
            throw new IllegalArgumentException("needs >= 2 number of colors");
        }
        if (positions != null && colors.length != positions.length) {
            throw new IllegalArgumentException("color and position arrays must be of equal length");
        }

        m_pts0 = new PointF(x0, y0);
        m_pts1 = new PointF(x1, y1);
        
        m_tileMode = tile;
        
        m_colors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            m_colors[i] = colors[i];
        }
        
        
        if (positions != null) {
            m_positions = new float[positions.length];
            for (int i = 0; i < positions.length; i++) {
                m_positions[i] = positions[i];
            }
        }
        //native_instance = nativeCreate1(x0, y0, x1, y1, colors, positions, tile.nativeInt);
    }

    /** Create a shader that draws a linear gradient along a line.
        @param x0       The x-coordinate for the start of the gradient line
        @param y0       The y-coordinate for the start of the gradient line
        @param x1       The x-coordinate for the end of the gradient line
        @param y1       The y-coordinate for the end of the gradient line
        @param  color0  The color at the start of the gradient line.
        @param  color1  The color at the end of the gradient line.
        @param  tile    The Shader tiling mode
    */
    public LinearGradient(float x0, float y0, float x1, float y1,
                          int color0, int color1, TileMode tile) {
        m_pts0 = new PointF(x0, y0);
        m_pts1 = new PointF(x1, y1);
        
        m_tileMode = tile;
        
        m_colors = new int[2];
        m_colors[0] = color0; m_colors[1] = color1;
        //native_instance = nativeCreate2(x0, y0, x1, y1, color0, color1, tile.nativeInt);
    }
}