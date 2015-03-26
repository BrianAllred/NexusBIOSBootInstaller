/*
 * Copyright (C) 2013 Brian D. Allred
 *
 *     This software source code is protected by copyright
 *     and may not be used, modified, or distributed
 *     without my permission.
 *
 *     All rights reserved.
 */

package com.frozeninferno.nexusbios;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;


/*
    This app is simple enough to only require one activity.
    The MainActivity builds the fragments of the app as necessary.
*/

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    //incremented or decremented by fragments as necessary in order
    //for MainActivity to display the correct fragment title in the
    //actionbar.
    public static int titleCase = 0;

    //save user's model choice
    public static int modelChoice = 0;

    //save user's frame rate choice
    public static int frameChoice = 5;

    //save user's device choice
    public static int deviceChoice = 0;

    //save user's force play choice
    public static boolean forceChoice = false;

    //used in conjunction with titleCase in order to help
    //MainActivity display the correct title
    public static int drawer = 0;

    //save user's root state
    public static boolean root = false;

    //set whether files have been extracted
    public static boolean filesExtracted = false;

    private static boolean rootSet = false;


    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        try {
            String s = getExternalFilesDir(null).getPath();
            FileDirManager.setExternalFilesPath(s);
        }
        catch(Exception e){
            Log.e("Error getting external path", e.toString());
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        titleCase = 0;
        switch (position) {
            //create the appropriate fragment depending on choice from drawer
            case 0:
                drawer = 0;
                fragmentManager.beginTransaction()
                        .replace(R.id.container, WelcomeFragment.newInstance(position))
                        .commit();
                break;
            case 1:
                if(!rootSet) {
                    Toast.makeText(getApplicationContext(), "Please wait for root check...", Toast.LENGTH_SHORT);
                    return;
                }
                fragmentManager.beginTransaction()
                        .replace(R.id.container, PreviewFragment.newInstance(position))
                        .commit();
                break;
            case 2:
                if(!rootSet) {
                    Toast.makeText(getApplicationContext(), "Please wait for root check...", Toast.LENGTH_SHORT);
                    return;
                }
                drawer = 1;
                fragmentManager.beginTransaction()
                        .replace(R.id.container, UninstallFragment.newInstance())
                        .commit();
        }
    }

    public void onSectionAttached(int number) {
        //restore the actionbar after any fragment is created in order
        //to update the title
        switch (number) {
            case 0:
                switch (drawer) {
                    case 0:
                        mTitle = getString(R.string.title_section1);
                        //check root
                        if (rootSet)
                            break;
                        //run root check in separate thread due to intensity of su
                        final ProgressDialog ringProgress = ProgressDialog.show(this, "Please wait...", "Acquiring root...", true, false);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (RootTools.isRootAvailable() && RootTools.isAccessGiven()) {
                                    //run Toast on UI thread because it's a UI element
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Root access granted!", Toast.LENGTH_SHORT).show();
                                            root = true;
                                            rootSet = true;
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Root access denied! App needs root to function!", Toast.LENGTH_SHORT).show();
                                            root = false;
                                            rootSet = false;
                                            finish();
                                        }
                                    });
                                }
                                ringProgress.dismiss();
                            }
                        }).start();
                        break;
                    case 1:
                        mTitle = getString(R.string.title_uninstall);
                        break;
                }
                break;
            case 1:
                mTitle = "Select Device";
                break;
            case 2:
                mTitle = getString(R.string.title_model);
                break;
            case 3:
                mTitle = getString(R.string.title_fps);
                break;
            case 4:
                mTitle = getString(R.string.title_preview);
                break;
            case 5:
                mTitle = getString(R.string.title_install_1);
                break;
        }
        restoreActionBar();
    }

    //sets the title text on the Action Bar
    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //exit app if fragment is one of the root fragments, otherwise, create
        //the previous fragment
        if (titleCase <= 0) {
            finish();
        } else if (titleCase <= 3) {
            if (titleCase == 2 && deviceChoice == 4) {
                titleCase -= 1;
            }
            titleCase -= 1;
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new PreviewFragment())
                    .commit();
        }
    }

    //Catch preview fragment's check box click
    public void onCheckboxClicked(View view) {
        forceChoice = ((CheckBox) view).isChecked();
    }

}
