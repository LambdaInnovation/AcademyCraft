/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import com.google.common.base.Throwables;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;
import net.minecraft.entity.player.EntityPlayer;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author KSkun
 */
public class ACMedia {

    private File file;

    private String author;

    private String name;

    private File coverPic;

    private String id;

    private String remark;

    public boolean available = true;

    public ACMedia(File _file) {
        file = _file;
    }
    public ACMedia(URL url) {
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }
    public ACMedia(String path) {
        file = new File(path);
    }

    /**
     * Get the sound file of the media.
     * @return The sound file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Get the author of the media.
     * @return The author.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Get the display name of the media.
     * @return The display name.
     */
    public String getName() {
        if(name != null) {
            return name;
        } else {
            return file.getName().replace(".ogg", "");
        }
    }

    /**
     * Get the cover picture of the record.
     * @return The cover pic.
     */
    public File getCoverPic() {
        return coverPic;
    }

    /**
     * Get the remark of the media.
     * @return The remark of the media.
     */
    public String getRemark() {
        return remark;
    }

    /**
     * Get the ID of the media.
     * @return The ID of the media.
     */
    public String getId() {
        if(id != null) {
            return id;
        } else {
            return file.getName().replace(".ogg", "");
        }
    }

    /**
     * Set the ID of the media.
     * @param _id The ID to set.
     * @return this
     */
    public ACMedia setId(String _id) {
        id = _id;
        return this;
    }

    /**
     * Set the display name of the media.
     * @param _name The display name.
     * @return this
     */
    public ACMedia setName(String _name) {
        name = _name;
        return this;
    }

    /**
     * Set the author of the media.
     * @param _author The author.
     * @return
     */
    public ACMedia setAuthor(String _author) {
        author = _author;
        return this;
    }

    /**
     * Set the cover picture of the record.
     * @param path The path of the cover pic.
     * @return this
     * @throws URISyntaxException
     */
    public ACMedia setCoverPic(URL path) throws URISyntaxException {
        setCoverPic(new File(path.toURI()));
        return this;
    }

    /**
     * Set the cover picture of the record.
     * @param path The path of the cover pic.
     * @return this
     */
    public ACMedia setCoverPic(String path) {
        setCoverPic(new File(path));
        return this;
    }

    /**
     * Set the cover picture of the record.
     * @param _file The cover pic file.
     * @return this
     */
    public ACMedia setCoverPic(File _file) {
        coverPic = _file;
        return this;
    }

    /**
     * Set the remark picture of the media.
     * @param _remark The remark.
     * @return this
     */
    public ACMedia setRemark(String _remark) {
        remark = _remark;
        return this;
    }

    public float getTotalLength() {
        VorbisFile vf = null;
        try {
            vf = new VorbisFile(file.getPath());
        } catch (JOrbisException e) {
            throw Throwables.propagate(e);
        }
        return vf.time_total(-1);
    }

}
