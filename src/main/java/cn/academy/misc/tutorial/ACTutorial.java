package cn.academy.misc.tutorial;

import java.util.*;
import java.util.stream.Collectors;

import cn.academy.core.client.Resources;
import cn.lambdalib.util.client.article.ArticleCompiler;
import cn.lambdalib.util.client.article.ArticlePlotter;
import cn.lambdalib.util.client.font.IFont.FontOption;
import com.google.common.collect.ImmutableList;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.RegDataPart;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

@Registrant
public class ACTutorial {

	public static final boolean SHOW_ALL = true;

	public final String id;

	private Condition condition = Condition.TRUE;

	// Client: Cached ArticlePlotters
	// TODO: Currently doesn't support runtime lang change. Support it?
	@SideOnly(Side.CLIENT)
	private ArticlePlotter cachedBrief, cachedContent;

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

	public String getBrief() {
		return local("brief");
	}

	public String getContent() {
		return local("content");
	}

	public String getTitle() {
		return local("title");
	}

	@SideOnly(Side.CLIENT)
	public ArticlePlotter getBriefPlotter(double defWidth, float fontSize) {
		if(cachedBrief == null) {
			cachedBrief = new ArticleCompiler(getBrief())
					.setFont(Resources.font())
					.setWidth(defWidth)
					.setFontOption(new FontOption(fontSize))
					.compile();
		}
		return cachedBrief;
	}

	@SideOnly(Side.CLIENT)
	public ArticlePlotter getContentPlotter(double defWidth, float fontSize) {
		if(cachedContent == null) {
			cachedContent = new ArticleCompiler(getContent())
					.setFont(Resources.font())
					.setWidth(defWidth)
					.setFontOption(new FontOption(fontSize))
					.compile();
		}
		return cachedContent;
	}

	private String key(String str) {
		return "ac.gui.tutorial." + id + "." + str;
	}

	private String local(String str) {
		return StatCollector.translateToLocal(key(str));
	}

	public boolean isActivated(EntityPlayer player) {
		if (SHOW_ALL)
			return true;
        return this.condition.exam(player);
	}

}
