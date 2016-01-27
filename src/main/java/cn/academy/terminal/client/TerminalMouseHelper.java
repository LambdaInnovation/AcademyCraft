/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.client;

import net.minecraft.util.MouseHelper;
import org.lwjgl.input.Mouse;

/**
 * @author WeAthFolD
 */
public class TerminalMouseHelper extends MouseHelper {
    
    public int dx, dy;
    
    @Override
    public void mouseXYChange() {
        this.dx = Mouse.getDX();
        this.dy = Mouse.getDY();
    }
}
