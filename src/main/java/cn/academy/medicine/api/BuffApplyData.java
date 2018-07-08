package cn.academy.medicine.api;

public class BuffApplyData {
    int tickLeft;
    private int maxTicks_;

    public BuffApplyData(int tickLeft,int maxTicks){
        this.tickLeft=tickLeft;
        this.maxTicks_=maxTicks;
    }

    public boolean isInfinite(){
        return maxTicks_ == -1;
    }

    public void setEnd(){
        if (isInfinite()) {
            maxTicks_ = 10;
        }
        tickLeft = 0;
    }

    public int maxTicks()
    {
        return maxTicks_;
    }
}
