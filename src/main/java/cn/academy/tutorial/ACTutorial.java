package cn.academy.tutorial;

import cn.academy.Resources;
import cn.lambdalib2.util.ResourceUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ACTutorial {

    public enum Tag {
        CRAFT, SMELT, VIEW;

        public final ResourceLocation icon = Resources.getTexture(
                "guis/icons/icon_" + this.name().toLowerCase());
    }

    public static final boolean SHOW_ALL = false;

    public final String id;

    private Condition condition = Conditions.alwaysTrue();
    private boolean defaultInstalled = true;

    private List<ViewGroup> previewHandlers = new ArrayList<>();

    public ACTutorial(String id) {
        this.id=id;
    }

    public ACTutorial addCondition(Condition condition) {
        defaultInstalled = false;
        if(this.condition == Conditions.alwaysTrue()) {
            this.condition = condition;
        } else {
            this.condition = this.condition.or(condition);
        }
        return this;
    }

    public ACTutorial addPreview(ViewGroup...handlers) {
        previewHandlers.addAll(Arrays.asList(handlers));
        return this;
    }

    public List<ViewGroup> getPreview() {
        return previewHandlers;
    }

    @SideOnly(Side.CLIENT)
    public String getContent() {
        final String unknown = "![title]\nUNKNOWN \n![brief]\n![content]\n ";
        try {
            String lang = Minecraft.getMinecraft().gameSettings.language;
            InputStream stream = ResourceUtils.getResourceStreamNullable(location(lang));
            if (stream == null) { // Make en_us the default fallback
                stream = ResourceUtils.getResourceStream(location("en_us"));
            }
            if (stream == null) {
                return unknown;
            } else {
                return IOUtils.toString(new InputStreamReader(stream, Charset.forName("UTF-8").newDecoder()));
            }
        } catch (NullPointerException|IOException e) {
            return unknown;
        }
    }

    /**
     * Note that this method currently requires IO and is inefficient. Don't call it too often.
     */
    @SideOnly(Side.CLIENT)
    public String getTitle() {
        String raw = getContent();
        int i1 = raw.indexOf("![title]"),
                i2 = raw.indexOf("![brief]");
        return raw.substring(i1+8, i2).trim();
    }

    private ResourceLocation location(String lang) {
        return new ResourceLocation("academy:tutorials/" + lang + "/" + id + ".md");
    }

    public boolean isActivated(EntityPlayer player) {
        if (SHOW_ALL)
            return true;
        return this.condition.test(player);
    }

    public boolean isDefaultInstalled() {
        return defaultInstalled;
    }

}