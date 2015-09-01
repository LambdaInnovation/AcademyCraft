package cn.academy.test;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.List;

import cn.annoreg.core.Registrant;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.GameTimer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;

// Used in video rendering of AC1.0 prerelease :) don't delete this too soon
//@Registrant
//@RegAuxGui
public class PerformanceUI extends AuxGui {
	
	static final int TIMEADD = 3000, BLENDTIME = 150, TIMEWAIT = 80;
	static final int PERLINE = 8;
	static final double SIZE = 30, MARGIN = 8, STEP = SIZE + MARGIN;
	
	List<ResourceLocation> iconList = new ArrayList();
	
	void block(String s) {
		tex("blocks/" + s);
	}
	
	void item(String s) {
		tex("items/" + s);
	}
	
	void em(String s) {
		tex("abilities/electromaster/skills/" + s);
	}
	
	void md(String s) {
		tex("abilities/meltdowner/skills/" + s);
	}
	
	void tp(String s) {
		tex("abilities/teleporter/skills/" + s);
	}
	
	void tex(String s) {
		iconList.add(new ResourceLocation("academy:textures/" + s + ".png"));
	}
	
	public PerformanceUI() {
		for(String s : new String[] { 
			"constraint_metal_ore",
			"crystal_ore",
			"ief_working_1",
			"imag_silicon_ore",
			"matrix",
			"metal_former_front",
			"phase_generator",
			"machine_top",
			"machine_frame",
			"node_top_1",
			"solar_gen",
			"windgen_base",
			"windgen_main",
			"windgen_pillar"
		}) block(s);
		
		for(String s : new String[] { 
				"app_freq_transmitter",
				"app_media_player",
				"app_settings",
				"app_skill_tree",
				"brain_component",
				"calc_chip",
				"constraint_ingot",
				"constraint_plate",
				"crystal_low",
				"crystal_normal",
				"crystal_pure",
				"data_chip",
				"energy_convert_component",
				"energy_unit_full",
				"imag_silicon_ingot",
				"info_component",
				"machine_casing",
				"maghook",
				"matrix_core_2",
				"matter_unit",
				"media_only_my_railgun",
				"reinforced_iron_plate",
				"terminal_installer",
				"silbarn",
				"resonance_component",
				"wafer",
				"windgen_fan",
				"terminal_installer"
		}) item(s);
		
		for(String s : new String[] { 
			"railgun",
			"thunder_clap",
			"mag_manip",
			"mag_movement",
			"alter",
			"lf"
		}) em(s);
		
		for(String s : new String[] { 
			"meltdowner",
			"rad_intensify",
			"electron_missile",
			"light_shield",
			"electron_bomb",
			"mine_ray_acc"
		}) md(s);
		
		for(String s : new String[] { 
			"dim_folding_theoreom",
			"location_teleport",
			"penetrate_teleport",
			"shift_tp",
			"space_fluct",
			"mark_teleport"
		}) tp(s);
	}
	
	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		glPushMatrix();
		double w = sr.getScaledWidth_double(), h = sr.getScaledHeight_double();
		
		glColor4f(0, 0, 0, 1);
		HudUtils.colorRect(0, 0, w, h);
		
		glColor4f(1, 1, 1, 1);
		
		long maxtime = iconList.size() * TIMEWAIT + TIMEADD;
		
		long time = GameTimer.getTime() % maxtime;
		int xx = GameTimer.getTime() % (maxtime * 2) < maxtime ? 1 : 0;
		
		double x = 0, y = 0 - xx * 200;
		
		int tl = 0;
		int i = 0;
		for(ResourceLocation r : iconList) {
			i++;
			
			double alpha = MathUtils.wrapd(0, 1, (time - i * TIMEWAIT) / (double) BLENDTIME);
			
			glColor4d(1, 1, 1, alpha);
			//glColor4d(0, 0, 0, 1);
			RenderUtils.loadTexture(r);
			HudUtils.rect(x + 10, y + 10, SIZE, SIZE);
			
			x += STEP;
			if(++tl == PERLINE) {
				tl = 0;
				y += STEP;
				x = 0;
			}
		}
		
		glPopMatrix();
	}

}
