package cn.academy.analyticUtil;

import cn.academy.AcademyCraft;
import cn.academy.analyticUtil.events.AnalyticLevelUpEvent;
import cn.academy.analyticUtil.events.AnalyticSkillEvent;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.LevelChangeEvent;
import cn.academy.event.ability.SkillLearnEvent;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkS11n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AnalyticDataListener {
    private AnalyticDto serverSource;
    private static final String CHANNEL="analysisChannel";
    private Map<String,AnalyticDto> sourceMap;
    public static final AnalyticDataListener instance = new AnalyticDataListener();
    private static AnalyticInfoSender sender;
    private AnalyticDataListener(){
        serverSource = null;
        sourceMap = new HashMap<>();
        NetworkS11n.addDirectInstance(this);
        MinecraftForge.EVENT_BUS.register(this);
        sender = new AnalyticInfoSender(20);
        sender.linkStart(sourceMap);
    }

    @SubscribeEvent
    public void loginListener(PlayerEvent.PlayerLoggedInEvent event){
        if(null==serverSource) {
            serverSource = new AnalyticDto();
            serverSource.setVersion(AcademyCraft.VERSION);
            serverSource.setUuidName("server");
            serverSource.setStartTime(new Date().getTime());
            String[] ipArray = getCurrentIPinfo().split(" ");
            if (ipArray.length == 0) {
                serverSource.initNaNIPInfo();
            } else {
                serverSource.setIp(ipArray[1].split("：")[1]);
                serverSource.setCountry(ipArray[3].split("：")[1]);
                serverSource.setProvince(ipArray[4]);
                serverSource.setCity(ipArray[5]);
            }
        }
        NetworkMessage.sendTo(event.player,this,CHANNEL,event.player);
    }

    //on Client
    @NetworkMessage.Listener(channel=CHANNEL,side=Side.CLIENT)
    public void clientInfoInitializer(EntityPlayer player){
        Boolean isServer = false;
        if(serverSource==null){
            isServer = true;
        }
        NetworkMessage.sendToServer(this,CHANNEL,getCurrentIPinfo(),player,isServer);
    }

    //receive the ip info from the client
    @NetworkMessage.Listener(channel = CHANNEL,side = Side.SERVER)
    public void serverIpCollector(String ipInfo,EntityPlayer player,Boolean isServer){
        if(isServer){
            if(!sourceMap.containsKey("server")){
                sourceMap.put("server",serverSource);
            }
        }
        AnalyticDto playerData = new AnalyticDto();
        playerData.setVersion(AcademyCraft.VERSION);
        playerData.setName(player.getName());
        playerData.setUuidName(player.getUniqueID()+player.getName());
        playerData.setStartTime(new Date().getTime());
        String[] ipArray = ipInfo.split(" ");
        if (ipArray.length == 0) {
            playerData.initNaNIPInfo();
        } else {
            playerData.setIp(ipArray[1].split("：")[1]);
            playerData.setCountry(ipArray[3].split("：")[1]);
            playerData.setProvince(ipArray[4]);
            playerData.setCity(ipArray[5]);
        }
        playerData.setLevel(AbilityData.get(player).getLevel());
        sourceMap.put(playerData.getUuidName(),playerData);
    }

    //on Server
    @SubscribeEvent
    public void skillListener(AnalyticSkillEvent event){
        EntityPlayer targetPlayer = event.getPlayer();
        String uuid = targetPlayer.getUniqueID()+targetPlayer.getName();
        if(sourceMap.containsKey(uuid)){
            Map<String,Integer> countMap = sourceMap.get(uuid).getCountMap();
            if(countMap.containsKey(event.getSkillName())){
                countMap.replace(event.getSkillName(), (countMap.get(event.getSkillName()) + 1));
            }else {
                countMap.put(event.getSkillName(),1);
            }
            sourceMap.get(uuid).sentReset();
        }
    }

    @SubscribeEvent
    public void levelUpListener(AnalyticLevelUpEvent event){
        String uuid = event.getEntityPlayer().getUniqueID() + event.getEntityPlayer().getName();
        if(sourceMap.containsKey(uuid)){
            Map<String,Integer> countMap = sourceMap.get(uuid).getCountMap();
            if(countMap.containsKey("levelUp")){
                countMap.replace("levelUp",(countMap.get("levelUp")+1));
            }else {
                countMap.put("levelUp",1);
            }
        }
    }

    @SubscribeEvent
    public void levelChangeListener(LevelChangeEvent event){
        String uuid = event.player.getUniqueID() + event.player.getName();
        sourceMap.get(uuid).setLevel(event.getAbilityData().getLevel());
    }

    @SubscribeEvent
    public void skillLearnListener(SkillLearnEvent event){
        String uuid = event.player.getUniqueID() + event.player.getName();
        if(sourceMap.containsKey(uuid)){
            Map<String,Integer> countMap = sourceMap.get(uuid).getCountMap();
            if(countMap.containsKey("skillLearn")){
                countMap.replace("skillLearn",(countMap.get("skillLearn")+1));
            }else {
                countMap.put("skillLearn",1);
            }
        }
    }

    //get ip info
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
