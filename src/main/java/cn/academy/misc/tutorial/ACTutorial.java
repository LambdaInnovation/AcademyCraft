package cn.academy.misc.tutorial;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.generic.RegistryUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

@Registrant
public class ACTutorial {

	public static final boolean SHOW_ALL = true;

	public final String id;

	private Condition condition = Condition.TRUE;

	private List<IPreviewHandler> previewHandlers = new ArrayList<>();
    private boolean previewInit = false;

    {
        previewHandlers.add(PreviewHandlers.nothing);
    }

	public ACTutorial(String id) {
		this.id=id;
	}

	public ACTutorial setCondition(Condition condition) {
		this.condition=condition;
		return this;
	}

	public ACTutorial addPreview(IPreviewHandler ...handlers) {
        if (!previewInit) {
            previewInit = true;
            previewHandlers.clear();
        }
		previewHandlers.addAll(Arrays.asList(handlers));
		return this;
	}

	public List<IPreviewHandler> getPreview() {
		return previewHandlers;
	}

    @SideOnly(Side.CLIENT)
	public String getContent() {
        final String unknown = "![title]\nUNKNOWN \n![brief]\n![content]\n ";
        try {
            String lang = Minecraft.getMinecraft().gameSettings.language;
            InputStream stream = RegistryUtils.getResourceStream(location(lang));
            if (stream == null) { // Make en_US the default fallback
                stream = RegistryUtils.getResourceStream(location("en_US"));
            }
            if (stream == null) {
                return unknown;
            } else {
                return IOUtils.toString(stream);
            }
        } catch (NullPointerException|IOException e) {
            return unknown;
        }
    }

    private ResourceLocation location(String lang) {
        return new ResourceLocation("academy:tutorials/" + lang + "/" + id + ".md");
    }

	public boolean isActivated(EntityPlayer player) {
		if (SHOW_ALL)
			return true;
        return this.condition.exam(player);
	}

}
