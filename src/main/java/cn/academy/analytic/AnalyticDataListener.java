package cn.academy.analytic;

import cn.academy.AcademyCraft;
import cn.academy.analytic.events.AnalyticLevelUpEvent;
import cn.academy.analytic.events.AnalyticSkillEvent;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

// Collects some **anonymous** analytics data
//  What we collect: a unique hash of user, location, in-mod action (Level-up, use ability, etc.)
//  The data is used to solely help us better understand player behaviour and improve the mod.
//  The data is not associated to each player, has nothing personal, so it can only be used for large-scale statistics,
//      and certainly compliants to contemporary data protection regulations (GDPR, etc.)
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
        sender = new AnalyticInfoSender(600);
        sender.linkStart(sourceMap);
    }

    @SubscribeEvent
    public void loginListener(PlayerEvent.PlayerLoggedInEvent event){
        Thread delaySender = new DelaySender(event.player);
        delaySender.start();//avoid sending message to the client thread before it hasn't been initialized
    }

    public void serverGetter(EntityPlayer player,String serverIp){
        if(null==serverSource) {
            serverSource = new AnalyticDto();
            serverSource.setVersion(AcademyCraft.VERSION);
            serverSource.setUuidName("server");
            serverSource.setStartTime(UTCZeroTime());
            String[] ipArray =serverIp.split(" ");
            if (ipArray.length < 6) {
                serverSource.initNaNIPInfo();
            } else {
                serverSource.setIp(ipArray[1].split("：")[1]);
                serverSource.setCountry(ipArray[3].split("：")[1]);
                serverSource.setProvince(ipArray[4]);
                serverSource.setCity(ipArray[5]);
            }
        }
        NetworkMessage.sendTo(player,this,CHANNEL,player,serverIp);
    }

    //on Client
    @NetworkMessage.Listener(channel=CHANNEL,side=Side.CLIENT)
    public void clientInfoInitializer(EntityPlayer player , String serverIp){
        Boolean isServer = false;
        String tempIp = getCurrentIPinfo();
        if(serverSource==null||(!serverIp.equals(tempIp))){
            isServer = true;
        }
        NetworkMessage.sendToServer(this,CHANNEL,tempIp,player,isServer);
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
        playerData.setUuidName(SHA(player.getUniqueID()+player.getName()));
        playerData.setStartTime(UTCZeroTime());
        String[] ipArray = ipInfo.split(" ");
        if (ipArray.length < 6) {
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
        String uuid = SHA(targetPlayer.getUniqueID()+targetPlayer.getName());
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
        String uuid = SHA(event.getEntityPlayer().getUniqueID() + event.getEntityPlayer().getName());
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
        String uuid = SHA(event.player.getUniqueID() + event.player.getName());
        if(sourceMap.containsKey(uuid)) {
            sourceMap.get(uuid).setLevel(event.getAbilityData().getLevel());
        }
    }

    @SubscribeEvent
    public void skillLearnListener(SkillLearnEvent event){
        String uuid = SHA(event.player.getUniqueID() + event.player.getName());
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
    public String getCurrentIPinfo(){
        String ipInfo = "";
        try {
            URL object = new URL("https://myip.ipip.net");
            HttpURLConnection con = (HttpURLConnection)object.openConnection();
            con.setRequestProperty("User-Agent","");
            con.setConnectTimeout(5000);
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine())!=null){
                sb.append(line);
            }
            ipInfo = sb.toString();

        }catch (Exception e){
            AcademyCraft.log.error(e);
        }
        if (ipInfo == null){
            ipInfo = "";
        }
        return ipInfo;
    }

    private String SHA(final String strText){
        String strResult="";
        String strType = "SHA-256";
        if(strText != null && strText.length()>0){
            try {
                MessageDigest messageDigest = MessageDigest.getInstance(strType);
                messageDigest.update(strText.getBytes());
                byte[] byteBuffer = messageDigest.digest();
                StringBuilder strHexString = new StringBuilder();
                for (byte aByteBuffer : byteBuffer){
                    String hex = Integer.toHexString(0xff & aByteBuffer);
                    if(hex.length() == 1){
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                strResult = strHexString.toString();
            }catch (NoSuchAlgorithmException e){
                AcademyCraft.log.error(e);
            }
        }
        return strResult;
    }

    private long UTCZeroTime(){
        SimpleDateFormat df =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
        String currDate = df.format(new Date());
        Timestamp timestamp = Timestamp.valueOf(currDate);
        return timestamp.getTime();
    }

}

class DelaySender extends Thread{
    private EntityPlayer player;
    private String serverIp;
    DelaySender(EntityPlayer player){
        this.player=player;
        this.serverIp=serverIp;
    }
    @Override
    public void run() {
        try {
            Thread.sleep(10000);
            serverIp = AcademyCraft.analyticDataListener.getCurrentIPinfo();
            AcademyCraft.analyticDataListener.serverGetter(player,serverIp);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
