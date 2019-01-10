package cn.academy.analyticUtil;

import cn.academy.AcademyCraft;

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
    public void linkStart(AnalyticDto dataSource){
        TimerTask task = new AnalysisTask(dataSource);
        Timer timer = new Timer();
        timer.schedule(task,0,second*1000);
    }
}

class AnalysisTask extends TimerTask{
    private AnalyticDto dataSource;
    AnalysisTask(AnalyticDto dataSource){
        this.dataSource=dataSource;
    }

    @Override
    public void run() {
        if(dataSource.isSended()) {
            AcademyCraft.log.info("nekoChan");
        }else {
            AcademyCraft.log.info("tsumetaiRaito");
            dataSource.setSended(true);
        }
        AcademyCraft.log.info(dataSource.getSkillTimes());
    }
}
