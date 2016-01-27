/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.ui;

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
