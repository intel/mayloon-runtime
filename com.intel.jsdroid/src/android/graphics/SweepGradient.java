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

public class SweepGradient extends Shader {
    private PointF m_center;
    private int[] m_colors;
    private float[] m_positions;

    /**
     * A subclass of Shader that draws a sweep gradient around a center point.
     *
     * @param cx       The x-coordinate of the center
     * @param cy       The y-coordinate of the center
     * @param colors   The colors to be distributed between around the center.
     *                 There must be at least 2 colors in the array.
     * @param positions May be NULL. The relative position of
     *                 each corresponding color in the colors array, beginning
     *                 with 0 and ending with 1.0. If the values are not
     *                 monotonic, the drawing may produce unexpected results.
     *                 If positions is NULL, then the colors are automatically
     *                 spaced evenly.
     */
    public SweepGradient(float cx, float cy,
                         int colors[], float positions[]) {
        if (colors.length < 2) {
            throw new IllegalArgumentException("needs >= 2 number of colors");
        }
        if (positions != null && colors.length != positions.length) {
            throw new IllegalArgumentException(
                        "color and position arrays must be of equal length");
        }
        m_center = new PointF(cx, cy);
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
        //native_instance = nativeCreate1(cx, cy, colors, positions);
    }

    /**
     * A subclass of Shader that draws a sweep gradient around a center point.
     *
     * @param cx       The x-coordinate of the center
     * @param cy       The y-coordinate of the center
     * @param color0   The color to use at the start of the sweep
     * @param color1   The color to use at the end of the sweep
     */
    public SweepGradient(float cx, float cy, int color0, int color1) {
        m_center = new PointF(cx, cy);
        m_colors = new int[2];
        m_colors[0] = color0; m_colors[1] = color1;
        //native_instance = nativeCreate2(cx, cy, color0, color1);
    }
}