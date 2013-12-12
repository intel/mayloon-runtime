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

package com.intel.jsdroid.home;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Slog;
import android.util.Time;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.SystemClock;

public class Home extends Activity implements OnClickListener {
    /**
     * Tag used for logging errors.
     */
    private static final String LOG_TAG = "Home>>>";
    private static boolean DEBUG = false;

    /**
     * Keys during freeze/thaw.
     */
    private static final String KEY_SAVE_GRID_OPENED = "grid.opened";

    private static final String DEFAULT_FAVORITES_PATH = "etc/favorites.xml";

    private static final String TAG_FAVORITES = "favorites";
    private static final String TAG_FAVORITE = "favorite";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_CLASS = "class";

    // Identifiers for option menu items
    private static final int MENU_WALLPAPER_SETTINGS = Menu.FIRST;
    private static final int MENU_SEARCH = MENU_WALLPAPER_SETTINGS + 1;
    private static final int MENU_SETTINGS = MENU_SEARCH + 1;

    /**
     * Maximum number of recent tasks to query.
     */
    private static final int MAX_RECENT_TASKS = 20;

    private static boolean mWallpaperChecked;
    private static ArrayList<ApplicationInfo> mApplications;
    private static LinkedList<ApplicationInfo> mFavorites;

//    private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();

    private GridView mGrid;

    private boolean mBlockAnimation;

//    private View mShowApplications;
//    private CheckBox mShowApplicationsCheck;

    private Animation mGridEntry;
    private Animation mGridExit;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        getWindow().removeBackKeyInLocalFeature();
        setContentView(R.layout.home);

        registerIntentReceivers();

        setDefaultWallpaper();

        loadApplications(true);

        bindApplications();
        bindFavorites(true);
        bindRecents();
        bindButtons();
    }
/**
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            getWindow().closeAllPanels();
        }
    }
*/
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove the callback for the cached drawables or we leak
        // the previous Home screen on orientation change
        final int count = mApplications.size();
        for (int i = 0; i < count; i++) {
            mApplications.get(i).icon.setCallback(null);
        }

//        unregisterReceiver(mWallpaperReceiver);
//        unregisterReceiver(mApplicationsReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindRecents();
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        final boolean opened = state.getBoolean(KEY_SAVE_GRID_OPENED, false);
        if (opened) {
            showApplications(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SAVE_GRID_OPENED, mGrid.getVisibility() == View.VISIBLE);
    }

    /**
     * Registers various intent receivers. The current implementation registers
     * only a wallpaper intent receiver to let other applications change the
     * wallpaper.
     */
    private void registerIntentReceivers() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
//        registerReceiver(mWallpaperReceiver, filter);

        filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
