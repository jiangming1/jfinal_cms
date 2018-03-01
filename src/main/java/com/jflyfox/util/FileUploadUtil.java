package com.jflyfox.util;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @ClassName: FileUtile
 * @Description: TODO(文件操作工具包)
 *
 */
public class FileUploadUtil {



    private static List<File> list = new ArrayList<File>(0);
    private static List<String> listImages = null;
    private static List<String> listVideos = null;
    private static List<String> listVoices = null;
    private static List<String> listFiles = null;
    private static List<String> listBooks = null;
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSS");
    private static Date currentTime = new Date();
    static {
        list = new ArrayList<File>(0);
        String imageTypes = "gif,jpg,jpeg,png,bmp";
        String videosTypes = "swf,flv,mp4,wav,wma,wmv,mid,avi,mpg,asf,rm,rmvb";
        String voiceTypes = "mp3,aac,wav,wma,cda,flac,m4a,mid,mka,mp2,mpa,mpc,ape,ofr,ogg,ra,wv,tta,ac3,dts,m4r";
        String fileTypes = "doc,docx,xls,xlsx,ppt,htm,html,txt,zip,rar,gz,bz2";
        String bookTypes = "epub,xml,pdf";
        listBooks = Arrays.asList(bookTypes.split(","));
        listImages = Arrays.asList(imageTypes.split(","));
        listVideos = Arrays.asList(videosTypes.split(","));
        listVoices = Arrays.asList(voiceTypes.split(","));
        listFiles = Arrays.asList(fileTypes.split(","));
    }

    /**
     * 获取系统信息
     *
     * @Title: getSystemInfo
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @return
     * @return String 返回类型
     */
    public static String getSystemInfo() {
        Properties props = System.getProperties(); // 获得系统属性集
        String osName = props.getProperty("os.name"); // 操作系统名称
        String osArch = props.getProperty("os.arch"); // 操作系统构架
        String osVersion = props.getProperty("os.version"); // 操作系统版本
        return osName + osArch + osVersion;
    }

