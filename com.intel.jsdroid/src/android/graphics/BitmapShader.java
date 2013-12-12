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

public class BitmapShader extends Shader {

    // we hold on just for the GC, since our native counterpart is using it
    private Bitmap mBitmap;
    private TileMode m_tileX, m_tileY;
    private String mTileMode = null;

    /**
     * Call this to create a new shader that will draw with a bitmap.
     *
     * @param bitmap            The bitmap to use inside the shader
     * @param tileX             The tiling mode for x to draw the bitmap in.
     * @param tileY             The tiling mode for y to draw the bitmap in.
     */
    public BitmapShader(Bitmap bitmap, TileMode tileX, TileMode tileY) {
        mBitmap = bitmap;
        m_tileX = tileX; m_tileY = tileY;
        // HTML5 createPattern only supports repeat or no-repeat, not including mirror.
        if (tileX == TileMode.REPEAT && tileY == TileMode.REPEAT) {
            mTileMode = "repeat";
        } else if (tileX == TileMode.REPEAT && tileY == TileMode.CLAMP) {
            mTileMode = "repeat-x";
        } else if (tileX == TileMode.CLAMP && tileY == TileMode.REPEAT) {
            mTileMode = "repeat-y";
        } else if (tileX == TileMode.CLAMP && tileY == TileMode.CLAMP) {
            mTileMode = "no-repeat";
        }
    }
}