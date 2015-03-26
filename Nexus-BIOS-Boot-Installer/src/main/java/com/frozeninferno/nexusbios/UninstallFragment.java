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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by Brian on 12/17/13.
 */
public class UninstallFragment extends Fragment {

    private String stock = "";

    public UninstallFragment() {

    }

    public static Fragment newInstance() {
        return new UninstallFragment();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_uninstall, container, false);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.list_item, new String[]{"Cleanup", "Uninstall"});
        ListView uninstallList = (ListView) rootView.findViewById(R.id.uninstallList);
        uninstallList.setAdapter(adapter);
        uninstallList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                switch (i) {
                    case 0:
                        alertBuilder.setTitle("Confirm Cleanup");
                        alertBuilder.setMessage("This will clear the app's cache, which it can rebuild again later, and then shut down the app. Continue?");
                        alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        FileDirManager.clearFiles(FileDirManager.ExternalFilesDir(), false);
                                        getActivity().finish();
                                    }
                                }).start();
                                Toast.makeText(getActivity(), "Cache Cleared!", Toast.LENGTH_SHORT).show();
                            }
                        });
                        alertBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        AlertDialog alert1 = alertBuilder.create();
                        alert1.show();
                        break;
                    case 1:
                        alertBuilder.setTitle("Choose Device");
                        alertBuilder.setMessage("Choose which device you're using.");
                        String[] devices = new String[]{"Nexus 5", "Nexus 7 2013", "Nexus 10"};
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.list_item, devices);
                        ListView devicesList = (ListView) inflater.inflate(R.layout.install_choices, container, false);
                        devicesList.setAdapter(adapter);
                        alertBuilder.setView(devicesList);
                        final AlertDialog alert = alertBuilder.create();
                        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                                switch (position) {
                                    case 0:
                                        stock = "n5-stock.zip";
                                        break;
                                    case 1:
                                        stock = "n7-stock.zip";
                                        break;
                                    case 2:
                                        stock = "n10-stock.zip";
                                        break;
                                }
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                                alertBuilder.setTitle("Installation Method Used");
                                alertBuilder.setMessage("Choose which installation method was used in order to restore backup correctly.");
                                String[] methods;
                                if (MainActivity.root)
                                    methods = new String[]{"/data/local/", "/system/media/"};
                                else
                                    methods = new String[]{"/data/local/", "/system/media/ (unavailable)"};
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.list_item, methods);
                                ListView methodList = (ListView) inflater.inflate(R.layout.install_choices, container, false);
                                methodList.setAdapter(adapter);
                                alertBuilder.setView(methodList);
                                final AlertDialog alert2 = alertBuilder.create();
                                methodList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
                                        if (!MainActivity.root && position == 1) {
                                            return;
                                        }
                                        alert2.dismiss();
                                        AlertDialog.Builder uninstallAlert = new AlertDialog.Builder(getActivity());
                                        uninstallAlert.setTitle("Uninstall Boot Animation");
                                        uninstallAlert.setMessage("This will uninstall the boot animation from " + (position == 0 ? "/data/local/" : "/system/media/") + ". Continue?");
                                        uninstallAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                                if (MainActivity.root) {
                                                    AlertDialog.Builder stockAlert = new AlertDialog.Builder(getActivity());
                                                    stockAlert.setTitle("Revert to Stock");
                                                    stockAlert.setMessage("Would you like to install the stock animation to /system/media and erase any animations from /data/local? (Useful if something's broken or you'd just prefer stock over your back up)");
                                                    stockAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                            final ProgressDialog ringProgress = ProgressDialog.show(getActivity(), "Please wait...", "Uninstalling custom animation and restoring stock...", true, false);
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    uninstall(position, true);
                                                                    ringProgress.dismiss();
                                                                    getActivity().runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Toast.makeText(getActivity(), doubleCheck() ? "Stock animation successfully restored!" : "Something went wrong!", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                }
                                                            }).start();
                                                        }
                                                    });
                                                    stockAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                            final ProgressDialog ringProgress = ProgressDialog.show(getActivity(), "Please wait...", "Uninstalling custom animation and restoring backup...", true, false);
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    uninstall(position, false);
                                                                    ringProgress.dismiss();
                                                                    getActivity().runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Toast.makeText(getActivity(), doubleCheck() ? "Backed up animation successfully restored!" : "Something went wrong!", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                }
                                                            }).start();
                                                        }
                                                    });
                                                    AlertDialog stockA = stockAlert.create();
                                                    stockA.show();
                                                } else {
                                                    final ProgressDialog ringProgress = ProgressDialog.show(getActivity(), "Please wait...", "Uninstalling custom animation...", true, false);
                                                    new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            uninstall(position, false);
                                                            ringProgress.dismiss();
                                                            getActivity().runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(getActivity(), doubleCheck() ? "Animation successfully removed!" : "Something went wrong!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    }).start();
                                                }
                                            }
                                        });
                                        uninstallAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        });
                                        AlertDialog uninstallA = uninstallAlert.create();
                                        uninstallA.show();
                                    }
                                });
                                alert.dismiss();
                                alert2.show();
                            }
                        });
                        alert.show();
                        break;
                }
            }
        });
        return rootView;
    }

    private boolean doubleCheck() {
        if (MainActivity.root) {
            String path = "/system/media/bootanimation.zip";
            boolean perms;
            RootTools.remount("/system", "RW");
            perms = RootTools.exists(path) && RootTools.getFilePermissionsSymlinks(path).getPermissions() == 644;
            RootTools.remount("/system", "RO");
            return perms;
        } else {
            String path = "/data/local/bootanimation.zip";
            return !RootTools.exists(path);
        }
    }

    private void uninstall(int which, boolean stock) {
        if (MainActivity.root) {
            try {
                String installPath;
                switch (which) {
                    case 0:
                        installPath = "/data/local/";
                        if (RootTools.exists(installPath + "bootanimation.zip")) {
                            RootTools.getShell(true).add(new CommandCapture(0, "rm -f " + installPath + "bootanimation.zip"));
                        }

                        break;
                    case 1:
                        installPath = "/system/media/";
                        if (RootTools.exists(installPath + "bootanimation.zip")) {
                            RootTools.remount("/system", "RW");
                            RootTools.getShell(true).add(new CommandCapture(0, "rm -f " + installPath + "bootanimation.zip"));
                            if (RootTools.exists(installPath + "old/bootanimation.zip")) {
                                RootTools.getShell(true).add(new CommandCapture(1, "cat " + installPath + "old/bootanimation.zip > " + installPath,
                                        "chmod 644 " + installPath + "bootanimation.zip",
                                        "rm -r " + installPath + "/old"));
                            }
                        }
                        if (!RootTools.exists(installPath + "bootanimation.zip")) {
                            RootTools.remount("/system", "RW");
                            FileDirManager.copyStock(getActivity().getAssets(), this.stock);
                            RootTools.getShell(true).add(new CommandCapture(0, "cat /mnt/shell/emulated/0/Android/data/com.frozeninferno.nexusbios/files/stock.zip > /system/media/bootanimation.zip",
                                    "chmod 644 /system/media/bootanimation.zip"));
                        }
                        RootTools.remount("/system", "RO");
                        break;
                }
                if (stock) {
                    RootTools.remount("/system", "RW");
                    if (RootTools.exists("/data/local/bootanimation.zip")) {
                        RootTools.getShell(true).add(new CommandCapture(0, "rm -f /data/local/bootanimation.zip"));
                    }
                    RootTools.getShell(true).add(new CommandCapture(1, "rm -f -r /data/local/old/"));
                    if (RootTools.exists("/system/media/bootanimation.zip")) {
                        RootTools.getShell(true).add(new CommandCapture(2, "rm -f /system/media/bootanimation.zip"));
                    }
                    FileDirManager.copyStock(getActivity().getAssets(), this.stock);
                    RootTools.getShell(true).add(new CommandCapture(3, "rm -f -r /system/media/old/",
                            "cat /mnt/shell/emulated/0/Android/data/com.frozeninferno.nexusbios/files/stock.zip > /system/media/bootanimation.zip",
                            "chmod 644 /system/media/bootanimation.zip"));
                    RootTools.remount("/system", "RO");
                }
            } catch (IOException e) {
                Log.e("IO Exception: ROOT", e.toString());
            } catch (TimeoutException e) {
                Log.e("Timed Out: ROOT", e.toString());
            } catch (RootDeniedException e) {
                Log.e("Root Denied: ROOT", e.toString());
            }
        } else {
            try {
                String installPath = "/data/local/";
                String bootZip = "bootanimation.zip";
                RootTools.getShell(false).add(new CommandCapture(0, "rm -f " + installPath + bootZip));
            } catch (IOException e) {
                Log.e("IO Exception: ROOT", e.toString());
            } catch (TimeoutException e) {
                Log.e("Timed Out: ROOT", e.toString());
            } catch (RootDeniedException e) {
                Log.e("Root Denied: ROOT", e.toString());
            }
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.titleCase);
    }
}
