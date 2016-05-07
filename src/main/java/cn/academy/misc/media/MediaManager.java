/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.generic.RegistryUtils;
import cn.lambdalib.util.mc.SideHelper;
import com.google.common.base.Throwables;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author KSkun
 */
@Registrant
public class MediaManager {

    private static Map<String, ACMedia> medias = new LinkedHashMap<>();
    private static List<ACMedia> mediasList = new ArrayList<>();
    private static List<ACMedia> internalMediasList = new ArrayList<>();

    private static final ResourceLocation missingCover = Resources.getTexture("guis/icons/icon_nomedia");

    @SideOnly(Side.CLIENT)
    private static SimpleTexture missingCoverTexture;

    private MediaManager() {}

    @RegInitCallback
    public static void __init() {
        Side side = FMLCommonHandler.instance().getSide();
        if (side == Side.CLIENT) {
            parseDefaultConfig();
            parseCustomConfig();
        } else {
            parseDefaultConfig_S();
        }
    }

    private static Config getDefaultConfig() {
        Reader reader = new InputStreamReader(
                RegistryUtils.getResourceStream(new ResourceLocation("academy:media/default.conf")));

        return ConfigFactory.parseReader(reader);
    }

    private static void parseDefaultConfig_S() {
        Config conf = getDefaultConfig();

        for (String id : conf.getStringList("default_medias")) {
            register(ACMedia.newInternal(id, null));
        }
    }

    private static void parseDefaultConfig() {
        Config conf = getDefaultConfig();

        File path = checkPath(new File(folder(), "acmedia/defaultSource"));

        // Temp workaround: Copy the default medias to the file system. Thus, the VorbisFile can read the length.
        //  this is because java's File doesn't support classpath resources.
        for (String id : conf.getStringList("default_medias")) {
            try {
                Path dst = new File(path, id + ".ogg").toPath();
                Files.copy(RegistryUtils.getResourceStream(new ResourceLocation("academy:media/source/" + id + ".ogg")),
                        dst, StandardCopyOption.REPLACE_EXISTING);

                register(ACMedia.newInternal(id, dst.toUri().toURL()));
            } catch (IOException ex) {
                AcademyCraft.log.error("Can't copy media file " + id + ".");
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void parseCustomConfig() {
        missingCoverTexture = new SimpleTexture(missingCover);

        File path = checkPath(new File(folder(), "acmedia"));

        // Also copy the readme_template.txt to the folder.
        {
            Path dst = new File(path, "README.txt").toPath();
            try {
                Files.copy(RegistryUtils.getResourceStream(new ResourceLocation("academy:media/readme_template.txt")),
                        dst, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                AcademyCraft.log.error("Can't copy media readme file.");
            }
        }

        File coverPath = checkPath(new File(path, "cover"));
        File sourcePath = checkPath(new File(path, "source"));

        for (File sound : sourcePath.listFiles()) {
            String fullname = sound.getName();
            int dotidx = fullname.lastIndexOf('.');
            if (dotidx == -1) {
                throw new IllegalArgumentException("Invalid filename: " + fullname);
            }

            String id = fullname.substring(0, dotidx);
            String postfix = fullname.substring(dotidx + 1);

            try {
                ACMedia media = ACMedia.newExternal(id, sound.toURI().toURL(), postfix);
                ResourceLocation loc = media.getCover();
                // Manually override the texture in the given path
                {
                    File coverFile = new File(coverPath, id + ".png");
                    try {
                        if (coverFile.isFile()) {
                            InputStream stream = new FileInputStream(coverFile);
                            BufferedImage buffer = ImageIO.read(stream);
                            ITextureObject object = new DynamicTexture(buffer);

                            Minecraft.getMinecraft().getTextureManager().loadTexture(loc, object);
                        } else {
                            Minecraft.getMinecraft().getTextureManager().loadTexture(loc, missingCoverTexture);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                register(media);
            } catch (MalformedURLException ex) {
                Throwables.propagate(ex);
            }
        }
    }

    private static File folder() {
        if (FMLCommonHandler.instance().getSide() == Side.SERVER) {
            return new File("/");
        } else {
            return folderName_c();
        }
    }

    @SideOnly(Side.CLIENT)
    private static File folderName_c() {
        return new File(Minecraft.getMinecraft().mcDataDir, "acmedia");
    }

    private static File checkPath(File file) {
        if (!file.exists()) {
            file.mkdirs();
        }

        if (file.isDirectory()) {
            return file;
        } else {
            throw new IllegalStateException("Not a directory");
        }
    }

    /**
     * Get all the medias that has been registered.
     * @return The collection of all the medias.
     */
    public static List<ACMedia> medias() {
        return mediasList;
    }

    public static List<ACMedia> internalMedias() {
        return internalMediasList;
    }

    /**
     * Register your media into the manager (in code).
     * @param media The media to register.
     */
    public static void register(ACMedia media) {
        medias.put(media.getID(), media);
        mediasList.add(media);
        if (!media.isExternal()) {
            internalMediasList.add(media);
        }
    }

    public static ACMedia get(String id) {
        if (medias.containsKey(id)) {
            return medias.get(id);
        } else {
            throw new IllegalArgumentException("No media with id " + id);
        }
    }

    public static ACMedia get(int idx) {
        return mediasList.get(idx);
    }

    public static int mediasCount() {
        return mediasList.size();
    }

    public static int indexOf(ACMedia media) {
        return mediasList.indexOf(media);
    }

}
