/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.internal;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.energy.api.IWirelessTile;
import cn.liutils.util.DebugUtils;

/**
 * Representing a block coord in a world representing a certain wireless tile type.
 * @author WeathFolD
 */
public class Coord {
    
    public final World world;
    public final int x, y, z;
    public final BlockType type;
    
    public Coord(IWirelessTile _te, BlockType _type) {
        this((TileEntity) _te, _type);
    }
    
    public Coord(TileEntity _te, BlockType _type) {
        this(_te.getWorldObj(), _te.xCoord, _te.yCoord, _te.zCoord, _type);
    }
    
    public Coord(World _world, ByteBuf buf) {
        world = _world;
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        type = BlockType.values()[buf.readByte()];
    }

    public Coord(World _world, int _x, int _y, int _z, BlockType _type) {
        world = _world;
        x = _x;
        y = _y;
        z = _z;
        type = _type;
    }
    
    public Coord(World _world, NBTTagCompound tag, BlockType type) {
        this(_world, 
                tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"), type);
    }
    
    public void save(NBTTagCompound tag) {
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
    }
    
    public boolean isLoaded() {
        return world.getChunkProvider().chunkExists(x >> 4, z >> 4);
    }
    
    /**
     * Get the tile this coord correspond to, at the same time do the type validation.
     * @return The desired tile, or null if validation failed.
     */
    public TileEntity getAndCheck() {
        if(!isLoaded()) return null;
        TileEntity te = world.getTileEntity(x, y, z);
        return type.validate(te) ? te : null;
    }
    
    public void toBuf(ByteBuf buf) {
        buf.writeInt(x).writeInt(y).writeInt(z).writeByte(type.ordinal());
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Coord))
            return false;
        Coord c = (Coord) o;
        return c.x == x && c.y == y && c.z == z && c.type == type;
    }
    
    @Override
    public int hashCode() {
        return x ^ (y << 4) ^ (z << 8);
    }
    
    @Override
    public String toString() {
        return "[" + DebugUtils.formatArray(x, y, z) + ", " + type.name() + "]";
    }

}
