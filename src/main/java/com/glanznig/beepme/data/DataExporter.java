/*
This file is part of BeepMe.

BeepMe is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

BeepMe is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with BeepMe. If not, see <http://www.gnu.org/licenses/>.

Copyright 2012-2014 Michael Glanznig
http://beepme.glanznig.com
*/

package com.glanznig.beepme.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.glanznig.beepme.BeeperApp;
import com.glanznig.beepme.R;
import com.glanznig.beepme.db.StorageHandler;
import com.glanznig.beepme.helper.AsyncImageScaler;
import com.glanznig.beepme.helper.PhotoUtils;
import com.glanznig.beepme.view.MainActivity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class DataExporter {
	
	private static final String EXPORT_PREFIX = "beepme_data_";
    private static final String EXPORT_DIR = "export";
	private static final String TAG = "DataExporter";
	private static final int BUFFER = 2048;
	private static final int NOTIFICATION_ID = 1438;
	Context ctx;
	
	public DataExporter(Context context) {
		ctx = context;
	}
	
	public String exportToZipFile(boolean exportPhotos) {
        return exportToZipFile(exportPhotos, 1);
    }

    public String exportToZipFile(boolean exportPhotos, int densityFactor) {
		//external storage is ready and writable - can be used
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			File exportDir = ctx.getExternalFilesDir(EXPORT_DIR);
			if (!exportDir.exists()) {
				exportDir.mkdirs();
			}
            BeeperApp app = (BeeperApp)ctx.getApplicationContext();

			String exportFilename = EXPORT_PREFIX;
            if (app.getPreferences().isTestMode()) {
                exportFilename += "testmode_";
            }
            exportFilename += new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime()) + ".zip";
			File exportFile = new File(exportDir, exportFilename);
			ArrayList<File> fileList = new ArrayList<File>();
            String archive = null;

            String dbName;
            File picDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            if (app.getPreferences().isTestMode()) {
                dbName = StorageHandler.getTestModeDatabaseName();
                picDir = new File(picDir, PhotoUtils.TEST_MODE_DIR);
            }
            else {
                dbName = StorageHandler.getProductionDatabaseName();
                picDir = new File(picDir, PhotoUtils.NORMAL_MODE_DIR);
            }
            fileList.add(ctx.getDatabasePath(dbName));

			if (exportPhotos) {
                if (picDir.exists()) {
                    FilenameFilter filter = new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            return fileName.endsWith(".jpg");
                        }
                    };
                    File[] photos = picDir.listFiles(filter);

                    if (densityFactor > 1) {
                        // downscale photos
                        String tempDirName = "scaled";
                        File scaledDir = new File(picDir, tempDirName);
                        scaledDir.mkdirs();

                        for (int i = 0; i < photos.length; i++) {
                            String srcUri = photos[i].getAbsolutePath();

                            File destPhoto = new File(srcUri);
                            String path = destPhoto.getParent() + File.separator + tempDirName;
                            destPhoto = new File(path, destPhoto.getName());

                            String destUri = destPhoto.getAbsolutePath();
                            Bundle dim = PhotoUtils.getPhotoDimensions(srcUri);
                            int width = dim.getInt("width");
                            int height = dim.getInt("height");

                            if (width > 0 && height > 0) {
                                Bitmap scaledPhoto = PhotoUtils.scalePhoto(srcUri, destUri,
                                        (int)Math.round(width / Math.sqrt((double)densityFactor)),
                                        (int)Math.round(height / Math.sqrt((double)densityFactor)));

                                if (scaledPhoto != null) {
                                    scaledPhoto.recycle();
                                    fileList.add(destPhoto);
                                }
                            }
                        }

                        archive = zipFiles(exportFile, fileList);

                        // remove temp dir and contents
                        File[] tempFiles;
                        tempFiles = scaledDir.listFiles();
                        for (int i=0; i < tempFiles.length; i++) {
                            tempFiles[i].delete();
                        }
                        scaledDir.delete();

                    } else {
                        for (int i = 0; i < photos.length; i++) {
                            fileList.add(photos[i]);
                        }

                        archive = zipFiles(exportFile, fileList);
                    }
                }
			}
            else {
                archive = zipFiles(exportFile, fileList);
            }

			return archive;
		}
		
		return null;
	}
	
	private String zipFiles(File zipFile, List<File> fileList) {
		String path = null;
		
		if (zipFile != null && fileList != null) {
			try {
				BufferedInputStream bufIn = null;
				FileOutputStream zipFOutStream = new FileOutputStream(zipFile);
				ZipOutputStream outStream = new ZipOutputStream(new BufferedOutputStream(zipFOutStream));
				
				byte data[] = new byte[BUFFER];
				Iterator<File> i = fileList.iterator();
				
				while (i.hasNext()) {
					File f = i.next();
					FileInputStream fIn = new FileInputStream(f);
					bufIn = new BufferedInputStream(fIn, BUFFER);
					ZipEntry entry = new ZipEntry(f.getName());
					outStream.putNextEntry(entry);
					int count;
					while ((count = bufIn.read(data, 0, BUFFER)) != -1) {
						outStream.write(data, 0, count);
					}
					bufIn.close();
				}
				outStream.close();
				
				path = zipFile.getAbsolutePath();
				
			} catch(Exception e) {
				Log.e(TAG, "error while zipping.", e);
			}
		}

		return path;
	}

    public double getArchiveSize(boolean exportPhotos, int densityFactor) {
        BeeperApp app = (BeeperApp)ctx.getApplicationContext();
        File db;
        double archiveSize = 0;

        if (!app.getPreferences().isTestMode()) {
            db = app.getDatabasePath(StorageHandler.getTestModeDatabaseName());
        }
        else {
            db = app.getDatabasePath(StorageHandler.getProductionDatabaseName());
        }

        if (db != null) {
            archiveSize += db.length();
        }

        if (exportPhotos) {
            File[] photoList = PhotoUtils.getPhotos(ctx);
            if (photoList != null) {
                int count;
                double photoOverallSize = 0;
                for (count = 0; count < photoList.length; count++) {
                    photoOverallSize += photoList[count].length();
                }
                double photoAvgSize;
                if (count > 0) {
                    photoAvgSize = photoOverallSize / count;
                }
                else {
                    photoAvgSize = 0;
                }

                if (densityFactor == 1) {
                    archiveSize += photoOverallSize;
                }
                else {
                    archiveSize += photoAvgSize / densityFactor * count;
                }
            }
        }

        return archiveSize;
    }

    public String getReadableArchiveSize(boolean exportPhotos, int densityFactor) {
        double size = getArchiveSize(exportPhotos, densityFactor);
        return getReadableFileSize(size, 0);
    }

    public static String getReadableFileSize(double size, int decimals) {
        if(size <= 0) {
            return "0 KB";
        }

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        NumberFormat numFormat = DecimalFormat.getInstance();
        numFormat.setMaximumFractionDigits(decimals);

        return numFormat.format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
