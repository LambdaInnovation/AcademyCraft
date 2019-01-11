package cn.academy.analyticUtil;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AnalyticInfoSender {
//    public void postSender(AnalyticDto dataSource) throws Exception {
//        String url = "http://www.baidu.com";
//        BeanMap testMap = new BeanMap(dataSource);
//        List<NameValuePair> params = new ArrayList<>();
//        for (Object key : testMap.keySet()) {
//            params.add(new BasicNameValuePair(key.toString(), testMap.get(key).toString()));
//        }
//     //   BeanUtils.populate(dataSource, hashMap);
//        HttpClient client = HttpClients.createDefault();
//        HttpPost post = new HttpPost(url);
//        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
//        HttpResponse response = client.execute(post);
//        //HttpEntity entity = response.getEntity();
//    }
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
    AnalysisTask(Map<String,AnalyticDto> sourceMap){
        this.sourceMap=sourceMap;
    }

    @Override
    public void run() {

    }
}
