package cn.academy.analytic;

import java.util.HashMap;
import java.util.Map;

public class AnalyticDto {

    private String uuidName;
    private String version;
    private String ip;
    private String country;
    private String province;
    private String city;
    private Integer sent=0;
    private long startTime;
    private Integer level;
    private Map<String,Integer> countMap=new HashMap<>();

    public void initNaNIPInfo(){
        ip="";
        country="";
        province="";
        city="";
    }

    public void sentReset(){
        sent=0;
    }

    public void sentPlus(){
        sent++;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUuidName() {
        return uuidName;
    }

    public void setUuidName(String uuidName) {
        this.uuidName = uuidName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Integer> getCountMap() {
        return countMap;
    }

    public void setCountMap(Map<String, Integer> countMap) {
        this.countMap = countMap;
    }

    public Integer getSent() {
        return sent;
    }

    public void setSent(Integer sent) {
        this.sent = sent;
    }


    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
