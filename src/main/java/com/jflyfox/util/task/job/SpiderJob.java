package com.jflyfox.util.task.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.config.Routes;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.upload.UploadFile;
import com.jflyfox.component.util.ImageModel;
import com.jflyfox.component.util.ImageUtils;
import com.jflyfox.jfinal.base.SessionUser;
import com.jflyfox.modules.admin.image.model.TbImage;
import com.jflyfox.modules.admin.image.model.TbImageAlbum;
import com.jflyfox.modules.admin.image.model.TbImageTags;
import com.jflyfox.modules.admin.site.SessionSite;
import com.jflyfox.modules.admin.site.SiteService;
import com.jflyfox.modules.admin.site.TbSite;
import com.jflyfox.system.file.model.FileUploadBean;
import com.jflyfox.system.file.util.FileUploadUtils;
import com.jflyfox.system.user.SysUser;
import com.jflyfox.util.DateUtils;
import com.jflyfox.util.FileUploadUtil;
import com.jflyfox.util.StrUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by linzuk on 2018/2/26.
 * 爬虫任务：每天定时爬取一个网站的数据
 */
public class SpiderJob implements Runnable {

    private SpiderJobCallable callable = new SpiderJobCallable() {
        @Override
        public boolean callback(String id, String title, List<String> pictures) {
            // 打印数据
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("title", title);
            data.put("pictures", pictures);
            System.out.println(JSON.toJSONString(data));
            String pid = "";
            Record p = Db.findFirst("SELECT ID FROM tb_image_album WHERE name = '内衣' ");
            pid = String.valueOf(p.get("ID"));
            List remarkList = TbImageAlbum.dao.find("SELECT * FROM tb_image_album WHERE remark = ? " ,id);
            title = title.replaceAll(" ","_");
//            String[] titles = title.split("/");
            if(remarkList.size()==0){
                //创建目录
                pid =  saveIbum(pid,title,id);
                for(int i=0;i<pictures.size();i++){
                    try {
                        String fileExt = pictures.get(i).substring(pictures.get(i).lastIndexOf(".")+1);
                        String fileName = DateUtils.getNow("yyyyMMdd_HHmmss") + "_" //
                                + new SecureRandom().nextInt(999999) + "." + fileExt;
                        //创建上传图片目录
                        FileUploadUtil.uploadImgLW(pictures.get(i).toString(),"/jflyfox/photo/image/" ,fileName);
                        if(i==pictures.size()-1){
                            //保存图片
                            TbSite site = getBackSite();
                            //UploadFile uploadImage = getBackSite(FileUploadUtil.getProjectPath()+"/u/py/" + title + String.valueOf(i+1) +".jpg", FileUploadUtils.getUploadTmpPath(site), FileUploadUtils.UPLOAD_MAX);
                            saveImage("http://localhost:8083/jflyfox/photo/image/" + fileName,"/jflyfox/photo/image/" + fileName,fileName,fileExt,pid);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // TODO: 将数据录入系统就好
            return false; // true: 之前已经录入过了
        }
    };
    public TbSite getBackSite() {
        SysUser user = SysUser.dao.findFirstByWhere("where userid = ?",1);
        if (user == null) {
            return null;
        }
        TbSite site = new SiteService().getSite(user.getBackSiteId());
        return site;
    }
    private boolean saveImage(String imgPath,String imageUrl,String title ,String  fileExt,String pid){
        TbImage model = new TbImage();

        model.put("album_id",pid);
        model.put("album_name",title);
        model.put("image_url",imageUrl);
        model.put("linkurl",imgPath);
//        model.put("image_net_url",imgNetUrl);
        model.put("ext",fileExt);

        Integer userid = 1;
        String now = DateUtils.getNow(DateUtils.DEFAULT_REGEX_YYYY_MM_DD_HH_MIN_SS);
        model.put("update_id", userid);
        model.put("update_time", now);
        // 新增
        model.remove("id");
        model.put("create_id", userid);
        model.put("create_time", now);
        model.save();

        return false;
    }

    public void doSpiderJob(SpiderJobCallable callable) throws IOException {
        // 登入
        String ssoUrl = login();
        System.out.println("sso: " + ssoUrl);
        // 单点登入
        sso(ssoUrl);
        // 请求页面
        String json = pageDate(1);
        // 提取总页数
        int totalPage = totalPage(json);
        System.out.println(totalPage);
        // 开始抓取数据
        for (int i = 1; i <= totalPage; i++) {
            String dataJson = pageDate(i);
            System.out.println(i + " : " + dataJson);
            // 从json提出去有用的数据
            boolean isBreak = fetchUsableData(dataJson, callable);
            System.out.println("第"+i+"页数据获取完毕！");
            if (isBreak) break;
        }
    }

    // APP登入请求
    private static String login() throws IOException {
        Connection conn = Jsoup.connect("http://sso.diexun.com/ClientsLogin/index")
                .cookies(cookies)
                .timeout(10*1000)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36")
                .ignoreContentType(true)
                .data("info", "eyJjcHVfaW5mbyI6IjIwMTcxMTIwMDkwNjU3NDY0IiwiUGNUeXBlIjoiMTAwIiwiaGRfaW5mbyI6InVuZGVmaW5lZCIsIlZlciI6IjEuMi4wIiwicGFzc3dvcmQiOiI2NTQzMjEiLCJwY19uYW1lIjoiaVBhZDYsMTEiLCJ1c2VybmFtZSI6ImRlc2hlbmcxNjkiLCJhdXRoa2V5IjoiZTE5NGJlMDlmNzlhYTZiZDA5OGJhOTg1YjRmZWI0OGEiLCJuZXRfaW5mbyI6InVuZGVmaW5lZCIsImFwcGlkIjoiMiJ9");
        Connection.Response resp = conn.method(Connection.Method.POST).execute();
        cookies.putAll(encodeAll(resp.cookies()));
        JSONObject json = JSON.parseObject(resp.body());
        Integer status = json.getInteger("status");
        if (2 != status) {
            System.out.println("登入失败: " + resp.body());
            throw new RuntimeException("登入失败");
        }
        return json.getString("uri");
    }

    // 网页单点登入请求
    private static void sso(String ssoUrl) throws IOException {
        Connection conn = Jsoup.connect(ssoUrl).cookies(cookies).timeout(10*1000);
        Connection.Response resp = conn.method(Connection.Method.GET).execute();
        cookies.putAll(encodeAll(resp.cookies()));
        String body = resp.body();
        System.out.println(body);
    }

    // 蕾丝列表页面请求
    private static String pageDate(int page) throws IOException {
        Connection conn = Jsoup.connect("http://www.sxxl.com/StyleGallery-index-extid-32940-cid-2-channel-32209.html")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36")
                .data("p", "" + page)
                .ignoreContentType(true)
//                .header("Cookie", "screenWidth=1920; 28ae21cdfc8d8236e4d783cb94411e97=d6advWeQCXkmIYGSMRWnbCwnzmCRlRwzAeNjPGBQ9BdoGtegtLLJ; haveShownLatest_2085=1; haveShownLatest_32209=1; lastViewCategory=2; %5F_ad_view=19; %5F_ad_date=1519380166; Hm_lvt_a3b242930672e1d3dd7781c8cd80b09a=1519369183,1519375791; Hm_lpvt_a3b242930672e1d3dd7781c8cd80b09a=1519380167; loginInfo=ec37sjnR400qk0Lob07xBmvN6tTFy416rMvgSpaj94VCs0ATdW0p67IzvrH5QSpQwTqvQUbPqsSD2zj0Bieilh0PIEiKpncZABZtrHNeuGeKVDSrxCeMbhkmpBsyxuqIDS3iG%2BykAQq4ZXehcwSlPfhG8n9b6ezAzwSIk5fknE%2F%2BRQLuMNJHO8lV%2FRiMeSj%2BMrfQ6SKmYQLTzyoCyKF16z%2BqAxaWDA1QGWRzoBhzu5ya91pIJ3R89jXYtD4rt4yTKNT8yHtdRrJsKsmnc4a5rhGej71OScCGvfCL4Ow252k5YcQqeP95A84tcwVSJcqa4gi1Pw3PrJy%2FX48BrghdtCup7dbJvjS9V53OQ69LW8QmM0dRonoQQ7tXrwNQjkESDF27SBpvzwd9q8NOSRUqCNqxgLA0bClfoNYe7GVoaeMISQ59FOPdB%2F9%2Bliopza3xx%2FJ5URBSgWE8di6TuD2OlfSp2m70eL53mt6PR721Ky81jnSEZIJgEQ8ULCwzNtATp780uBYjxy%2FZrra3BXoHxen338WNjhDo4%2BN7ifU015nwhiS7UiAtkle%2FXTMLCfgmOo1QNYz%2BMaF9jxSW8fiog4Y7Hjp8OSKSVUsVGBLxL6tzclo9t2A2OApxND0TTvpa0MMxtKhUm1bEyiPdfWfakgAle8OAar0teWXxsnn5b5DtJmegvkzIa1%2B0MBo5LvztD9FEZuYx10NMCPNS35HjZgrSE4kVOdY8tSg3V93Utqc%2BNsUiSr1Int%2FIE8tqhblUKBSaI9Ni5bEFISS8hQ0BrbC7vRxGoUQTEKEXfHLmqynAWlzhu4b%2Fpz0QmYqkOg%2BR8wopY7JZP240mx%2B5TgYnlnMLghQztf0mRipoUxIQFMbNzLctfaTYTf38JqonmXpJ5wnLkSa8u4O0uTTHfdMFtQX1XMwwd2q6fLxUYOtUKl3n1kU9%2FrXFTXhPX9Fms4SSsJGw%2BwjsODzFi3Qog7dCV9WyzJVurcqT583NNxZfDVRtAnrIgp%2F4lZ0GwTP8GbYZPQitG3Un4Pt21Pl9gCxDA6QFjWZ%2FDOGrtr%2B6FkA; PHPSESSID=26554c3447ec7feea70eeda2cda28232; 73935d94db91e82f4bbec2322d03f55a=d6advWeQCXkmIYGSMRWnbCwnzmCRlRwzAeNjPGBQ9BdoGtegtLLJ; lastViewChannel=32209");
                .cookies(cookies);
        Connection.Response resp = conn.method(Connection.Method.GET).cookies(cookies).timeout(60*1000).execute();
        cookies.putAll(encodeAll(resp.cookies()));
        Pattern p = Pattern.compile("var pageData = .*?;\tvar extid");
        Matcher m = p.matcher(resp.body());
        String pageData = m.find() ? m.group(0) : "";
        return pageData.substring("var pageData = ".length(), pageData.length() - ";\tvar extid".length());
    }

    private static int totalPage(String json) {
        JSONObject jsonObj = JSON.parseObject(json);
        String pageMin = jsonObj.getString("page_min");
        Pattern p = Pattern.compile("</strong>/.*?&nbsp;&nbsp;");
        Matcher m = p.matcher(pageMin);
        String page = m.find() ? m.group(0) : "";
        return Integer.parseInt(page.substring("</strong>/".length(), page.length() - "&nbsp;&nbsp;".length()));
    }

    private static boolean fetchUsableData(String dataJson, SpiderJobCallable callable) throws IOException {
        System.out.println(dataJson);
        JSONObject jsonObj = JSON.parseObject(dataJson);
        JSONArray list = jsonObj.getJSONArray("list");
        for (int i = 0; i < 1; i++) {//list.size()
            JSONObject obj = list.getJSONObject(i);
            // 信息提取: id、title
            String id = obj.getString("id");
            String title = obj.getString("picture_title");
            // 信息提取: 图片
            JSONArray subsidiaries = obj.getJSONArray("subsidiary");
            List<String> pictures = new LinkedList<>();
            if (null == subsidiaries) {
                String vipPicture = obj.getString("vip_picture");
                pictures.add(vipPicture);
            } else {
                for (int j = 0; j < subsidiaries.size(); j++) {
                    JSONObject subsidiary = subsidiaries.getJSONObject(j);
                    String vipPicture = subsidiary.getString("vip_picture");
                    pictures.add(vipPicture);
                }
            }
            // 回调数据
            boolean isBreak = callable.callback(id, title, pictures);
            if (isBreak) return true;
        }
        return false;
    }

    private static Map<String, String> cookies = new HashMap<>(); // 保存cookie

    private static Map<String, String> encodeAll(Map<String, String> m) {
        Set<String> keys =  m.keySet();

        for (String key : keys) {
            m.put(key, m.get(key).replaceAll("%252","%2"));
        }
        return m;
    }

   // @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("################任务执行#################");
//        try {
//            // 执行作业
//            doSpiderJob(callable);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private String saveIbum(String pid ,String title,String remark){
        TbImageAlbum model = new TbImageAlbum();

            model.setParentId(Integer.valueOf(pid));
            model.setName(title);
            model.setSort(1);

            Integer userid = 1;// admin用户
            String now = DateUtils.getNow(DateUtils.DEFAULT_REGEX_YYYY_MM_DD_HH_MIN_SS);
            model.put("update_id", userid);
            model.put("update_time", now);
            model.remove("id");
            model.put("create_id", userid);
            model.put("create_time", now);
            model.put("remark",remark);
            model.put("name",title);
            model.save();
            pid = String.valueOf(model.get("id"));


//        String[] ibums = title.split("/");
//        Integer pid = null ;
//        for(int i =0 ; i <ibums.length;i++){
//            TbImageAlbum model = new TbImageAlbum();
//
//            if(i==0){
//                model.put("remark",id);
//                pid = 5;
//            }
//            model.setParentId(pid);
//            model.setName(ibums[i]);
//            model.setSort(1);
//
//            Integer userid = 1;// admin用户
//            String now = DateUtils.getNow(DateUtils.DEFAULT_REGEX_YYYY_MM_DD_HH_MIN_SS);
//            model.put("update_id", userid);
//            model.put("update_time", now);
////            if (pid != null && pid > 0) { // 更新
////                model.update();
////            } else { // 新增
//                model.remove("id");
//                model.put("create_id", userid);
//                model.put("create_time", now);
//                model.save();
////            }
//             pid = model.get("id");
//        }
        return pid;
    }

    @Override
    public void run() {
        System.out.println("################任务执行 cron4j#################");
        try {
            // 执行作业
            doSpiderJob(callable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
