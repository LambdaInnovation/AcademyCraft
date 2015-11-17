package cn.academy.misc.tutorial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.lambdalib.util.client.article.ArticleCompiler;
import cn.lambdalib.util.client.article.ArticlePlotter;
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
import scala.actors.threadpool.Arrays;

@Registrant
public class ACTutorial {

	// Static registry
	private static HashMap<String,ACTutorial> tutorials=new HashMap<String,ACTutorial>();
	private static final ACTutorialDataPart data = new ACTutorialDataPart();
	static List<Condition> savedConditions = new ArrayList<Condition>();

	public static void addTutorials(ACTutorial...tutorial) {
		for(ACTutorial t : tutorial){
			if(tutorials.containsKey(t.id))
				throw new RuntimeException("Alreadyhas a tutorial with this id:" + t.id);
			tutorials.put(t.id, t);
		}
	}

	public static ACTutorial addTutorial(String string) {
		ACTutorial t=new ACTutorial(string);
		addTutorials(t);
		return t;
	}

	public static void addTutorials(String...string) {
		ACTutorial[] acTu=new ACTutorial[string.length];
		int i = 0;
		for(String s : string){
			acTu[i] = new ACTutorial(s);
			i++;
		}
		addTutorials(acTu);
	}

	public static ACTutorial getTutorial(String s) {
		ACTutorial t;
		if(!tutorials.containsKey(s))
			throw new RuntimeException("No such a tutorial;");
		t=tutorials.get(s);
		return t;
	}

	/**
	 * Get a collection of tutorial learned by the player.
	 */
	public static Collection<ACTutorial> getLearned(EntityPlayer player) {
		return tutorials
				.values()
				.stream()
				.filter(t -> t.getIsLoad(player))
				.collect(Collectors.toList());
	}

	/**
	 * Get a immutable enumeration of all registered tutorial.
	 */
	public static Collection<ACTutorial> enumeration() {
		return ImmutableList.copyOf(tutorials.values());
	}
	// Static registry end

	private static IPreviewHandler[] defaultHandlers = new IPreviewHandler[] { PreviewHandlers.nothing };
	public static final boolean SHOW_ALL = true;

	public final String id;
	
	private Condition condition;

	// Client: Cached ArticlePlotters
	// TODO: Currently doesn't support runtime lang change. Support it?
	@SideOnly(Side.CLIENT)
	private ArticlePlotter cachedBrief, cachedContent;

	private IPreviewHandler[] previewHandlers = defaultHandlers;

	public ACTutorial(String id) {
		this.id=id;
	}
	
	public ACTutorial setCondition(Condition condition) {
		this.condition=condition;
		condition.addNeedSavingToTutorial(this);
		return this;
	}

	public ACTutorial setPreview(IPreviewHandler ...handlers) {
		previewHandlers = handlers;
		return this;
	}

	public IPreviewHandler[] getPreview() {
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
			cachedBrief = new ArticleCompiler(getBrief()).setWidth(defWidth).setFontSize(fontSize).compile();
		}
		return cachedBrief;
	}

	@SideOnly(Side.CLIENT)
	public ArticlePlotter getContentPlotter(double defWidth, float fontSize) {
		if(cachedContent == null) {
			cachedContent = new ArticleCompiler(getContent()).setWidth(defWidth).setFontSize(fontSize).compile();
		}
		return cachedContent;
	}

	private String key(String str) {
		return "ac.gui.tutorial." + id + "." + str;
	}

	private String local(String str) {
		return StatCollector.translateToLocal(key(str));
	}

	public boolean getIsLoad(EntityPlayer player) {
		if(SHOW_ALL)
			return true;
		if(this.condition!=null)
			return this.condition.exam(player);
		return true;
	}

	public static void debug(EntityPlayer player) {
		AcademyCraft.log.info("DEBUG:");
		for(ACTutorial t : tutorials.values()){
			AcademyCraft.log.info(t.id+" : "+t.getIsLoad(player));
		}
		for(Condition c : savedConditions){
			AcademyCraft.log.info(c.index+" : "+c.exam(player));
		}
	}

	@RegDataPart("ACTutorial")
	public static class ACTutorialDataPart extends DataPart{
		boolean[] allSaved=null;

		void init() {
			allSaved = new boolean[ACTutorial.savedConditions.size()];
			Arrays.fill(allSaved, false);
		}

		public void update() {
			sync();
		}

		@Override
		public void fromNBT(NBTTagCompound tag) {
			if(allSaved==null)init();
			Set<String> set=tag.func_150296_c();
			for(String s : set){
				try {
					allSaved[Integer.parseInt(s)]= tag.getBoolean(s);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public NBTTagCompound toNBT() {
			if(allSaved==null)init();
			NBTTagCompound tag = new NBTTagCompound();
			for(int i=0;i<allSaved.length;i++){
				tag.setBoolean(String.valueOf(i), allSaved[i]);
			}
			return tag;
		}

	}

}
