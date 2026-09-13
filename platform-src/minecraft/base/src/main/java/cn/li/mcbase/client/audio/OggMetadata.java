package cn.li.mcbase.client.audio;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.io.File;

/**
 * Cheap .ogg duration probing (header/seek-table only, no full PCM decode) 鈥?
 * used to list external tracks (see media_player app's "external tracks"
 * feature) without loading each whole file into memory just to show a length.
 */
public final class OggMetadata {

    private OggMetadata() {}

    public static final class Info {
        public final float lengthSecs;

        public Info(float lengthSecs) {
            this.lengthSecs = lengthSecs;
        }
    }

    /** Returns null if the file can't be opened/decoded as Ogg Vorbis. */
    public static Info probe(String filePath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] error = new int[1];
            long handle = STBVorbis.stb_vorbis_open_filename(filePath, error, null);
            if (handle == 0L) {
                return null;
            }
            try {
                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(handle, info);
                int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(handle);
                int sampleRate = info.sample_rate();
                float lengthSecs = sampleRate > 0 ? (float) totalSamples / sampleRate : 0.0f;
                return new Info(lengthSecs);
            } finally {
                STBVorbis.stb_vorbis_close(handle);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** List *.ogg files directly inside `dir` (non-recursive), or empty array if absent. */
    public static File[] listOggFiles(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return new File[0];
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg"));
        return files != null ? files : new File[0];
    }
}
