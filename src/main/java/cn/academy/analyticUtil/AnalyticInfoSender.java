package cn.academy.analyticUtil;

import cn.academy.AcademyCraft;
import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

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
    String voidclRBQ="http://144.34.208.247:8080/lambda";
    AnalysisTask(Map<String,AnalyticDto> sourceMap){
        this.sourceMap=sourceMap;
    }
    
    @Override
    public void run() {
        try {
            postSender(sourceMap);
        }catch (Exception e){
            AcademyCraft.log.error(e.getMessage());
        }
    }

    private void postSender(Map<String,AnalyticDto> sourceMap) throws Exception{
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
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(voidclRBQ);
            StringEntity requestEntity = new StringEntity(paramsJson, ContentType.APPLICATION_JSON);
            post.setEntity(requestEntity);
            if(params.size()!=0) {
                client.execute(post);
                AcademyCraft.log.info(paramsJson);
            }
        }
    }
}
