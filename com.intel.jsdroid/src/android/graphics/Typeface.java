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

import android.content.res.AssetManager;

import java.io.File;

import com.intel.mpt.annotation.MayloonStubAnnotation;

/**
 * The Typeface class specifies the typeface and intrinsic style of a font.
 * This is used in the paint, along with optionally Paint settings like
 * textSize, textSkewX, textScaleX to specify
 * how text appears when drawn (and measured).
 */
public class Typeface {

    /** The default NORMAL typeface object */
    public static final Typeface DEFAULT;
    /**
     * The default BOLD typeface object. Note: this may be not actually be
     * bold, depending on what fonts are installed. Call getStyle() to know
     * for sure.
     */
    public static final Typeface DEFAULT_BOLD;
    /** The NORMAL style of the default sans serif typeface. */
    public static final Typeface SANS_SERIF;
    /** The NORMAL style of the default serif typeface. */
    public static final Typeface SERIF;
    /** The NORMAL style of the default monospace typeface. */
    public static final Typeface MONOSPACE;

    private static Typeface[] sDefaults;
    

    private String familyName;
    private int style;

    // Style
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int BOLD_ITALIC = 3;

    /** Returns the typeface's intrinsic style attributes */
    public int getStyle() {
        return style;
    }
    
    public String getStyleName() {
        switch (getStyle()) {
            case 0:
                return "normal";
            case 1:
                return "bold";
            case 2:
                return "italic";
            case 3:
                return "bold italic";
            default:
                return "bold italic";
        }
    }
    
    public String getFamilyName() {
        return familyName;
    }

    /** Returns true if getStyle() has the BOLD bit set. */
    public final boolean isBold() {
        return (getStyle() & BOLD) != 0;
    }

    /** Returns true if getStyle() has the ITALIC bit set. */
    public final boolean isItalic() {
        return (getStyle() & ITALIC) != 0;
    }
    
    public Typeface(String familyName, int style) {
        this.familyName = familyName;
        this.style = style;
    }

    /**
     * Create a typeface object that best matches the specified existing
     * typeface and the specified Style. Use this call if you want to pick a new
     * style from the same family of an existing typeface object. If family is
     * null, this selects from the default font's family.
     *
     * @param family May be null. The name of the existing type face.
     * @param style  The style (normal, bold, italic) of the typeface.
     *               e.g. NORMAL, BOLD, ITALIC, BOLD_ITALIC
     * @return The best matching typeface.
     */
    public static Typeface create(Typeface family, int style) {
        return create(family.familyName, style);
    }

    /**
     * Create a typeface object given a family name, and option style information.
     * If null is passed for the name, then the "default" font will be chosen.
     * The resulting typeface object can be queried (getStyle()) to discover what
     * its "real" style characteristics are.
     *
     * @param familyName May be null. The name of the font family.
     * @param style  The style (normal, bold, italic) of the typeface.
     *               e.g. NORMAL, BOLD, ITALIC, BOLD_ITALIC
     * @return The best matching typeface.
     */
    public static Typeface create(String familyName, int style) {
        return new Typeface(familyName, style);
    }

    /**
     * Returns one of the default typeface objects, based on the specified style
     *
     * @return the default typeface that corresponds to the style
     */
    public static Typeface defaultFromStyle(int style) {
        return sDefaults[style];
    }

    // don't allow clients to call this directly
    private Typeface(int ni) {
        if (0 == ni) {
            throw new RuntimeException("native typeface cannot be made");
        }
    }
    
    static {
        DEFAULT         = create((String)null, 0);
        DEFAULT_BOLD    = create((String)null, Typeface.BOLD);
        SANS_SERIF      = create("sans-serif", 0);
        SERIF           = create("serif", 0);
        MONOSPACE       = create("monospace", 0);
        
        sDefaults = new Typeface[] {
            DEFAULT,
            DEFAULT_BOLD,
            create((String)null, Typeface.ITALIC),
            create((String)null, Typeface.BOLD_ITALIC),
        };
    }

    /**
     * @j2sNative
     * console.log("Missing method: createFromFile");
     */
    @MayloonStubAnnotation()
    public static Typeface createFromFile(String path) {
        System.out.println("Stub" + " Function : createFromFile");
        return null;
    }

    /**
     * @j2sNative
     * console.log("Missing method: finalize");
     */
    @MayloonStubAnnotation()
    protected void finalize() {
        System.out.println("Stub" + " Function : finalize");
        return;
    }

    /**
     * @j2sNative
     * console.log("Missing method: createFromFile");
     */
    @MayloonStubAnnotation()
    public static Typeface createFromFile(File path) {
        System.out.println("Stub" + " Function : createFromFile");
        return null;
    }

}
