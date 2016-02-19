/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import com.google.common.base.Throwables;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.util.*;

/**
 * @author KSkun
 */
public class MediaManager {

    protected String pathPrefix = true ?
            MediaManager.class.getProtectionDomain().getCodeSource().getLocation().getPath()
                    .replace("/out/production/forge-1.7.10-10.13.4.1517-1.7.10-src/cn/academy/misc/media/MediaManager.class",
                            "/eclipse").concat("/acmedia/") :
            MediaManager.class.getProtectionDomain().getCodeSource().getLocation().getPath()
                    .replaceAll("/mods/.+jar", "").concat("/acmedia/");
    protected File cfile;
    protected Config conf;
    protected Map<String, ACMedia> medias = new HashMap<String, ACMedia>();
    /** The instance of MediaManager. */
    public static final MediaManager INSTANCE = new MediaManager();

    protected MediaManager() {
        cfile = new File(pathPrefix + "media.conf");
    }

    public void init() {
        File mediaDir = new File(pathPrefix);
        if(!mediaDir.exists()) mediaDir.mkdir();
        if(!checkConf()) {
            AcademyCraft.log.error(getClass().getName() + " can't be initialized!");
            return;
        }

        List<ACMedia> files = new ArrayList<ACMedia>();

        if(mediaDir.listFiles() != null) {
            for (File f : mediaDir.listFiles()){
                if(f.getName().contains(".ogg")) {
                    files.add(new ACMedia(f));
                }
            }
        }
        
        conf = ConfigFactory.parseFile(cfile);

        for(ACMedia media : files) {
            String pathname = "ac.media." + media.getFile().getName();
            if(conf.hasPath(pathname)) {
                Config mediac = conf.getConfig(pathname);
                if(mediac.hasPath("author")) media.setAuthor(mediac.getString("author"));
                if(mediac.hasPath("name")) media.setName(mediac.getString("name"));
                if(mediac.hasPath("id")) media.setId(mediac.getString("id"));
                if(mediac.hasPath("picfile")) media.setCoverPic(pathPrefix + mediac.getString("picfile"));
                if(mediac.hasPath("remark")) media.setRemark(mediac.getString("remark"));
            }
            medias.put(media.getId(), media);
        }
    }

    /**
     * Get all the medias that has been registered.
     * @return The collection of all the medias.
     */
    public Collection<ACMedia> getMedias() {
        return medias.values();
    }

    /**
     * Get the media with given ID.
     * @param id The ID of media.
     * @return A instance of media with given ID.
     */
    public ACMedia getMedia(String id) {
        return medias.get(id);
    }

    /**
     * Get all the IDs of the medias.
     * @return The collection of all the IDs.
     */
    public Collection<String> getMediaIds() {
        return medias.keySet();
    }

    /**
     * Register your media into the manager (in code).
     * @param media Your media.
     */
    public void registerMedia(ACMedia media) {
        medias.put(media.getId(), media);
    }

    protected boolean checkConf() {
        if(!cfile.exists()) {
            AcademyCraft.log.info("Local media config File not found!");
            try {
                cfile.createNewFile();
            } catch(Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return true;
    }

    //TODO: Do init NBT Datas when MediaPlayer is installed.

}
