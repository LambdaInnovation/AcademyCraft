package cn.academy.core.client.ui;

import cn.academy.core.AcademyCraft;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * AC global HUD drawing dispatcher.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class ACHud extends AuxGui {
    
    @RegAuxGui
    public static ACHud instance = new ACHud();
    
    private List<Node> nodes = new ArrayList<>();
    
    private CGui gui = new CGui();

    private ACHud() {}

    @Override
    public boolean isForeground() {
        return false;
    }

    @Override
    public void draw(ScaledResolution sr) {
        gui.resize(sr.getScaledWidth_double(), sr.getScaledHeight_double());
        for(Node n : nodes) {
            n.w.transform.doesDraw = n.cond.shows();
        }
        
        gui.draw();
    }
    
    public void addElement(Widget w, Condition showCondition, String name, Widget preview) {
        Node node = new Node(w, showCondition, name, preview);
        nodes.add(node);
        node.updatePosition();

        gui.addWidget(w);
    }

    public List<Node> getNodes() {
        return ImmutableList.copyOf(nodes);
    }
    
    public interface Condition {
        boolean shows();
    }
    
    class Node {
        final Widget w;
        final Condition cond;
        final String name;
        final float defaultX, defaultY;
        final Widget preview;
        
        public Node(Widget _w, Condition _cond, String _name, Widget _preview) {
            w = _w;
            cond = _cond;
            name = _name;
            defaultX = (float) w.transform.x;
            defaultY = (float) w.transform.y;
            preview = _preview;
        }

        double[] getPosition() {
            return prop().getDoubleList();
        }

        void updatePosition() {
            double[] pos = getPosition();
            w.pos(pos[0], pos[1]);
            w.dirty = true;
        }

        void setPosition(double newX, double newY) {
            Property prop = prop();
            prop.set(new double[] { newX, newY });
            updatePosition();
        }

        private Property prop() {
            return conf().get("gui", name, new double[] { defaultX, defaultY });
        }

        private Configuration conf() {
            return AcademyCraft.config;
        }
    }

}
