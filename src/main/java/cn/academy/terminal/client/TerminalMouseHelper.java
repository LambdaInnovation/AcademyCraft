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