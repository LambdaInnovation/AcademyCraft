package cn.academy.util;

public class Plotter {

    enum Axis {X, Y, Z}

    final Axis axis; // The main axis which we incr(decr) through
    final double dyx, dzx;
    final int dirflag;
    int x0, y0, z0;

    int x, y, z;

    public Plotter(int _x0, int _y0, int _z0,
            double dx, double dy, double dz) {
        x0 = _x0;
        y0 = _y0;
        z0 = _z0;

        // Determine which direction to increment to
        // and swap the values so we are always thinking we are incrementing via x+
        double adx = Math.abs(dx), ady = Math.abs(dy), adz = Math.abs(dz);

        int itemp;
        double dtemp;
        if(adz > ady && adz > adx) {
            dtemp = dz;
            dz = dx;
            dx = dtemp;
            itemp = z0;
            z0 = x0;
            x0 = itemp;
            axis = Axis.Z;
        } else if(ady > adx) {
            dtemp = dy;
            dy = dx;
            dx = dtemp;
            itemp = y0;
            y0 = x0;
            x0 = itemp;
            axis = Axis.Y;
        } else if(adx > 0) {
            axis = Axis.X;
        } else
            throw new RuntimeException("Zero slope vector");

        x = x0;
        y = y0;
        z = z0;
        dyx = dy / dx;
        dzx = dz / dx;
        dirflag = dx > 0 ? 1 : -1;
    }

    public int[] next() {
        int nextX = x + dirflag;
        double valy = y0 + (nextX - x0) * dyx;
        double valz = z0 + (nextX - x0) * dzx;
        if(Math.abs(valy - y) > 0.5) {
            y += Math.signum(dyx) * dirflag;
        } else if(Math.abs(valz - z) > 0.5) {
            z += Math.signum(dzx) * dirflag;
        } else {
            x = nextX;
        }

        switch(axis) {
        case X:
            return new int[] {x, y, z};
        case Y:
            return new int[] {y, x, z};
        case Z:
            return new int[] {z, y, x};
        }
        throw new IllegalStateException("Never reach here");
    }

}