    /**
     * 将map中的值存在属性文件中
     *
     * @param map
     * @param outFile
     *            生成的目标属性文件
     */
    public static void storePropertiesToFile(Map<String, Object> map,
                                             File outFile) {
        try {
            if (!map.isEmpty()) {
                if (!outFile.exists()) { // 如果目标文件不存在则创建
                    outFile.getParentFile().mkdirs();
                    outFile.createNewFile();
                }
                // outFile.createNewFile();
                OutputStream out = new FileOutputStream(outFile);
                Properties properties = new Properties();
                Set<String> keys = map.keySet();
                for (String key : keys) {
                    properties.setProperty(key, map.get(key).toString());
                }
                properties.store(out, "这是是提示");
                out.close();
                System.out.println("创建属性文件完成");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取属性文件中制定的键值
     *
     * @param key
     *            键名
     * @param filePath
     *            属性文件的路径
     * @return
     */
    public static Object getPropertyValueByKey(String key, String filePath) {
        Object value = null;
        try {
            InputStream in = new FileInputStream(filePath);
            Properties properties = new Properties();
            properties.load(in);
            value = properties.getProperty(key);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }




    /**
     * 获取
     *
     * @Title: getUrlCallBackInfo
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param fileurl
     * @return
     */
    public static String getUrlCallBackInfo(String fileurl, String charset) {
        StringBuffer sb = new StringBuffer();
        try {
            URL url = new URL(fileurl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            InputStream bis = url.openStream();
            StringBuffer s = new StringBuffer();
            if (charset == null || "".equals(charset)) {
                charset = "utf-8";
            }
            String rLine = null;
            BufferedReader bReader = new BufferedReader(new InputStreamReader(
                    bis, charset));
            PrintWriter pw = null;

            FileOutputStream fo = new FileOutputStream("../index.html");
            OutputStreamWriter writer = new OutputStreamWriter(fo, "utf-8");
            pw = new PrintWriter(writer);
            while ((rLine = bReader.readLine()) != null) {
                String tmp_rLine = rLine;
                int str_len = tmp_rLine.length();
                if (str_len > 0) {
                    s.append(tmp_rLine);
                    pw.println(tmp_rLine);
                    pw.flush();
                }
                tmp_rLine = null;
            }
            bis.close();
            pw.close();
            return s.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    /**
     * 上传文件
     *
     * @Title: uploadFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param file
     * @param fileName
     * @return void 返回类型
     */
    public static void uploadFile(File file, String fileName) {
        try {
            if (!file.exists()) { // 如果文件的路径不存在就创建路径
                file.getParentFile().mkdirs();
            }
            InputStream bis = new FileInputStream(file);
            uploadFile(bis, fileName);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传文件
     *
     * @Title: uploadFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param in
     * @param fileName
     * @return void 返回类型
     */
    public static void uploadFile(InputStream in, String fileName) {
        if (in == null || fileName == null || fileName.equals("")) {
            return;
        }
        try {
            File uploadFile = new File(fileName);
            if (!uploadFile.exists()) { // 如果文件的路径不存在就创建路径
                uploadFile.getParentFile().mkdirs();
            }
            OutputStream out = new FileOutputStream(fileName);
            byte[] buffer = new byte[2048];
            int temp = 0;
            while ((temp = in.read(buffer)) != -1) {
                out.write(buffer, 0, temp);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * 获得指定长度的随机数
     */
    public static String getRandomString(int length) {
        String str = "abcdef0123456789";
        Random random = new Random();
        StringBuffer sf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(16);// 0~16
            sf.append(str.charAt(number));
        }
        return sf.toString();
    }

    /**
     * 将字符串输出到文件中
     * @Title: wireStringToFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @return void    返回类型
     */
    public static String  wireStringToFile(String content,String filePath,String fileName){
        if(filePath==null || filePath.equals("")){
            return null;
        }
        BufferedWriter out=null;
        try {
            File uploadFile = new File(filePath);
            if (!uploadFile.exists()) { // 如果文件的路径不存在就创建路径
                uploadFile.mkdirs();
            }
            String file=filePath+ File.separator +fileName;
            out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"utf-8"));
            out.write(content);
            out.flush();
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                if(out!=null){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 文件上传方法
     *
     * @author peng
     * @param file
     *            上传的文件
     * @param uploadPath
     *            上传的文件路径
     * @param fileName
     *            双传的文件名称
     */
    public static void uploadFile(File file, String uploadPath, String fileName) {
        try {
            File uploadFile = new File(uploadPath);
            if (!uploadFile.exists()) { // 如果文件的路径不存在就创建路径
                uploadFile.mkdirs();
            }
            InputStream bis = new FileInputStream(file);
            uploadFile(bis, uploadPath + File.separator + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param fileName
     *            文件的名称
     * @return 文件的后缀名(即格式名称)
     */
    public static String getSuffix(String fileName) {
        if (fileName == null || "".equals(fileName)) {
            return "";
        }
        if (fileName.contains(".")) {
            String[] temp = fileName.split("\\.");
            return temp[temp.length - 1];
        }
        return null;
    }
/*
    /**
     * 将字符串保存到指定的文件中
     *
     * @Title: createFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param fileContent
     * @param fileName
     * @return void 返回类型
     */
    /*
    public static void createFile(String fileContent, String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            PrintWriter out = new PrintWriter(file, "UTF-8");
            out.write(fileContent);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
*/
    // 清空文件夹以及文件夹里面的所有文件
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); // 删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete(); // 删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除指定文件夹下所有文件
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);// 先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);// 再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }

    // 压缩文件或文件夹
    static byte[] buffer = new byte[204800];

    public static void zip(File[] files, String baseFolder, ZipOutputStream zos)
            throws Exception {
        FileInputStream fis = null;
        ZipEntry entry = null;
        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                zip(file.listFiles(), file.getName() + File.separator, zos);
                continue;
            }
            entry = new ZipEntry(baseFolder + file.getName());
            zos.putNextEntry(entry);
            fis = new FileInputStream(file);
            while ((count = fis.read(buffer, 0, buffer.length)) != -1)
                zos.write(buffer, 0, count);
        }
    }





    /**
     * 获取文件的名称
     *
     * @Title: getFileName
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @return
     * @return String 返回类型
     */
    public static String getFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("hhmmssSSS");
        return sdf.format(new Date()) + getRandomString(4);
    }





    /**
     * 列出某个目录下的所有文件
     *
     * @param sourceFile
     */
    public static void listAllFiles(File sourceFile) {
        try {
            if (sourceFile.isDirectory()) {
                for (File file : sourceFile.listFiles()) {
                    listAllFiles(file);
                }
            } else {
                list.add(sourceFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取文件大小
     *
     * @Title: getFileSizes
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param f
     * @return
     * @throws Exception
     * @return long 返回类型
     */
    public static long getFileSizes(File f) throws Exception {// 取得文件大小
        long s = 0;
        if (f.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(f);
            s = fis.available();
        } else {
            f.createNewFile();
            System.out.println("文件不存在");
        }
        return s;
    }

    /**
     * 视频格式转换
     *
     * @Title: getFileSizes
     * @Description: TODO(视频格式转换)
     * @param
     * @return
     * @throws Exception
     * @return long 返回类型
     */
    public static int checkContentType(String type) {
        // ffmpeg能解析的格式：（asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等）
        if (type.equals("avi")) {
            return 0;
        } else if (type.equals("mpg")) {
            return 0;
        } else if (type.equals("wmv")) {
            return 0;
        } else if (type.equals("3gp")) {
            return 0;
        } else if (type.equals("mov")) {
            return 0;
        } else if (type.equals("asf")) {
            return 0;
        } else if (type.equals("mp4")) {
            return 0;
        } else if (type.equals("asx")) {
            return 0;
        } else if (type.equals("flv")) {
            return 2;
        }
        // 对ffmpeg无法解析的文件格式(wmv9，rm，rmvb等),
        // 可以先用别的工具（mencoder）转换为avi(ffmpeg能解析的)格式.
        else if (type.equals("wmv9")) {
            return 1;
        } else if (type.equals("rm")) {
            return 1;
        } else if (type.equals("rmvb")) {
            return 1;
        }
        return 9;
    }

    // ffmpeg能解析的格式：（asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等）
    public static boolean processFLV(String oldfilepath, String outPath,
                                     String ffmpegPath) {
        List<String> commend = new ArrayList<String>();
        commend.add(ffmpegPath);
        commend.add("-i");
        commend.add(oldfilepath);
        commend.add("-ab");
        commend.add("128");
        commend.add("-acodec");
        commend.add("libmp3lame");
        commend.add("-ac");
        commend.add("1");
        commend.add("-ar");
        commend.add("22050");
        commend.add("-qscale");
        commend.add("6");
        commend.add("-r");
        commend.add("29.97");
        commend.add("-b");
        commend.add("512");
        commend.add("-y");
        commend.add(outPath);

        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(commend);
            Process p = builder.start();
            ioWrite(p.getInputStream(), p.getErrorStream());
            p.waitFor();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 将rmvb转换成avi
    public static String processAVI(String path, String outPath,
                                    String mencoderPath) {
        List<String> commend = new ArrayList<String>();
        /*
         * commend.add("f:\\mencoder"); commend.add(PATH); commend.add("-oac");
         * commend.add("lavc"); commend.add("-lavcopts");
         * commend.add("acodec=mp3:abitrate=64"); commend.add("-ovc");
         * commend.add("xvid"); commend.add("-xvidencopts");
         * commend.add("bitrate=600"); commend.add("-of"); commend.add("avi");
         * commend.add("-o"); commend.add("f:\\rmvb.avi");
         */
        commend.add(mencoderPath);
        commend.add(path);
        commend.add("-oac");
        commend.add("mp3lame");
        commend.add("-lameopts");
        commend.add("preset=64");
        commend.add("-ovc");
        commend.add("xvid");
        commend.add("-xvidencopts");
        commend.add("bitrate=600");
        commend.add("-of");
        commend.add("avi");
        commend.add("-o");
        commend.add(outPath);
        // 命令类型：mencoder 1.rmvb -oac mp3lame -lameopts preset=64 -ovc xvid
        // -xvidencopts bitrate=600 -of avi -o rmvb.avi
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(commend);
            Process p = builder.start();
            ioWrite(p.getInputStream(), p.getErrorStream());
            // 等Mencoder进程转换结束，再调用ffmepg进程
            p.waitFor();
            return outPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void ioWrite(final InputStream is1, final InputStream is2) {
        new Thread() {
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        is1));
                try {
                    String lineB = null;
                    while ((lineB = br.readLine()) != null) {
                        if (lineB != null)
                            System.out.println(lineB);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("报错了！123");
                }
            }
        }.start();

        new Thread() {
            public void run() {
                BufferedReader br2 = new BufferedReader(new InputStreamReader(
                        is2));
                try {
                    String lineC = null;
                    while ((lineC = br2.readLine()) != null) {
                        if (lineC != null)
                            System.out.println(lineC);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("报错了！456");
                }
            }
        }.start();
    }

    // 截图
    public static boolean screenshot(String Path, String outPicPath,
                                     String ffmpegPath) throws IOException {
        List<String> commend = new ArrayList<String>();
        commend.add(ffmpegPath);
        commend.add("-i");
        commend.add(Path);
        commend.add("-y");
        commend.add("-f");
        commend.add("image2");
        commend.add("-ss");
        commend.add("8");
        commend.add("-t");
        commend.add("0.001");
        commend.add("-s");
        commend.add("420x320");
        commend.add(outPicPath);
        ProcessBuilder builder = new ProcessBuilder(commend);
        builder.command(commend);
        builder.start();
        return true;
    }

    /**
     * 获取文件的大小
     *
     * @Title: floatFormart
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param f
     * @return
     * @throws NumberFormatException
     * @throws Exception
     * @return String 返回类型
     */
    public static String floatFormart(File f) {
        String str = "0K";
        try {
            NumberFormat numFormat = NumberFormat.getNumberInstance();
            numFormat.setMaximumFractionDigits(2);
            str = numFormat.format(Float.parseFloat(String.valueOf(FileUploadUtil
                    .getFileSizes(f))) / 1024 / 1024) + "M";
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }


    /**
     * 获取上传时文件夹名称
     * @Title: getFolderName
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @return
     * @return String    返回类型
     * @date 2015-6-17 上午11:26:15
     */
    public static String getFolderName(){
        String folderName = formatter.format(currentTime).toString().substring(0,8);
        return folderName;
    }

    /**
     * 获取转码后文件名称（实体文件存储名称）
     * @Title: getNewFileName
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @return
     * @return String    返回类型
     */
    public static String getNewFileName(){
        String newFileName = formatter.format(currentTime).toString().substring(8,formatter.format(currentTime).toString().length());
        return newFileName;
    }

    /**
     * 创建文件夹
     * @Title: createFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param resourcePath    对应资源所保存的文件夹结构
     * @param data  当前时间作为文件夹
     * @return
     * @return File    返回类型
     */
    public static File createFile(String resourcePath, String data){
        String path = FileUploadUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int index=path.indexOf("WEB-INF");
        if(index>0){
            path=path.substring(0, index);
        }
        System.out.println("path"+path);
        File file=new File(path);
        String savepath = file.getAbsolutePath()+File.separator+resourcePath+File.separator+data;

        System.out.println("savepath"+savepath);
        File savedir=new File(savepath);
        if(!savedir.exists())
            savedir.mkdirs();
        return savedir;
    }


    /**
     * 获取项目根目录
     * @Title: getProjectPath
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @return
     * @return String    返回类型
     */
    public static String getProjectPath(){
        String path = FileUploadUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int index=path.indexOf("WEB-INF");
        if(index>0){
            path=path.substring(0, index);
        }
        File file=new File(path);
        String savepath = file.getAbsolutePath()+File.separator;


        return savepath;
    }

    /**
     * 上传临时文件...
     * @Title: uploadTemp
     * @Description:
     * @param file
     * @return
     * @throws Exception
     * @return String    返回类型
     * @date 2015-9-1 上午9:37:06
     */
//    public static String uploadTemp(MultipartFile file,String userId) throws Exception{
//        //文件全名
//        String fileName = file.getOriginalFilename();
//        String suffix = fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
//        String coverNewName = userId+"."+suffix;
//        //文件保存路径 (使用随机名称: 项目名/WEB-INF/temp_file/随机数.jpg)
//        File saveFile =new File(createFile("/resource/temp_file/"+userId,"").getPath()+"/"+coverNewName);
//        //获取上传文件流
//        InputStream in = file.getInputStream();
//
//        //上传文件(复制)
//        FileUtils.copyInputStreamToFile(in, saveFile);
//
//        return coverNewName;
//
//    }


    /**
     * 上传文件(Book)
     * @Title: uplodFile
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param file
     * @return
     * @return String    返回类型
     * @date 2015-7-1 上午11:43:16
     */
//    public static String uplodFile(MultipartFile file){
//
//        //文件输入流
//        InputStream in = null;
//        //创建一个文件保存路径
//        File filePath = null;
//        //原文件全名
//        String fileName = null;
//        //文件名
//        String fName = null;
//        //保存文件对象
//        File saveFile= null;
//        try {
//            //文件全名
//            fileName = file.getOriginalFilename();
//            //文件名
//            fName = fileName.substring(0,fileName.lastIndexOf("."));
//
//            //文件后缀名,没有 '.'
//            String suffix = fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
//
//            //图片资源保存路径
//            if(listImages.contains(suffix)){
//                filePath = createFile("pic",getFolderName());
//            }
//            if(filePath == null){
//                return "errorType";
//            }
//            //创建要保存的文件的对象
//            saveFile= new File(filePath.getPath()+File.separator+getFileName()+"."+suffix);
//            //获取上传文件流
//            in = file.getInputStream();
//
//            //上传文件(复制)
//            FileUtils.copyInputStreamToFile(in, saveFile);
//
//
//        } catch (Exception  e1) {
//            System.out.println("上传失败!");
//            e1.printStackTrace();
//            try {
//                in.close();
//            } catch (IOException e2) {
//                e2.printStackTrace();
//            }
//            return "fail";
//
//        }
//        String relaPath = saveFile.getPath().replaceAll("\\\\", "/");
//
//        return relaPath;
//    }

    /**
     * 根据绝对磁盘路径获取相对路径
     * @Title: getRelativePath
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param realPath
     * @return
     * @return String    返回类型
     * @date 2015-7-16 下午5:14:45
     */
    public static String getRelativePath(String realPath){
        String relativePath = realPath;
        if(!realPath.equals("fail") && !realPath.equals("errorType")){
            relativePath= "/"+realPath.substring(realPath.indexOf("image"));
        }
        return relativePath;
    }


    /**
     * 上传头像
     * @Title: uploadHeadPortrait
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param file
     * @return
     * @return String    返回类型
     */
//    public static String uploadHeadPortrait(MultipartFile file){
//
//        //文件输入流
//        InputStream in = null;
//        //创建一个文件保存路径
//        File filePath = null;
//        //原文件全名
//        String fileName = null;
//        //文件名
//        String fName = null;
//        //保存文件对象
//        File saveFile= null;
//        try {
//            //文件全名
//            fileName = file.getOriginalFilename();
//            //文件名
//            fName = fileName.substring(0,fileName.lastIndexOf("."));
//
//            //文件后缀名,没有 '.'
//            String suffix = fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
//            //图片资源保存路径
//            filePath = FileUtil.createFile("image",getFolderName());
//            if(filePath == null){
//                return "errorType";
//            }
//            long filerandomname=System.currentTimeMillis();
//            //创建要保存的文件的对象
//            saveFile= new File(filePath.getPath()+File.separator+filerandomname+"."+suffix);
//            //获取上传文件流
//            in = file.getInputStream();
//            //上传文件(复制)
//            FileUtils.copyInputStreamToFile(in, saveFile);
//
//        } catch (Exception  e1) {
//            System.out.println("上传失败!");
//            e1.printStackTrace();
//            try {
//                in.close();
//            } catch (IOException e2) {
//                e2.printStackTrace();
//            }
//            return "fail";
//
//        }
//        String relaPath = saveFile.getPath().replaceAll("\\\\", "/");
//
//        return relaPath;
//    }






    /**
     * 获取文件前6位16进制，用于过滤
     * @Title: bytesToHexString
     * @Description: TODO(这里用一句话描述这个方法的作用)
     * @param file
     * @return
     * @return String    返回类型
     * @date 2015-9-21 上午11:20:40
     */
    public static String bytesToHexString(File file) {
        StringBuilder stringBuilder = new StringBuilder();
        byte[] bt;
        try {
            InputStream is = new FileInputStream(file);
            bt = new byte[3];
            is.read(bt);
            if (bt == null || bt.length <= 0) {
                return null;
            }
            for (int i = 0; i < bt.length; i++) {
                int v = bt[i] & 0xFF;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }
                stringBuilder.append(hv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public static void uploadImgLW(String urls,String filePath ,String imgName) throws Exception {
//        URL url = new URL("http://sx010.img.diexun.com/business/2018_A/0227/2711/201802271152598796_l_1320.jpg");
//        File imageFile = new File("/u/py/" + filePath);
//        if (!imageFile.exists())
//            imageFile.mkdir();
        download(urls,imgName+".jpg",filePath);
//        //打开链接
//        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//        //设置请求方式为"GET"
//        conn.setRequestMethod("GET");
//        //超时响应时间为5秒
//        conn.setConnectTimeout(5 * 1000);
//        //通过输入流获取图片数据
//        InputStream inStream = conn.getInputStream();
//        //得到图片的二进制数据，以二进制封装得到数据，具有通用性
//        byte[] data = readInputStream(inStream);
//        //new一个文件对象用来保存图片，默认保存当前工程根目录
//        //File imageFile = createFile("/u/py/"+filePath,imgName+".jpg");//new File(imgName);
//        File imageFile = new File("/u/py/" + filePath);
//        if (!imageFile.exists())
//            imageFile.mkdir();
//        String newFilePath = "/u/py"+"/" + imgName+".jpg";
//        imageFile = new File(newFilePath);
//        imageFile.createNewFile();
////        imageFile = new File("pic_"+imgName+".jpg");
//        //创建输出流
//        FileOutputStream outStream = new FileOutputStream(imageFile);
//        //写入数据
//        outStream.write(data);
//        //关闭输出流
//        outStream.close();
    }
    public static byte[] readInputStream(InputStream inStream) throws Exception{
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        //创建一个Buffer字符串
        byte[] buffer = new byte[1024];
        //每次读取的字符串长度，如果为-1，代表全部读取完毕
        int len = 0;
        //使用一个输入流从buffer里把数据读取出来
        while( (len=inStream.read(buffer)) != -1 ){
            //用输出流往buffer里写入数据，中间参数代表从哪个位置开始读，len代表读取的长度
            outStream.write(buffer, 0, len);
        }
        //关闭输入流
        inStream.close();
        //把outStream里的数据写入内存
        return outStream.toByteArray();
    }

    public static void download(String urlString, String filename,String savePath) throws Exception {
        // 构造URL
        URL url = new URL(urlString);
        // 打开连接
        URLConnection con = url.openConnection();
        //设置请求超时为5s
        con.setConnectTimeout(5*1000);
        // 输入流
        InputStream is = con.getInputStream();

        // 1K的数据缓冲
        byte[] bs = new byte[1024];
        // 读取到的数据长度
        int len;
        // 输出的文件流
        File sf=new File(getProjectPath()+savePath);
        if(!sf.exists()){
            sf.mkdirs();
        }
        OutputStream os = new FileOutputStream(sf.getPath()+"\\"+filename);
        // 开始读取
        while ((len = is.read(bs)) != -1) {
            os.write(bs, 0, len);
        }
        // 完毕，关闭所有链接
        os.close();
        is.close();
    }

}