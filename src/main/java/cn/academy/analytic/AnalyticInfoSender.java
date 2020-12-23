package cn.academy.analytic;

import cn.academy.AcademyCraft;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AnalyticInfoSender {
    private final int second;
    public AnalyticInfoSender(int second){
        this.second = second;
    }
    public void linkStart(Map<String,AnalyticDto> sourceMap){
        TimerTask task = new AnalysisTask(sourceMap);
        Timer timer = new Timer();
        timer.schedule(task,0,second*1000);
    }
}

class AnalysisTask extends TimerTask{
    private Map<String,AnalyticDto> sourceMap;
    private String voidclRBQ="https://ac.li-dev.cn/analytics";
    AnalysisTask(Map<String,AnalyticDto> sourceMap){
        this.sourceMap=sourceMap;
    }
    
    @Override
    public void run() {
        try {
            postSender(sourceMap);
        }catch (Exception e){
            AcademyCraft.log.error(e);
        }
    }

    private void postSender(Map<String,AnalyticDto> sourceMap) throws Exception{
        URL object = new URL(voidclRBQ);
        HttpURLConnection con = (HttpURLConnection)object.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type","application/json");
        con.setRequestProperty("Accept","application/json");
        OutputStream wr = con.getOutputStream();
        Map<String,AnalyticDto> params=new HashMap<>();
        if(sourceMap.size()!=0) {
            for(String key:sourceMap.keySet()){//if a data is sent 6 times(an hour) without update , it will be removed
                Integer flag = sourceMap.get(key).getSent();
                if(flag<=5){
                    params.put(key,sourceMap.get(key));
                    sourceMap.get(key).sentPlus();
                }
            }
            Gson gson = new Gson();
            String paramsJson = gson.toJson(params);
            if(params.size()!=0) {
                try {
                    wr.write(paramsJson.getBytes(StandardCharsets.UTF_8));
                    wr.flush();
                    con.getResponseCode();
                    con.getResponseMessage();
                }catch (Exception e){
                    AcademyCraft.log.info("offLine mode");
                }
                //AcademyCraft.log.info(paramsJson);
            }
        }
    }
}
