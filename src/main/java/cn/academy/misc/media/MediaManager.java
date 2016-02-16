/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
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
        conf = ConfigFactory.parseFile(cfile);
    }

    public void init() {
        if(!checkConf()) {
            AcademyCraft.log.error("MediaManager can't be initialized!");
            return;
        }

        Config conf = ConfigFactory.parseFile(cfile);
        for(String id : conf.getStringList("ac.media.medias")) {
            ACMedia media = new ACMedia(id);
            Config mediac = conf.getConfig("ac.media." + id);
            if(mediac.hasPath("author")) media.setAuthor(mediac.getString("author"));
            media.setName(mediac.getString("name"));
            media.setFile(pathPrefix + mediac.getString("filename"));
            if(mediac.hasPath("picfile")) media.setCoverPic(pathPrefix + mediac.getString("picfile"));
            medias.put(id, media);
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
     * @param id The ID of your media.
     */
    public void registerMedia(ACMedia media, String id) {
        medias.put(id, media);
    }

    /**
     * Register your media into the manager (in code).
     * @param media Your media.
     */
    public void registerMedia(ACMedia media) {
        registerMedia(media, media.getId());
    }

    protected boolean checkConf() {
        if(!cfile.exists()) {
            AcademyCraft.log.info("Local media config File not found!");
            try {
                cfile.createNewFile();
            } catch(Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    //TODO: Do init NBT Datas when MediaPlayer is installed.

}
