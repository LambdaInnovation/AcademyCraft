package cn.academy.client.gui;

import net.minecraft.util.ResourceLocation;

/**
 * Represents one instance of message to be displayed on the main gui.
 * @author WeAthFolD
 */
public interface INotification {
    
    ResourceLocation getIcon();
    
    /**
     * The title string to be directly displayed.
     */
    String getTitle();
    
    /**
     * The content string to be directly displayed.
     */
    String getContent();
    
}