//        registerReceiver(mApplicationsReceiver, filter);
    }

    /**
     * Creates a new appplications adapter for the grid view and registers it.
     */
    private void bindApplications() {
        if (mGrid == null) {
            mGrid = (GridView) findViewById(R.id.all_apps);
        }

        if(DEBUG) System.out.println("HOME>>before set gridview");
        mGrid.setAdapter(new ApplicationsAdapter(this, mApplications));
        if(DEBUG) System.out.println("HOME>>after set gridview");
        mGrid.setSelection(0);
    }

    /**
     * Binds actions to the various buttons.
     */
    private void bindButtons() {
//        mShowApplications = findViewById(R.id.show_all_apps);
//        mShowApplications.setOnClickListener(new ShowApplications());
//        mShowApplicationsCheck = (CheckBox) findViewById(R.id.show_all_apps_check);

        mGrid.setOnItemClickListener(new ApplicationLauncher());
    }

    /**
     * When no wallpaper was manually set, a default wallpaper is used instead.
     */
    private void setDefaultWallpaper() {
        if (!mWallpaperChecked) {
            Drawable wallpaper = null;
            if (wallpaper == null) {
            } else {
//                getWindow().setBackgroundDrawable(new ClippedDrawable(wallpaper));
            }
            mWallpaperChecked = true;
        }
    }

    /**
     * Refreshes the favorite applications stacked over the all apps button.
     * The number of favorites depends on the user.
     */
    private void bindFavorites(boolean isLaunching) {
        if (!isLaunching || mFavorites == null) {

            if (mFavorites == null) {
                mFavorites = new LinkedList<ApplicationInfo>();
            } else {
                mFavorites.clear();
            }

            FileReader favReader;

            // Environment.getRootDirectory() is a fancy way of saying ANDROID_ROOT or "/system".
            /**
            final File favFile = new File(Environment.getRootDirectory(), DEFAULT_FAVORITES_PATH);
            try {
                favReader = new FileReader(favFile);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "Couldn't find or open favorites file " + favFile);
                return;
            }
*/
            final Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            final PackageManager packageManager = getPackageManager();
        }
    }

    /**
     * Refreshes the recently launched applications stacked over the favorites. The number
     * of recents depends on how many favorites are present.
     */
    private void bindRecents() {
	/**
        final PackageManager manager = getPackageManager();
        final ActivityManager tasksManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        final List<ActivityManager.RecentTaskInfo> recentTasks = tasksManager.getRecentTasks(
                MAX_RECENT_TASKS, 0);

        final int count = recentTasks.size();
        final ArrayList<ApplicationInfo> recents = new ArrayList<ApplicationInfo>();

        for (int i = count - 1; i >= 0; i--) {
            final Intent intent = recentTasks.get(i).baseIntent;

            if (Intent.ACTION_MAIN.equals(intent.getAction()) &&
                    !intent.hasCategory(Intent.CATEGORY_HOME)) {

                ApplicationInfo info = getApplicationInfo(manager, intent);
                if (info != null) {
                    info.intent = intent;
                    if (!mFavorites.contains(info)) {
                        recents.add(info);
                    }
                }
            }
        }

        mApplicationsStack.setRecents(recents);
        */
    }

    private static ApplicationInfo getApplicationInfo(PackageManager manager, Intent intent) {
        final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);

        if (resolveInfo == null) {
            return null;
        }

        final ApplicationInfo info = new ApplicationInfo();
        final ActivityInfo activityInfo = resolveInfo.activityInfo;
        info.icon = activityInfo.loadIcon(manager);
        if (info.title == null || info.title.length() == 0) {
            info.title = activityInfo.loadLabel(manager);
        }
        if (info.title == null) {
            info.title = "";
        }
        return info;
    }
