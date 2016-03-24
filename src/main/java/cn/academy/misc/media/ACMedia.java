package cn.academy.misc.media;

import cn.academy.core.AcademyCraft;
import com.google.common.base.Throwables;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class ACMedia {

    public static void updateExternalName(ACMedia media, String name) {
        extName(media).set(name);
    }

    public static void updateExternalDesc(ACMedia media, String desc) {
        extDesc(media).set(desc);
    }

    private static Property extName(ACMedia media) {
        return config().get("media", media.getID() + "_name", media.getID());
    }

    private static Property extDesc(ACMedia media) {
        return config().get("media", media.getID() + "_desc", "N/A");
    }

    public static ACMedia newExternal(String id, URL url, String postfix) {
        return new ACMedia(id, true, url, postfix) {
            @Override
            public String getName() {
                return extName(this).getString();
            }
            @Override
            public String getDesc() {
                return extDesc(this).getString();
            }
        };
    }

    private static Configuration config() {
        return AcademyCraft.config;
    }

    public static ACMedia newInternal(String id) {
        URL url = ACMedia.class.getResource("/assets/academy/media/source/" + id + ".ogg");
        if (url.getFile().isEmpty()) {
            throw new IllegalStateException("Can't find internal media with url " + url);
        }

        return new ACMedia(id, false, url, ".ogg") {
            @Override
            public String getName() {
                return StatCollector.translateToLocal("ac.media." + id + ".name");
            }
            @Override
            public String getDesc() {
                return StatCollector.translateToLocal("ac.media." + id + ".desc");
            }
        };
    }

    private float length = -1;
    private final String id;
    private final URL source;
    private final ResourceLocation cover;
    private final String postfix;
    private final boolean external;

    private ACMedia(String _id, boolean _external, URL _source, String _postfix) {
        id = _id;
        source = _source;
        cover = new ResourceLocation("academy:media/cover/" + id + ".png");
        external = _external;
        postfix = _postfix;
    }

    public abstract String getName();
    public abstract String getDesc();

    public String getID() {
        return id;
    }

    public URL getSource() {
        return source;
    }

    public boolean isExternal() {
        return external;
    }

    public ResourceLocation getCover() {
        return cover;
    }

    public String getFilePostfix() {
        return postfix;
    }

    @SideOnly(Side.CLIENT)
    public final float getLength() {
        if (length == -1) try {
            try (InputStream stream = getSource().openStream()) {
                VorbisFile vf = new VorbisFile(stream, null, 0);
                length = vf.time_total(-1);

                vf.close();
            } catch (JOrbisException e) {
                throw Throwables.propagate(e);
            }
        } catch (IOException ex) {
            Throwables.propagate(ex);
        }

        return length;
    }

}
