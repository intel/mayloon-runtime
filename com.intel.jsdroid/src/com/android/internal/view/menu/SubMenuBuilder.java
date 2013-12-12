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

package com.android.internal.view.menu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.SubMenu;
import android.view.View;

/**
 * Subclass of {@link MenuBuilder} for sub menus.
 * <p>
 * Sub menus do not support item icons, or nested sub menus.
 */

public class SubMenuBuilder extends MenuBuilder implements SubMenu {
    private MenuBuilder mParentMenu;
    private MenuItemImpl mItem;
    private Context mContext;

    public SubMenuBuilder(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    protected SubMenuBuilder(Context context, MenuBuilder parentMenu,
            MenuItemImpl item) {
        super(context);
        mParentMenu = parentMenu;
        mItem = item;
    }

    /**
     * Sets the submenu header's title to the title given in <var>titleRes</var>
     * resource identifier.
     * 
     * @param titleRes
     *            The string resource identifier used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenuBuilder setHeaderTitle(int titleRes) {
        return (SubMenuBuilder) super.setHeaderTitleInt(titleRes);
    }

    /**
     * Sets the submenu header's title to the title given in <var>title</var>.
     * 
     * @param title
     *            The character sequence used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenuBuilder setHeaderTitle(CharSequence title) {
        return (SubMenuBuilder) super.setHeaderTitleInt(title);
    }

    /**
     * Sets the submenu header's icon to the icon given in <var>iconRes</var>
     * resource id.
     * 
     * @param iconRes
     *            The resource identifier used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenuBuilder setHeaderIcon(int iconRes) {
        return (SubMenuBuilder) super.setHeaderIconInt(iconRes);
    }

    /**
     * Sets the submenu header's icon to the icon given in <var>icon</var>
     * {@link Drawable}.
     * 
     * @param icon
     *            The {@link Drawable} used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenuBuilder setHeaderIcon(Drawable icon) {
        return (SubMenuBuilder) super.setHeaderIconInt(icon);
    }

    /**
     * Sets the header of the submenu to the {@link View} given in
     * <var>view</var>. This replaces the header title and icon (and those
     * replace this).
     * 
     * @param view
     *            The {@link View} used for the header.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderView(View view) {
        return (SubMenu) super.setHeaderViewInt(view);
    }

    /**
     * Clears the header of the submenu.
     */
    public void clearHeader() {
    }

    /**
     * Change the icon associated with this submenu's item in its parent menu.
     * 
     * @see MenuItemImpl#setIcon(int)
     * @param iconRes
     *            The new icon (as a resource ID) to be displayed.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenuBuilder setIcon(int iconRes) {
        mItem.setIcon(iconRes);
        return this;
    }

    /**
     * Change the icon associated with this submenu's item in its parent menu.
     * 
     * @see MenuItemImpl#setIcon(Drawable)
     * @param icon
     *            The new icon (as a Drawable) to be displayed.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenuBuilder setIcon(Drawable icon) {
        mItem.setIcon(icon);
        return this;
    }

    /**
     * Gets the {@link MenuItemImpl} that represents this submenu in the parent
     * menu. Use this for setting additional item attributes.
     * 
     * @return The {@link MenuItemImpl} that launches the submenu when invoked.
     */
    public MenuItemImpl getItem() {
        return mItem;
    }

    public void setmParentMenu(MenuBuilder mParentMenu) {
        this.mParentMenu = mParentMenu;
    }

    public void setmItem(MenuItemImpl mItem) {
        this.mItem = mItem;
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        mParentMenu.setQwertyMode(isQwerty);
    }

    @Override
    public boolean isQwertyMode() {
        return mParentMenu.isQwertyMode();
    }

    // @Override
    // public boolean isShortcutsVisible() {
    // return mParentMenu.isShortcutsVisible();
    // }

    public MenuBuilder getParentMenu() {
        return mParentMenu;
    }

    @Override
    public Callback getCallback() {
        return mParentMenu.getCallback();
    }

    @Override
    public void setCallback(Callback callback) {
        mParentMenu.setCallback(callback);
    }

    @Override
    public MenuBuilder getRootMenu() {
        return mParentMenu;
    }
}
