/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.app.settings;

import cn.lambdalib.cgui.gui.CGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.gui.component.ElementList;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.VerticalDragBar;
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.lambdalib.cgui.xml.CGUIDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author WeAthFolD
 */
public class SettingsUI extends CGuiScreen {
    
    static final WidgetContainer document = CGUIDocument.panicRead(new ResourceLocation("academy:guis/settings.xml"));
    
    private static Map<String, List<UIProperty>> properties = new HashMap<>();
    static {
        addProperty(PropertyElements.CHECKBOX, "generic", "attackPlayer", true, true);
        addProperty(PropertyElements.CHECKBOX, "generic", "destroyBlocks", true, true);
        addProperty(PropertyElements.CHECKBOX, "generic", "headsOrTails", false, false);
    }
    
    public static void addProperty(IPropertyElement elem, String cat, String id, Object defValue, boolean singlePlayer) {
        add(cat, new UIProperty.Config(elem, cat, id, defValue, singlePlayer));
    }

    public static void addCallback(String id, String cat, Runnable callback, boolean singlePlayer) {
        add(cat, new UIProperty.Callback(PropertyElements.CALLBACK, id, callback, singlePlayer));
    }

    private static void add(String cat, UIProperty prop) {
        List<UIProperty> list = properties.get(cat);
        if(list == null)
            properties.put(cat, list = new ArrayList<>());
        list.add(prop);
    }
    
    public SettingsUI() {
        initPages();
    }
    
    private void initPages() {
        Widget main = document.getWidget("main").copy();
        
        Widget area = main.getWidget("area");
        
        boolean singlePlayer = Minecraft.getMinecraft().isSingleplayer();
        
        ElementList list = new ElementList(); 
        {
            for(Entry<String, List<UIProperty>> entry : properties.entrySet()) {
                Widget head = document.getWidget("t_cathead").copy();
                TextBox.get(head.getWidget("text")).setContent(local("cat." + entry.getKey()));
                list.addWidget(head);
                
                for(UIProperty prop : entry.getValue()) {
                    if(!prop.singlePlayer || singlePlayer)
                        list.addWidget(prop.element.getWidget(prop));
                }
                
                Widget placeholder = new Widget();
                placeholder.transform.setSize(10, 20);
                list.addWidget(placeholder);
            }
        } 
        area.addComponent(list);
        
        Widget bar = main.getWidget("scrollbar");
        bar.listen(DraggedEvent.class, (w, e) ->
        {
            list.setProgress((int) (list.getMaxProgress() * VerticalDragBar.get(w).getProgress()));
        });
        
        gui.addWidget(main);
    }
    
    private String local(String id) {
        return StatCollector.translateToLocal("ac.settings." + id);
    }
    
}
