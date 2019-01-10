package cn.academy.analyticUtil;

import cn.academy.AcademyCraft;
import cn.academy.event.ability.LevelChangeEvent;
import cn.academy.event.ability.SkillLearnEvent;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AnalyticDataListener {
    private AnalyticDto dataSource;
    public static final AnalyticDataListener instence = new AnalyticDataListener();
    private boolean isClinet;
    private String ACversion;
    private static AnalyticInfoSender sender = new AnalyticInfoSender(300);

    private AnalyticDataListener(){
        dataSource = new AnalyticDto();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void loginListener(PlayerEvent.PlayerLoggedInEvent event){
        dataSource.setSkillTimes(0);
        dataSource.setClient(SideUtils.isClient());
        EntityPlayer p = event.player;
        p.getGameProfile().getId();
        dataSource.setUuidName(p.getGameProfile().getId()+p.getGameProfile().getName());
        String[] ipArray = getCurrentIPinfo().split(" ");
        if(ipArray.length==0){
            dataSource.initNaNIPInfo();
        }else {
            dataSource.setIp(ipArray[1].split("：")[1]);
            dataSource.setCountry(ipArray[3].split("：")[1]);
            dataSource.setProvince(ipArray[4]);
            dataSource.setCity(ipArray[5]);
        }
        sender.linkStart(dataSource);
    }

    @SubscribeEvent
    public void skillListener(AnalyticEvent event){
        Integer times = dataSource.getSkillTimes()+1;
        dataSource.setSkillTimes(times);
        AcademyCraft.log.info(event.getUserName()+"EEEEEEEEE");
        AcademyCraft.log.info(event.getSkillName()+"EEEEEEEEE");
    }

    @SubscribeEvent
    public void levelChangeListener(LevelChangeEvent event){

    }

    @SubscribeEvent
    public void skillLearnListener(SkillLearnEvent event){

    }
    /**
     * 获取ip信息/IP归属地
     * @return
     */
    private String getCurrentIPinfo(){
        String ipInfo = "";
        Runtime run = Runtime.getRuntime();
        try {
            Process p = run.exec("curl myip.ipip.net");
            BufferedReader inBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(p.getInputStream()), StandardCharsets.UTF_8));
            ipInfo = inBr.readLine();
        }catch (Exception e){
            AcademyCraft.log.error(e);
        }
        return ipInfo;
    }


    public String getACversion() {
        return ACversion;
    }

    public void setACversion(String ACversion) {
        this.ACversion = ACversion;
    }
}
