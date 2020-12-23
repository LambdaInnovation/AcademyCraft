package cn.academy.ability;

import cn.academy.Resources;
import cn.academy.ACConfig;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * An Ability Category. Describes the skills that this category contains and the various other info.
 * You should use {@link CategoryManager#register(Category)} at mod init stage to register a category.
 * @author WeAthFolD
 */
public class Category {

    private final List<Skill> skillList = new ArrayList<>();
    private final List<Controllable> ctrlList = new ArrayList<>();
    
    private final String name;
    
    int catID = -1;
    
    // icon is displayed in developer and many other places, and overlay icon is used for culling effect on the CPBar.
    protected ResourceLocation icon, overlay, developerIcon;
    
    /**
     * The color style of this category. Used in many various places for rendering, like screen overlay.
     */
    @SideOnly(Side.CLIENT)
    private Color colorStyle;
    
    public Category(String _name) {
        name = _name;
        icon = initIcon();
        overlay = initOverlayIcon();
        developerIcon = initDeveloperIcon();
        if(SideUtils.isClient())
        {
            colorStyle = Colors.white();
        }
    }
    public void setColorStyle(int r, int g, int b)
    {
        setColorStyle(r, g, b, 0);
    }

    public void setColorStyle(int r ,int g, int b, int a)
    {
        if(SideUtils.isClient())
        {
            colorStyle.set(r, g, b, a);
        }
    }

    @SideOnly(Side.CLIENT)
    public Color getColorStyle() {
        return colorStyle;
    }
    
    public void addSkill(Skill skill) {
        if(getSkill(skill.getName()) != null)
            throw new RuntimeException("Duplicating skill " + skill.getName() + "!!");
        
        skillList.add(skill);
        addControllable(skill);
        
        skill.addedSkill(this, skillList.size() - 1);
    }
    
    public int getSkillID(Skill s) {
        return skillList.indexOf(s);
    }
    
    public int getSkillCount() {
        return skillList.size();
    }
    
    public Skill getSkill(int id) {
        return (id >= skillList.size() || id < 0) ? null : skillList.get(id);
    }
    
    public boolean containsSkill(Skill skill) {
        return skill == getSkill(skill.getID());
    }
    
    public Skill getSkill(String name) {
        for(Skill s : skillList)
            if(s.getName().equals(name))
                return s;
        return null;
    }
    
    /**
     * Get an <b>immutable</b> list of skills in this category.
     */
    public List<Skill> getSkillList() {
        return ImmutableList.copyOf(skillList);
    }
    
    public List<Skill> getSkillsOfLevel(int level) {
        List<Skill> ret = new ArrayList<>();
        for(Skill s : skillList)
            if(s.getLevel() == level)
                ret.add(s);
        return ret;
    }
    
    public int getCategoryID() {
        return catID;
    }
    
    public void addControllable(Controllable c) {
        ctrlList.add(c);
        c.addedControllable(this, ctrlList.size() - 1);
    }
    
    /**
     * Get the controllable with given id.
     */
    public Controllable getControllable(int id) {
        if(id < 0)
            return null;
        if(ctrlList.size() > id)
            return ctrlList.get(id);
        return null;
    }

    public float getProgIncrRate() {
        return (float) Preconditions.checkNotNull(ACConfig.instance().getConfig("ac.ability.category." + name)).getDouble("common.prog_incr_rate");
    }
    
    /**
     * Get the immutable controllable list of this category.
     */
    public List<Controllable> getControllableList() {
        return ImmutableList.copyOf(ctrlList);
    }
    
    public ResourceLocation getIcon() {
        return icon;
    }

    public ResourceLocation getDeveloperIcon() {
        return developerIcon;
    }

    public ResourceLocation getOverlayIcon() {
        return overlay;
    }
    
    /**
     * @return The internal name (or identifier) of this Category.
     */
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return I18n.translateToLocal("ac.ability." + name + ".name");
    }
    
    // Path init
    protected ResourceLocation initIcon() {
        return Resources.getTexture("abilities/" + name + "/icon");
    }
    
    protected ResourceLocation initOverlayIcon() {
        return Resources.getTexture("abilities/" + name + "/icon_overlay");
    }

    protected ResourceLocation initDeveloperIcon() {
        return Resources.getTexture("guis/icons/icon_" + name);
    }
    
}