/**
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            mBackDown = mHomeDown = false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    mBackDown = true;
                    return true;
                case KeyEvent.KEYCODE_HOME:
                    mHomeDown = true;
                    return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (!event.isCanceled()) {
                        // Do BACK behavior.
                    }
                    mBackDown = true;
                    return true;
                case KeyEvent.KEYCODE_HOME:
                    if (!event.isCanceled()) {
                        // Do HOME behavior.
                    }
                    mHomeDown = true;
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }
*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_WALLPAPER_SETTINGS, 0, R.string.menu_wallpaper)
                 .setIcon(android.R.drawable.ic_menu_gallery);
        menu.add(0, MENU_SEARCH, 0, R.string.menu_search)
                .setIcon(android.R.drawable.ic_search_category_default);
        menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setIntent(new Intent(android.provider.Settings.ACTION_SETTINGS));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_WALLPAPER_SETTINGS:
                startWallpaper();
                return true;
            case MENU_SEARCH:
                //onSearchRequested();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startWallpaper() {
        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        startActivity(Intent.createChooser(pickWallpaper, getString(R.string.menu_wallpaper)));
    }

    /**
     * Loads the list of installed applications in mApplications.
     */
    private void loadApplications(boolean isLaunching) {
        if (isLaunching && mApplications != null) {
            return;
        }

        Slog.v(LOG_TAG, "Start to load applications: " + Time.getCurrentTime());

        PackageManager manager = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) {
            final int count = apps.size();

            if (mApplications == null) {
                mApplications = new ArrayList<ApplicationInfo>(count);
            }
            mApplications.clear();

            for (int i = 0; i < count; i++) {
                ApplicationInfo application = new ApplicationInfo();
                ResolveInfo info = apps.get(i);

                application.title = info.loadLabel(manager);
                application.setActivity(new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                if(DEBUG) Log.i(LOG_TAG, "");
                application.icon = info.activityInfo.loadIcon(manager);
//               if(application.icon!=null)
//            	   System.out.println("application.icon is loaded correctly:"+application.icon.toString());
//               else {
//				System.out.println("application icon is not loaded correctly!");
//			}

                mApplications.add(application);
            }
        }

        Slog.v(LOG_TAG, "Finished loading applications: " + Time.getCurrentTime());
    }

    /**
     * Shows all of the applications by playing an animation on the grid.
     */
    private void showApplications(boolean animate) {
        if (mBlockAnimation) {
            return;
        }
        mBlockAnimation = true;

        /**
        mShowApplicationsCheck.toggle();

        if (mShowLayoutAnimation == null) {
            mShowLayoutAnimation = AnimationUtils.loadLayoutAnimation(
                    this, R.anim.show_applications);
        }
	*/
        // This enables a layout animation; if you uncomment this code, you need to
        // comment the line mGrid.startAnimation() below
//        mGrid.setLayoutAnimationListener(new ShowGrid());
//        mGrid.setLayoutAnimation(mShowLayoutAnimation);
//        mGrid.startLayoutAnimation();

        if (animate) {
            mGridEntry.setAnimationListener(new ShowGrid());
            mGrid.startAnimation(mGridEntry);
        }

        mGrid.setVisibility(View.VISIBLE);

        if (!animate) {
            mBlockAnimation = false;
        }

        // ViewDebug.startHierarchyTracing("Home", mGrid);
    }

    /**
     * Hides all of the applications by playing an animation on the grid.
     */
    private void hideApplications() {
        if (mBlockAnimation) {
            return;
        }
        mBlockAnimation = true;

        /**
        mShowApplicationsCheck.toggle();

        if (mHideLayoutAnimation == null) {
            mHideLayoutAnimation = AnimationUtils.loadLayoutAnimation(
                    this, R.anim.hide_applications);
        }
	*/
        mGridExit.setAnimationListener(new HideGrid());
        mGrid.startAnimation(mGridExit);
        mGrid.setVisibility(View.INVISIBLE);
        //mShowApplications.requestFocus();

        // This enables a layout animation; if you uncomment this code, you need to
        // comment the line mGrid.startAnimation() above
//        mGrid.setLayoutAnimationListener(new HideGrid());
//        mGrid.setLayoutAnimation(mHideLayoutAnimation);
//        mGrid.startLayoutAnimation();
    }

    /**
     * Receives notifications when applications are added/removed.

    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApplications(false);
            bindApplications();
            bindRecents();
            bindFavorites(false);
        }
    }
     */
    /**
     * GridView adapter to show the list of all installed applications.
     */
    private class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
        private Rect mOldBounds = new Rect();

        public ApplicationsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
            super(context, 0, apps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Slog.v(LOG_TAG, "Start to getView " + position + ": " + System.currentTimeMillis() +"ms");

            final ApplicationInfo info = mApplications.get(position);

            if (convertView == null) {
                final LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.application, parent, false);
            }

            Drawable icon = info.icon;

            if(icon!=null){
            	if(DEBUG) Log.i(LOG_TAG, "before info.filtered icon is not null");
            	icon.setBounds(0, 0, 48, 48);
            }
            else {
				if(DEBUG) Log.i(LOG_TAG, "before info.filtered icon is null");
			}

            final ImageView img=(ImageView)convertView.findViewById(R.id.imageView);
            img.setImageDrawable(icon);
            final TextView textView = (TextView) convertView.findViewById(R.id.label);
            //textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
            textView.setGravity(Gravity.CENTER);
            textView.setText(info.title);
            textView.setTag(info.intent);

            Slog.v(LOG_TAG, "Finished getView " + position + ": " + System.currentTimeMillis() +"ms");

            return convertView;
        }
    }

    /**
     * Shows and hides the applications grid view.
     */
    private class ShowApplications implements View.OnClickListener {
        public void onClick(View v) {
            if (mGrid.getVisibility() != View.VISIBLE) {
                showApplications(true);
            } else {
                hideApplications();
            }
        }
    }

    /**
     * Hides the applications grid when the layout animation is over.
     */
    private class HideGrid implements Animation.AnimationListener {
        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {
            mBlockAnimation = false;
        }

        public void onAnimationRepeat(Animation animation) {
        }
    }

    /**
     * Shows the applications grid when the layout animation is over.
     */
    private class ShowGrid implements Animation.AnimationListener {
        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {
            mBlockAnimation = false;
            // ViewDebug.stopHierarchyTracing();
        }

        public void onAnimationRepeat(Animation animation) {
        }
    }

    /**
     * Starts the selected activity/application in the grid view.
     */
    private class ApplicationLauncher implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
        	if(DEBUG) System.out.println("Home Grid Item Click>>"+position);
            ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
	    long startTime = SystemClock.uptimeMillis();
            startActivity(app.intent);
	    long endTime = SystemClock.uptimeMillis();
	    long launchTime = endTime - startTime;
	    Log.i(LOG_TAG, "-----------Application launch time is:"+launchTime);
        }
    }

	@Override
	public void onClick(View v) {
		if(DEBUG) System.out.println("Home Click>>");
		Intent intent = (Intent)v.getTag();
		startActivity(intent);
	}
}
