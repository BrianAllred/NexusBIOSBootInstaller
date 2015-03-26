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

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Brian on 8/20/2014.
 */
public class FileDirManager {

    public static String ExternalFilesPath;

    public static void setExternalFilesPath(String path){
        ExternalFilesPath = path;
    }

    public static File ExternalFilesDir(){
        return new File(ExternalFilesPath);
    }

    public static void clearUnused(File dir) {
        try {
            if (dir.isDirectory()) {
                for (final File child : dir.listFiles()) {
                    if (child.getName().equals("images") || child.getName().equals("images.7z"))
                        clearFiles(child);
                }
            }
        } catch (Exception e) {
            Log.e("Error Clearing Files", e.toString());
        }
    }

    public static void clearFiles(File dir, boolean deleteRoot) {
        if (dir.isDirectory()) {
            for (final File child : dir.listFiles()) {
                clearFiles(child);
            }
            if (deleteRoot){
                try{
                    if(!dir.delete())
                        throw new IOException();
                }
                catch(IOException e){
                    Log.e("Error clearing files", e.toString());
                }
            }
        }
    }

    private static void clearFiles(File dir) {
        try {
            if (dir.isDirectory()) {
                for (final File child : dir.listFiles()) {
                    clearFiles(child);
                }
            }
            if(!dir.delete())
                throw new IOException();
        } catch (Exception e) {
            Log.e("Error Clearing Files", e.toString());
        }
    }

    public static void extractImages(AssetManager assets) {
        try {
            File mainDir = ExternalFilesDir();
            if(mainDir.mkdirs() || mainDir.exists()) {
                FileDirManager.clearFiles(mainDir, false);
                InputStream in = assets.open("images.7z");
                File outFile = new File(mainDir, "images.7z");
                if(outFile.createNewFile()) {
                    OutputStream out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    out.flush();
                    out.close();
                    in.close();
                }
                else{
                    throw new IOException();
                }
            }
            else{
                throw new IOException();
            }
        } catch (IOException e) {
            Log.e("7Zip Extract Error", e.toString());
        }
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 64];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void makeZip() {
        //get the cache directory and bootanimation file name
        String srcDir = ExternalFilesPath;
        String zipFile = srcDir + "/bootanimation.zip";
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            //zip method MUST be "STORED" for bootanimations.
            zos.setMethod(ZipEntry.STORED);
            //set up an ignore list so that the zip file doesn't keep recursively zipping
            //itself
            File ignore = new File(zipFile);
            ArrayList<String> ignoreList = new ArrayList<>(0);
            ignoreList.add(ignore.getPath());
            //actually create zip file
            File dir = new File(srcDir);
            //recursively add the files to the zip.
            //*THIS METHOD IS AWESOME AND CAN BE USED FOR OTHER THINGS
            addToZip(dir, zos, 0, ignoreList);
            //don't forget to close the zip
            zos.close();
        } catch (IOException e) {
            Log.e("MakeZIP Error", e.toString());
        }
    }

    //recursive zipping function
    private static void addToZip(File dir, ZipOutputStream zos, int depthLevel, ArrayList<String> ignore) {
        try {
            //get list of files in current directory (determined by parameter "dir"
            File[] files = dir.listFiles();
            boolean breakOut;
            //run for each file/directory
            for (File file : files) {
                breakOut = false;
                //check this file against the ignore list
                for (String s : ignore) {
                    if (s.equals(file.getPath())) {
                        //stop checking if this file matches just one entry in the ignore list
                        breakOut = true;
                        break;
                    }
                }
                //if this file has a match in the ignore list, skip it and continue on.
                if (breakOut) {
                    continue;
                }
                //call this function with an incremented depth level if the file is a directory
                if (file.isDirectory()) {
                    addToZip(file, zos, depthLevel + 1, ignore);
                }
                //if it's a normal file
                else {
                    FileInputStream fis = new FileInputStream(file.getPath());
                    //we have to get the CRC32 hash value for the file
                    //(yes, this is retarded, and yes it's required for the
                    //"STORED" method of zipping
                    long crc = getCRC32(file);
                    //use a huge buffer (modern devices have enough RAM to handle this)
                    byte[] buffer = new byte[1024 * 64];
                    String entry = file.getName();
                    File tempFile = new File(file, "");
                    //run through the full path name the amount of depth levels, that way
                    //files with depth level of 0 are in the root of the zip and files with
                    //depth n > 0 have n directories prepended to their name
                    //(basically, this recreates the folder structure of the base directory
                    //with the base directory being the root of the zip file)
                    for (int x = 0; x < depthLevel; x++) {
                        tempFile = tempFile.getParentFile();
                        entry = tempFile.getName() + File.separator + entry;
                    }
                    ZipEntry zEntry = new ZipEntry(entry);
                    //since the zip file method is "STORED", there's no compression.
                    //thus, compressed size and size are the same, and the entry's
                    //method should be "STORED" as well.
                    //Also, set the CRC32 has value because screw you, that's why.
                    zEntry.setCompressedSize(file.length());
                    zEntry.setSize(file.length());
                    zEntry.setMethod(ZipEntry.STORED);
                    zEntry.setCrc(crc);
                    //put the entry in the file
                    //(on a deeper level, this command sets the ZOS buffer
                    //to the appropriate byte for the next entry)
                    zos.putNextEntry(zEntry);
                    int length;
                    //copy the file into the ZOS buffer
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    //close the entry.
                    zos.closeEntry();
                    //close the input stream
                    fis.close();
                }
            }
        } catch (IOException e) {
            Log.e("Folder To Zip Error", e.toString());
        }
    }

    //get the hash value of a file
    private static long getCRC32(File file) {
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(file.getPath()));
            CRC32 crc = new CRC32();
            //use a huge buffer because a) modern devices can handle it and b)
            //smaller buffers make this take a LONG LONG LONG time
            byte[] buffer = new byte[1024 * 64];
            int length;
            //update the hash value for each read
            while ((length = is.read(buffer)) != -1) {
                crc.update(buffer, 0, length);
            }
            return crc.getValue();
        } catch (FileNotFoundException e) {
            Log.e("CRC Error", e.toString());
            return -1;
        } catch (IOException e) {
            Log.e("CRC Error", e.toString());
            return -1;
        }
    }

    public static void copyStock(AssetManager assets, String stock) {
        InputStream in;
        OutputStream out;
        try {
            String[] files = assets.list("");
            String stockZip = "";
            for (String file : files) {
                if (file.equals(stock)) {
                    stockZip = file;
                    break;
                }
            }
            in = assets.open(stockZip);
            File outDir = ExternalFilesDir();
            if(outDir.mkdirs() || outDir.exists()) {
                File outFile = new File(outDir, "stock.zip");
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
        } catch (IOException e) {
            Log.e("Write Assets Error", e.toString());
        }
    }

}
