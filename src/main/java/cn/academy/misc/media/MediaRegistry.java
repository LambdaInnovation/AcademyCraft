package cn.academy.misc.media;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaRegistry {
    static final Map<String, Media> medias = new HashMap();
    static final List<Media> mediaList = new ArrayList();
    
    static {
        addMedia("only_my_railgun", 257);
        addMedia("level5_judgelight", 285);
        addMedia("sisters_noise", 283);
    }
    
    public static void addMedia(String name, int len) {
        addMedia(new Media(name, len));
    }
    
    public static void addMedia(Media media) {
        mediaList.add(media);
        media.id = mediaList.size() - 1;
        
        medias.put(media.name, media);
    }
    
    public static Media getMedia(int id) {
        return mediaList.get(id);
    }
    
    public static Media getMedia(String name) {
        return medias.get(name);
    }
    
    public static Collection<Media> getMedias() {
        return medias.values();
    }
    
    public static int getMediaCount() {
        return mediaList.size();
    }
}
