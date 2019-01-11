package cn.academy.analyticUtil;

import cn.academy.AcademyCraft;
import cn.academy.analyticUtil.events.AnalyticLevelUpEvent;
import cn.academy.analyticUtil.events.AnalyticSkillEvent;
import cn.academy.event.ability.SkillLearnEvent;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AnalyticDataListener {
    private AnalyticDto serverSource;
    private static final String CHANNEL="analysisChannel";
    private Map<String,AnalyticDto> sourceMap;
    public static final AnalyticDataListener instance = new AnalyticDataListener();
    private static AnalyticInfoSender sender = new AnalyticInfoSender(300);

    private AnalyticDataListener(){
        serverSource = null;
        sourceMap = new HashMap<>();
        NetworkS11n.addDirectInstance(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void loginListener(PlayerEvent.PlayerLoggedInEvent event){
        if(null==serverSource) {
            serverSource = new AnalyticDto();
            serverSource.setVersion(AcademyCraft.VERSION);
            serverSource.setClient(SideUtils.isClient());
            serverSource.setUuidName("server");
            String[] ipArray = getCurrentIPinfo().split(" ");
            if (ipArray.length == 0) {
                serverSource.initNaNIPInfo();
            } else {
                serverSource.setIp(ipArray[1].split("：")[1]);
                serverSource.setCountry(ipArray[3].split("：")[1]);
                serverSource.setProvince(ipArray[4]);
                serverSource.setCity(ipArray[5]);
            }
            sourceMap.put("server",serverSource);
            sender.linkStart(sourceMap);
        }
        NetworkMessage.sendTo(event.player,this,CHANNEL,event.player);
    }

    @NetworkMessage.Listener(channel=CHANNEL,side=Side.CLIENT)
    public void clientInfoInitializer(EntityPlayer player){
        AcademyCraft.log.info(player.getName());
    }

    @NetworkMessage.Listener(channel = CHANNEL,side = Side.SERVER)
    public void testA(){
        AcademyCraft.log.info("serverGet");
    }

    @NetworkMessage.Listener(channel = CHANNEL,side = Side.CLIENT)
    public void testB(){
        AcademyCraft.log.info("clientGet");
    }

    //on Server
    @SubscribeEvent
    public void skillListener(AnalyticSkillEvent event){
        AcademyCraft.log.info(event.getUserName()+"EEEEEEEE");
        AcademyCraft.log.info(event.getSkillName()+"EEEEEEEEE");

    }
    //on Server
    @SubscribeEvent
    public void levelUpListener(AnalyticLevelUpEvent event){
        AcademyCraft.log.info(event.getEntityPlayer().getUniqueID() + event.getEntityPlayer().getName());
    }

    @SubscribeEvent
    public void skillLearnListener(SkillLearnEvent event){
        event.player.getName();
        event.player.getUniqueID();
        AcademyCraft.log.info(event.player.getName()+event.skill.getName());
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

}
