package cn.academy.terminal;

import cn.academy.AcademyCraft;
import cn.lambdalib2.util.Debug;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

public enum DonatorList {
    Instance;

    List<String> _donators = Collections.emptyList();

    final Gson _gson = new Gson();

    volatile boolean _requesting = false;

    public boolean isLoaded() {
        return _donators.size() > 0;
    }

    private class Attributes {
        public List<String> list;
    }

    private class ResponseData {
        public boolean success;
        public Attributes attributes;
    }

    public List<String> getList() {
        return _donators;
    }

    public void tryRequest() {
        if (!isLoaded() && !_requesting) {
            _requesting = true;
            new Thread(() -> {
                try {
                    String url = "https://ac.li-dev.cn/donators";
                    URLConnection con = new URL(url).openConnection();
                    con.setDoInput(true);

                    InputStream is = con.getInputStream();
                    String text = IOUtils.toString(is, "UTF-8");
                    ResponseData rsp = _gson.fromJson(text, ResponseData.class);

                    if (rsp.success) {
                        Debug.assertNotNull(rsp.attributes);
                        Debug.assertNotNull(rsp.attributes.list);

                        // Acknowledge the result in CLIENT thread
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            _donators = rsp.attributes.list;
                            MinecraftForge.EVENT_BUS.post(new DonatorListRefreshEvent());
                        });
                    } else {
                        Debug.error("AcademyCraft failed when requesting donator list.");
                        if (AcademyCraft.DEBUG_MODE)
                            Debug.error("Rsp str: " + text);
                    }
                } catch (Exception e) {
                    if (AcademyCraft.DEBUG_MODE)
                        Debug.error(e);
                }
                _requesting = false;
            }).start();
        }
    }

    public static class DonatorListRefreshEvent extends Event {}
}
