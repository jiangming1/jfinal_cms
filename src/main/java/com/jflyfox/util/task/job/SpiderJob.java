package com.jflyfox.util.task.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.kit.FileKit;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jflyfox.modules.admin.image.model.TbImage;
import com.jflyfox.modules.admin.image.model.TbImageAlbum;
import com.jflyfox.modules.admin.site.SiteService;
import com.jflyfox.modules.admin.site.TbSite;
import com.jflyfox.system.user.SysUser;
import com.jflyfox.util.DateUtils;
import com.jflyfox.util.FileUploadUtil;
import com.xiaoleilu.hutool.http.HttpUtil;
import com.xiaoleilu.hutool.util.FileUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import sun.nio.ch.FileKey;

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
        public boolean callback(String category, String id, String title, List<String> pictures) {
            // 打印数据
//            Map<String, Object> data = new HashMap<>();
//            data.put("id", id);
//            data.put("title", title);
//            data.put("pictures", pictures);
//            System.out.println(JSON.toJSONString(data));
            String fileDir = PathKit.getWebRootPath() + File.separator + "jflyfox" + File.separator + "photo" + File.separator + "image" + File.separator;
            String pid;
            try {
                Record p = Db.findFirst("SELECT ID FROM tb_image_album WHERE name = '" + category + "' ");
                pid = String.valueOf(p.get("ID"));
            }catch (Exception e) {
                pid =  saveIbum(null,category,"");
            }

            List remarkList = TbImageAlbum.dao.find("SELECT * FROM tb_image_album WHERE remark = ? " ,id);
            title = title.replaceAll(" ","_");
            title += id.substring(id.length()-2,id.length());
            // 如果之前已经录入该商品，后面的数据就不爬取了
            if (remarkList.size() > 0) return true; // true: 之前已经录入过了
            // 创建目录
            pid =  saveIbum(pid,title,id);
            for(int i=0;i<pictures.size();i++){
                try {
                    String fileExt = pictures.get(i).substring(pictures.get(i).lastIndexOf(".")+1);
                    String fileName = DateUtils.getNow("yyyyMMdd_HHmmss") + "_" //
                            + new SecureRandom().nextInt(999999) + "." + fileExt;
                    //创建上传图片目录
                    File file = new File(fileDir, fileName);
                    HttpUtil.downloadFile(pictures.get(i), file);
                    //保存图片
                    saveImage(file.getAbsolutePath(),"/jflyfox/photo/image/" + fileName, fileName, fileExt, pid, title);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    };

    public static void main(String[] args) {
        System.out.println();
    }


    // -----------------------------------------------------------------------------------------------------------

    private static Map<String, String> categories = new HashMap<>();
    static {
        categories.put("内衣", "http://www.sxxl.com/StyleGallery-index-extid-32940-cid-2-channel-32209.html");
    }

    public void doSpiderJob(SpiderJobCallable callable) throws IOException {
        // 登入
        String ssoUrl = login();
        System.out.println("sso: " + ssoUrl);
        // 单点登入
        sso(ssoUrl);
        for (String categoryName : categories.keySet()) {
            String categoryUrl = categories.get(categoryName);
            // 请求页面
            String json = pageDate(categoryUrl, 1);
            // 提取总页数
            int totalPage = totalPage(json);
            System.out.println(totalPage);
            // 开始抓取数据
            for (int i = 1; i <= totalPage; i++) {
                String dataJson = pageDate(categoryUrl, i);
                // 从json提出去有用的数据
                boolean isBreak = fetchUsableData(categoryName, dataJson, callable);
                System.out.println("第"+i+"页数据获取完毕！");
                if (isBreak) break;
            }
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
    }

    // 蕾丝列表页面请求
    private static String pageDate(String categoryUrl, int page) throws IOException {
        Connection conn = Jsoup.connect(categoryUrl)
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

    private static boolean fetchUsableData(String categoryName, String dataJson, SpiderJobCallable callable) throws IOException {
        System.out.println(dataJson);
        JSONObject jsonObj = JSON.parseObject(dataJson);
        JSONArray list = jsonObj.getJSONArray("list");
        for (int i = 0; i < list.size(); i++) {
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
            boolean isBreak = callable.callback(categoryName, id, title, pictures);
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
    //------------------------------------------------------------------------------------------------------------------


    //保存图片到本地
    private boolean saveImage(String imgPath,String imageUrl,String fileName ,String fileExt,String pid ,String albumTitle){
        TbImage model = new TbImage();

        model.setAlbumId(Integer.valueOf(pid));
        model.setAlbumName(albumTitle);
        model.setName(fileName);
        model.setImageUrl(imageUrl);
        model.setLinkurl(imgPath);
        model.setExt(fileExt);
//        model.setImageNetUrl(imageNetUrl)

//        SysUser user = SysUser.dao.findFirstCache("user_job","getUser","select * from sys_user where username = 'admin' ");
        Integer userid =1;
        String now = DateUtils.getNow(DateUtils.DEFAULT_REGEX_YYYY_MM_DD_HH_MIN_SS);
        // 新增
        model.remove("id");
        model.setCreateId(userid);
        model.setCreateTime(now);
        model.save();

        return false;
    }

    //创建相册
    private String saveIbum(String pid ,String title,String remark){
        TbImageAlbum model = new TbImageAlbum();

        if (pid==null){
            model.setParentId(0);
        }else{
            model.setParentId(Integer.valueOf(pid));
        }
        model.setName(title);
        model.setSort(1);
        Integer userid = 1; // admin用户
        String now = DateUtils.getNow(DateUtils.DEFAULT_REGEX_YYYY_MM_DD_HH_MIN_SS);

        model.remove("id");
        model.setCreateId(userid);
        model.setCreateTime(now);
        model.setRemark(remark);
        model.setName(title);
        model.save();
        pid = String.valueOf(model.get("id"));

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
