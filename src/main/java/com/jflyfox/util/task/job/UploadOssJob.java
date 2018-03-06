package com.jflyfox.util.task.job;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.jfinal.kit.LogKit;
import com.jflyfox.modules.admin.image.model.TbImage;
import com.jflyfox.util.DateUtils;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadOssJob implements Runnable {
    @Override
    public void run() {
        System.out.println("################任务执行 cron4j uploadOss #################");
        // 1、获取需要上传的文件
        String sql = " SELECT * FROM tb_image WHERE image_net_url IS NULL AND is_recommend=2 LIMIT 30 ";
        List<TbImage> tbImages = TbImage.dao.find(sql);
        if (tbImages == null || tbImages.size() == 0) return;
        // 2、上传文件
        for (TbImage tbImage : tbImages) {
            String filePath = tbImage.getLinkurl();
            String ossFileUrl = uploadFileToOss(new File(filePath));
            if (null != ossFileUrl) {
                tbImage.setImageNetUrl(ossFileUrl);
            }
            tbImage.setIsRecommend(1);//已执行上传oss的改推荐状态改为1
            tbImage.setUpdateId(1);//admin用户
            String now = DateUtils.getNow(DateUtils.DEFAULT_REGEX_YYYY_MM_DD_HH_MIN_SS);
            tbImage.setUpdateTime(now);
            // 3、将oss图片填入数据库
            tbImage.update();
        }

    }

    private static String endpoint = "oss-cn-hangzhou.aliyuncs.com";
    private static String accessKeyId = "LTAIQ8SAOtynX6CU";
    private static String accessKeySecret = "WGpXHi4wsNZMWKNYCPAGDaNS1aOq5I";
    private static String bucketName = "eswn-spider";

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    public String uploadFileToOss(final File file) {
        if (!file.exists()) {
            LogKit.info("file not exist: " + file);
            return null;
        }
        OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
        try {
            // oss 初始化
            if (!ossClient.doesBucketExist(bucketName)) {
                LogKit.info("Creating bucket " + bucketName);
                ossClient.createBucket(bucketName);
                CreateBucketRequest createBucketRequest= new CreateBucketRequest(bucketName);
                createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
                ossClient.createBucket(createBucketRequest);
            }
            // 上传文件
            uploadToOss(ossClient, "upload/" + file.getName(), file.getAbsolutePath());
            // 上传完后删除本地图片
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    file.delete();
                }
            });
            return "http://eswn-spider.oss-cn-hangzhou.aliyuncs.com/upload/" + file.getName();
        } catch (OSSException oe) {
            String msg = ("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.\n") +
                    "Error Message: " + oe.getErrorCode() + "\n" +
                    "Error Code:       " + oe.getErrorCode() + "\n" +
                    "Request ID:      " + oe.getRequestId() + "\n" +
                    "Host ID:           " + oe.getHostId();
            LogKit.error(msg);
            return null;
        } catch (ClientException ce) {
            String msg = ("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.\n") +
                    "Error Message: " + ce.getMessage();
            LogKit.error(msg);
            return null;
        } catch (Throwable e) {
            LogKit.error(e.getMessage());
            return null;
        } finally {
            ossClient.shutdown();
        }
    }

    private void uploadToOss(OSSClient ossClient, String fileName, String filePath) throws Throwable {
        ossClient.putObject(new PutObjectRequest(bucketName, fileName, new File(filePath)));
        boolean exists = ossClient.doesObjectExist(bucketName, fileName);
        LogKit.info("Does object " + bucketName + " exist? " + exists);
    }
}
