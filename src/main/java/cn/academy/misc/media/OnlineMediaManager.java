/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import com.typesafe.config.ConfigFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author KSkun
 */
public class OnlineMediaManager extends MediaManager {

    private final String source = "http://127.0.0.1/";

    /** The instance of OnlineMediaManager. */
    public static final OnlineMediaManager INSTANCE = new OnlineMediaManager();

    protected OnlineMediaManager() {
        pathPrefix = super.pathPrefix.concat("online/");
        cfile = new File(pathPrefix + "media.conf");
        conf = ConfigFactory.parseFile(cfile);
    }

    /**
     * Download the given media to local.
     * @param media The media to download.
     * @return If the media was successfully downloaded.
     */
    public boolean downloadMedia(ACMedia media) {
        try {
            return getOnlineFile(new URL(source + media.getFile().getName()), media.getFile());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete the local media files.
     * @param media The media to delete.
     * @return If the media was successfully deleted.
     */
    public boolean removeLocalMedia(ACMedia media) {
        return media.getFile().delete() && media.getCoverPic().delete();
    }

    @Override
    protected boolean checkConf() {
        if(!cfile.exists()) {
            AcademyCraft.log.info("Online media config File not found!");
            try {
                getOnlineFile(new URL(source + "media.conf"), new File(this.pathPrefix + "media.conf"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private boolean getOnlineFile(URL url, File local) {
        if(local.exists()) {
            local.delete();
            try {
                local.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HttpURLConnection conn = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                byte[] buffer = new byte[2048];
                is = conn.getInputStream();
                os = new FileOutputStream(local);
                int count = 0, finished = 0, size = conn.getContentLength();
                while ((count = is.read(buffer)) != -1) {
                    if (count != 0) {
                        os.write(buffer, 0, count);
                        finished += count;
                    } else {
                        break;
                    }
                }
            } else {
                try {
                    is.close();
                    os.close();
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                is.close();
                os.close();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                return false;

            }
        }
        return true;
    }
}
