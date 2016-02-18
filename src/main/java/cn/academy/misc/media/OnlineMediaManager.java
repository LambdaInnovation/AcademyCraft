/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import com.google.common.base.Throwables;
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
        } catch (Exception e) {
            throw Throwables.propagate(e);
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
            AcademyCraft.log.error("Online media config File not found!");
            try {
                if(!getOnlineFile(new URL(source + "media.conf"), new File(this.pathPrefix + "media.conf"))) {
                    AcademyCraft.log.error("Can't download online media config from server! Ignore loading online resource.");
                }
            } catch (MalformedURLException e) {
                throw Throwables.propagate(e);
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
                throw Throwables.propagate(e);
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
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return false;
            }
        } catch (FileNotFoundException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            //Exceptions will be thrown when no Internet or other network problems, and it is no reason to crash it.
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
