package com.frozeninferno.nexusbios;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

/**
 * Created by Brian on 12/16/13.
 */

public class InstallFragment extends Fragment {

    //timer for animation preview
    private int timerCount = 0;
    private Timer timer = new Timer();

    public InstallFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_install, container, false);
        try {
            final ImageView image = (ImageView) rootView.findViewById(R.id.imageView);
            //get user's model choice
            String mod = "";
            String dev = "";
            switch (MainActivity.deviceChoice) {
                case 0:
                    dev = "N5";
                    switch (MainActivity.modelChoice) {
                        case 0:
                            mod = "16gb-d820";
                            break;
                        case 1:
                            mod = "32gb-d820";
                            break;
                        case 2:
                            mod = "16gb-d821";
                            break;
                        case 3:
                            mod = "32gb-d821";
                            break;
                    }
                    break;
                case 1:
                    dev = "N72012";
                    switch(MainActivity.modelChoice){
                        case 0:
                            mod = "8gb-grouper";
                            break;
                        case 1:
                            mod = "16gb-grouper";
                            break;
                    }
                    break;
                case 2:
                    dev = "N72012";
                    switch(MainActivity.modelChoice){
                        case 0:
                            mod = "16gb-tilapia";
                            break;
                        case 1:
                            mod = "32gb-tilapia";
                            break;
                    }
                    break;
                case 3:
                    dev = "N7";
                    switch (MainActivity.modelChoice) {
                        case 0:
                            mod = "16gb-flo";
                            break;
                        case 1:
                            mod = "32gb-flo";
                            break;
                    }
                    break;
                case 4:
                    dev = "N7";
                    mod = "32gb-deb";
                    break;
                case 5:
                    dev = "N10";
                    switch (MainActivity.modelChoice) {
                        case 0:
                            mod = "16gb";
                            break;
                        case 1:
                            mod = "32gb";
                            break;
                    }
            }
            final String model = mod;
            final String device = dev;
            final String size = model.substring(0, model.indexOf('-'));
            timerCount = 0;
            //schedule the timer to update the image.
            //simulates the animation
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if(MainActivity.filesExtracted)
                                draw(image, device, model, size);
                        }
                    });
                }//use divide 1 second over the framerate choice in order to get the update
                //frequency of the images
            }, 0, (long)1000/MainActivity.frameChoice);

            final Button install = (Button) rootView.findViewById(R.id.installButton);
            install.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    killTimer();
                    //build the dialog.
                    AlertDialog.Builder installChoice = new AlertDialog.Builder(getActivity());
                    installChoice.setTitle("Choose Installation Location (long press for more details)");
                    String[] install_choices;
                    if (MainActivity.root)
                        install_choices = new String[]{"/data/local", "/system/media"};
                    else
                        install_choices = new String[]{"/data/local", "/system/media (unavailable)"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.list_item, install_choices);
                    ListView installView = (ListView) inflater.inflate(R.layout.install_choices, container, false);
                    installView.setAdapter(adapter);
                    installChoice.setView(installView);
                    //Dialogs using listview are very different, but necessary in order to
                    //implement things like long press listeners
                    final AlertDialog installC = installChoice.create();
                    installView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, final int position, final long l) {
                            //if /system/media is selected without root, don't do anything
                            if (!MainActivity.root && position == 1) {
                                return;
                            }
                            installC.dismiss();
                            final AlertDialog.Builder installAlert = new AlertDialog.Builder(getActivity());
                            installAlert.setTitle("Install Boot Animation");
                            installAlert.setMessage("Are you sure?");
                            installAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    //ring progress dialog uses a spinning ring instead of a progress bar.
                                    //could probably use progress bar if i wanted, too lazy at the moment to care.
                                    final ProgressDialog ringProgress = ProgressDialog.show(getActivity(), "Please wait...", "Copying files...", true, false);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                String part0path = FileDirManager.ExternalFilesPath + "/images/" + device + "/part0-" + model;
                                                String part1path = FileDirManager.ExternalFilesPath + "/images/" + device + "/part1-" + size;
                                                //copy the files into cache so they can be zipped up
                                                writePart0(part0path);
                                                writePart1(part1path);
                                                writePart2(part1path);
                                                writeDesc();
                                                MainActivity.filesExtracted = false;
                                                FileDirManager.clearUnused(FileDirManager.ExternalFilesDir());
                                                //we're inside a new thread, so have to call
                                                //runOnUiThread in order to update ui assets.
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ringProgress.setMessage("Zipping files...");
                                                    }
                                                });
                                                //make the zip file
                                                FileDirManager.makeZip();
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ringProgress.setMessage("Copying zip...");
                                                    }
                                                });
                                                //move the zip where it goes
                                                moveZip(position);
                                                ringProgress.dismiss();
                                                //again, have to run on ui thread.
                                                //checks to make sure the animation installed
                                                //correctly.
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(getActivity(), doubleCheck(position) ? "Boot animation successfully installed!" : "Something went wrong!", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } catch (Exception e) {
                                                Log.e("Progress error", e.toString());
                                            }
                                            //for the love of GOD, don't forget to .start() new threads.
                                            //debugging this *sucks* just because it's so DUMB
                                        }
                                    }).start();
                                }
                            });
                            installAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            //same goes here, make sure to create the dialog from the builder
                            //and .show() it.
                            AlertDialog alert = installAlert.create();
                            alert.show();
                        }
                    });
                    installView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                            int position = (int) l;
                            AlertDialog.Builder detailsAlert = new AlertDialog.Builder(getActivity());
                            switch (position) {
                                case 0:
                                    detailsAlert.setTitle("/data/local");
                                    if (MainActivity.root)
                                        detailsAlert.setMessage(R.string.data_local_root);
                                    else
                                        detailsAlert.setMessage(R.string.data_local_non_root);
                                    break;
                                case 1:
                                    detailsAlert.setTitle("/system/media");
                                    if (MainActivity.root)
                                        detailsAlert.setMessage(R.string.system_media_root);
                                    else
                                        detailsAlert.setMessage(R.string.sysmte_media_non_root);
                                    break;
                            }
                            detailsAlert.setPositiveButton("I understand", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog detailsA = detailsAlert.create();
                            detailsA.show();
                            //true "consumes" the click (really weird way to say it provides user
                            //feedback (haptic, visual, and sound, if enabled).
                            //false just causes the long click to darken the entry.
                            return true;
                        }
                    });
                    installC.show();
                }
            });
        } catch (Exception e) {
            Log.e("Drawing Error", e.toString());
        }
        return rootView;
    }

    //double check to make sure the animation exists, that way the toast messages aren't just fluff
    private boolean doubleCheck(int position) {
        if (MainActivity.root) {
            String path = "";
            switch (position) {
                case 0:
                    path = "/data/local/bootanimation.zip";
                    break;
                case 1:
                    RootTools.remount("/system", "RW");
                    path = "/system/media/bootanimation.zip";
                    break;
            }
            boolean perms = RootTools.exists(path) && RootTools.getFilePermissionsSymlinks(path).getPermissions() == 644;
            RootTools.remount("/system", "RO");
            return perms;
        } else {
            String path = "/data/local/bootanimation.zip";
            return RootTools.exists(path);
        }
    }

    //move the zip to the appropriate directory
    private void moveZip(int which) {
        //rooted devices
        if (MainActivity.root) {
            try {
                String installPath = "";
                switch (which) {
                    case 0:
                        installPath = "/data/local/";
                        break;
                    case 1:
                        installPath = "/system/media/";
                        //have to mount /system as rw since it's mounted as RO by default
                        RootTools.remount("/system", "RW");
                        if (RootTools.exists(installPath + "bootanimation.zip")) {
                            RootTools.getShell(true).add(new CommandCapture(0, "mkdir " + installPath + "old"));
                            if (!RootTools.exists(installPath + "old/bootanimation.zip"))
                                RootTools.getShell(true).add(new CommandCapture(0, "cat " + installPath + "bootanimation.zip > " + installPath + "old/bootanimation.zip"));
                        }
                        //get rid of animation here so it doesn't conflict (we assume, of course
                        //that the user knows that /data/local overrides /system/media and doesn't
                        // want that to happen).
                        if (RootTools.exists("/data/local/bootanimation.zip")) {
                            RootTools.getShell(true).add(new CommandCapture(0, "rm -f /data/local/bootanimation.zip"));
                        }
                        break;
                }
                //actually execute the move
                RootTools.getShell(true).add(new CommandCapture(0, "cat /mnt/shell/emulated/0/Android/data/com.frozeninferno.nexusbios/files/bootanimation.zip > " + installPath + "bootanimation.zip",
                        "chmod 644 " + installPath + "bootanimation.zip"));
                //make sure to remount the system as RO if it was mounted RW. Maybe not
                //necessary, but full consequences of not doing this are unknown. Best to stay safe
                if (which == 1)
                    RootTools.remount("/system", "RO");
            } catch (IOException e) {
                Log.e("IO Exception: ROOT", e.toString());
            } catch (TimeoutException e) {
                Log.e("Timed Out: ROOT", e.toString());
            } catch (RootDeniedException e) {
                Log.e("Root Denied: ROOT", e.toString());
            }
        }
        //not rooted
        else {
            try {
                String installPath = "/data/local/";
                String bootZip = "bootanimation.zip";
                String appPath = FileDirManager.ExternalFilesPath;
                //getshell(false) calls a standard user shell
                RootTools.getShell(false).add(new CommandCapture(0, "cat " + appPath + bootZip + " > " + installPath + bootZip,
                        "chmod 644 " + installPath + bootZip));
            } catch (IOException e) {
                Log.e("IO Exception: ROOT", e.toString());
            } catch (TimeoutException e) {
                Log.e("Timed Out: ROOT", e.toString());
            } catch (RootDeniedException e) {
                Log.e("Root Denied: ROOT", e.toString());
            }
        }
    }

    //write the desc.txt needed by the boot animation.
    private void writeDesc() {
        try {
            File outDir = FileDirManager.ExternalFilesDir();
            File outFile = new File(outDir, "desc.txt");
            if(outFile.createNewFile()) {
                OutputStream outStream = new FileOutputStream(outFile);
                OutputStreamWriter out = new OutputStreamWriter(outStream);
                //use system line separator. Android is *nix, should just be \n
                //but better safe than sorry
                String eol = System.getProperty("line.separator");
                if (MainActivity.forceChoice) {
                    switch (MainActivity.deviceChoice) {
                        case 0:
                            out.write("1080 1920 " + MainActivity.frameChoice + eol + "c 1 0 part0" + eol + "c 1 0 part1" + eol + "p 0 0 part2" + eol + eol);
                            break;
                        case 1:
                        case 2:
                            out.write("800 1280 " + MainActivity.frameChoice + eol + "c 1 0 part0" + eol + "c 1 0 part1" + eol + "p 0 0 part2" + eol + eol);
                            break;
                        case 3:
                        case 4:
                        case 5:
                            out.write("1200 1920 " + MainActivity.frameChoice + eol + "c 1 0 part0" + eol + "c 1 0 part1" + eol + "p 0 0 part2" + eol + eol);
                            break;
                    }
                } else {
                    switch (MainActivity.deviceChoice) {
                        case 0:
                            out.write("1080 1920 " + MainActivity.frameChoice + eol + "p 1 0 part0" + eol + "p 1 0 part1" + eol + "p 0 0 part2" + eol + eol);
                            break;
                        case 1:
                        case 2:
                            out.write("800 1280 " + MainActivity.frameChoice + eol + "p 1 0 part0" + eol + "p 1 0 part1" + eol + "p 0 0 part2" + eol + eol);
                        case 3:
                        case 4:
                        case 5:
                            out.write("1200 1920 " + MainActivity.frameChoice + eol + "p 1 0 part0" + eol + "p 1 0 part1" + eol + "p 0 0 part2" + eol + eol);
                            break;
                    }
                }
                out.close();
            }
            else{
                throw new IOException();
            }
        } catch (IOException e) {
            Log.e("Write Desc Error", e.toString());
        }
    }

    //write the part2 part of the boot animation
    private void writePart2(String part1path) {
        try {
            File outDir = new File(FileDirManager.ExternalFilesPath + "/part2");
            if(outDir.mkdirs() || outDir.exists()) {
                for (int x = 39; x < 43; x++) {
                    InputStream in = new FileInputStream(part1path + "/00" + x + ".png");
                    File outFile = new File(outDir, "00" + x + ".png");
                    if(outFile.createNewFile()) {
                        OutputStream out = new FileOutputStream(outFile);
                        FileDirManager.copyFile(in, out);
                        out.flush();
                        out.close();
                        in.close();
                    }
                    else{
                        throw new IOException();
                    }
                }
            }
            else{
                throw new IOException();
            }
        } catch (IOException e) {
            Log.e("Write Asset Error", e.toString());
        }
    }

    //write part1 part of the bootanimation
    private void writePart1(String part1path) {
        String[] files = null;
        try {
            File file = new File(part1path);
            files = file.list();
        } catch (Exception e) {
            Log.e("Access Assets Error", e.toString());
        }
        //copy each file
        for (String filename : files) {
            InputStream in;
            OutputStream out;
            try {
                in = new FileInputStream(part1path + "/" + filename);
                File outDir = new File(FileDirManager.ExternalFilesPath + "/part1");
                //make sure the directories exist, this is a pain to debug
                if(outDir.mkdirs() || outDir.exists()) {
                    File outFile = new File(outDir, filename);
                    if(outFile.createNewFile()) {
                        out = new FileOutputStream(outFile);
                        FileDirManager.copyFile(in, out);
                        in.close();
                        out.flush();
                        out.close();
                    }
                    else{
                        throw new IOException();
                    }
                }
                else{
                    throw new IOException();
                }
            } catch (IOException | NullPointerException e) {
                Log.e("Write Assets Error", e.toString());
            }
        }
    }

    //write part0 part of the bootanimation.
    private void writePart0(String part0path) {
        String[] files = null;
        try {
            File file = new File(part0path);
            files = file.list();
        } catch (Exception e) {
            Log.e("Access Assets Error", e.toString());
        }
        for (String filename : files) {
            InputStream in;
            OutputStream out;
            try {
                in = new FileInputStream(part0path + "/" + filename);
                File outDir = new File(FileDirManager.ExternalFilesPath + "/part0");
                if(outDir.mkdirs() || outDir.exists()) {
                    File outFile = new File(outDir, filename);
                    if(outFile.createNewFile()) {
                        out = new FileOutputStream(outFile);
                        FileDirManager.copyFile(in, out);
                        in.close();
                        out.flush();
                        out.close();
                    }
                    else {
                        throw new IOException();
                    }
                }
                else{
                    throw new IOException();
                }
            } catch (IOException | NullPointerException e) {
                Log.e("Write Assets Error", e.toString());
            }
        }
    }

    //draw the images on screen for the animation preview.
    private void draw(ImageView image, String device, String model, String size) {
        try {
            String path = FileDirManager.ExternalFilesPath;
            //timercount is used to determine which of the folders to read from
            //there are 95 frames in part0, 0-94
            if (timerCount < 95) {
                if (timerCount < 10) {
                    image.setImageDrawable(Drawable.createFromPath(path + "/images/" + device + "/part0-" + model + "/000" + timerCount + ".png"));
                } else {
                    image.setImageDrawable(Drawable.createFromPath(path + "/images/" + device + "/part0-" + model + "/00" + timerCount + ".png"));
                }
                //there are 40 frames in part1
            } else if (timerCount < 135) {
                //use a separate variable so timecount is pristine
                int frame = timerCount - 95;
                if (frame < 10) {
                    image.setImageDrawable(Drawable.createFromPath(path + "/images/" + device + "/part1-" + size + "/000" + frame + ".png"));
                } else {
                    image.setImageDrawable(Drawable.createFromPath(path + "/images/" + device + "/part1-" + size + "/00" + frame + ".png"));
                }
            } else {
                if (timerCount > 138) timerCount = 135;
                int frame = timerCount - 96;
                image.setImageDrawable(Drawable.createFromPath(path + "/images/" + device + "/part1-" + size + "/00" + frame + ".png"));
            }
        } catch (Exception e) {
            Log.e("Draw Error", e.toString());
        }
        timerCount++;
    }

    //method to kill the timer since it needs to happen in a lot of circumstances
    public void killTimer() {
        try {
            timer.cancel();
            timer.purge();
            timerCount = 0;
        } catch (Exception e) {
            Log.e("Timer Error", e.toString());
        }
    }

    //kill the timer, then call the super function (else errors occur (don't ask, just do))
    public void onDetach() {
        killTimer();
        super.onDetach();
    }

    @Override
    public void onPause() {
        killTimer();
        super.onPause();
    }

    @Override
    public void onStop() {
        killTimer();
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.titleCase + 1);
    }

}
