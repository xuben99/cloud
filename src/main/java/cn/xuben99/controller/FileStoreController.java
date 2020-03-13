package cn.xuben99.controller;
import cn.xuben99.entity.FileFolder;
import cn.xuben99.entity.FileStore;
import cn.xuben99.entity.MyFile;
import cn.xuben99.utils.FtpUtil;
import cn.xuben99.utils.LogUtils;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName: FileStoreController
 * @Description: 文件仓库控制器
 * @author: xw
 * @date 2020/2/6 16:04
 * @Version: 1.0
 **/
@Controller
public class FileStoreController extends BaseController {
    private Logger logger = LogUtils.getInstance(FileStoreController.class);

    /**
     * @Description 网盘的文件上传
     * @Author xw
     * @Date 23:10 2020/2/10
     * @Param [files]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    @PostMapping("/uploadFile")
    @ResponseBody
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile files) {
        Map<String, Object> map = new HashMap<>();
        FileStore store = fileStoreService.getFileStoreByUserId(loginUser.getUserId());
        Integer folderId = Integer.valueOf(request.getHeader("id"));
        String name = files.getOriginalFilename().replaceAll(" ","");
        //获取当前目录下的所有文件，用来判断是否已经存在
        List<MyFile> myFiles = null;
        if (folderId == 0){
            //当前目录为根目录
            myFiles = myFileService.getRootFilesByFileStoreId(loginUser.getFileStoreId());
        }else {
            //当前目录为其他目录
            myFiles = myFileService.getFilesByParentFolderId(folderId);
        }
        for (int i = 0; i < myFiles.size(); i++) {
            if ((myFiles.get(i).getMyFileName()+myFiles.get(i).getPostfix()).equals(name)){
                logger.error("当前文件已存在!上传失败...");
                map.put("code", 501);
                return map;
            }
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = format.format(new Date());
        String path = loginUser.getUserId()+"/"+dateStr +"/"+folderId;
        if (!checkTarget(name)){
            logger.error("上传失败!文件名不符合规范...");
            map.put("code", 502);
            return map;
        }
        Integer sizeInt = Math.toIntExact(files.getSize() / 1024);
        //是否仓库放不下该文件
        if(store.getCurrentSize()+sizeInt > store.getMaxSize()){
            logger.error("上传失败!仓库已满。");
            map.put("code", 503);
            return map;
        }
        //处理文件大小
        String size = String.valueOf(files.getSize()/1024.0);
        int indexDot = size.lastIndexOf(".");
        size = size.substring(0,indexDot);
        int index = name.lastIndexOf(".");
        String tempName = name.substring(index);
        name = name.substring(0,index);
        //获得文件类型
        int type = getType(tempName.toLowerCase());
        String postfix = tempName.toLowerCase();
        try {
            //提交到FTP服务器
            boolean b = FtpUtil.uploadFile("/"+path, name + postfix, files.getInputStream());
            if (b){
                //上传成功
                logger.info("文件上传成功!"+files.getOriginalFilename());
                //向数据库文件表写入数据
                myFileService.addFileByFileStoreId(
                        MyFile.builder()
                              .myFileName(name).fileStoreId(loginUser.getFileStoreId()).myFilePath(path)
                              .downloadTime(0).uploadTime(new Date()).parentFolderId(folderId).
                              size(Integer.valueOf(size)).type(type).postfix(postfix).build());
                //更新仓库表的当前大小
                fileStoreService.addSize(store.getFileStoreId(),Integer.valueOf(size));
                try {
                    Thread.sleep(5000);
                    map.put("code", 200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                logger.error("文件上传失败!"+files.getOriginalFilename());
                map.put("code", 504);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * @Description 网盘的文件下载 
     * @Author xw
     * @Date 23:13 2020/2/10
     * @Param [fId]
     * @return void
     **/
    @GetMapping("/downloadFile")
    public void downloadFile(@RequestParam Integer fId){
        //获取文件信息
        MyFile myFile = myFileService.getFileByFileId(fId);
        String remotePath = myFile.getMyFilePath();
        String fileName = myFile.getMyFileName()+myFile.getPostfix();
        try {
            File temp = new File("temp");
            if (!temp.exists()) {
                temp.mkdirs();
            }
            String tempStr = "temp/"+UUID.randomUUID().toString();
            File file = new File(tempStr);
            //去FTP上拉取
            //boolean flag = FtpUtil.downloadFile("/" + remotePath, fileName,tempStr);
            boolean flag = FtpUtil.downloadFile(remotePath, fileName,tempStr);
            if (flag) {
                myFileService.updateFile(
                        MyFile.builder().myFileId(myFile.getMyFileId()).downloadTime(myFile.getDownloadTime()+1).build());
                //获得服务器本地的文件，并使用IO流写出到浏览器
                InputStream is = new BufferedInputStream(new FileInputStream(file));
                OutputStream os = new BufferedOutputStream(response.getOutputStream());
                response.setCharacterEncoding("utf-8");
                // 设置返回类型
                response.setContentType("multipart/form-data");
                // 文件名转码一下，不然会出现中文乱码
                response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(fileName, "UTF-8"));
                // 设置返回的文件的大小
                response.setContentLength((int) file.length());
                byte[] b = new byte[1024];
                int len = 0;
                while (-1 != (len = is.read(b))) {
                    os.write(b, 0, len);
                }
                os.flush();
                os.close();
                is.close();
                logger.info("文件下载成功!"+myFile);
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description 删除文件
     * @Author xw
     * @Date 23:14 2020/2/10
     * @Param [fId, folder]
     * @return java.lang.String
     **/
    @GetMapping("/deleteFile")
    public String deleteFile(@RequestParam Integer fId,Integer folder){
        //获得文件信息
        MyFile myFile = myFileService.getFileByFileId(fId);
        String remotePath = myFile.getMyFilePath();
        String fileName = myFile.getMyFileName()+myFile.getPostfix();
        //从FTP文件服务器上删除文件
        boolean b = FtpUtil.deleteFile(remotePath, fileName);
        if (b){
            //删除成功,返回空间
            fileStoreService.subSize(myFile.getFileStoreId(),Integer.valueOf(myFile.getSize()));
            //删除文件表对应的数据
            myFileService.deleteByFileId(fId);
        }
        logger.info("删除文件成功!"+myFile);
        return "redirect:/files?fId="+folder;
    }
    
    /**
     * @Description 删除文件夹并清空文件
     * @Author xw
     * @Date 15:22 2020/2/12
     * @Param [fId]
     * @return java.lang.String
     **/
    @GetMapping("/deleteFolder")
    public String deleteFolder(@RequestParam Integer fId){
        FileFolder folder = fileFolderService.getFileFolderByFileFolderId(fId);
        //强制删除
        deleteFolderF(folder);
        return folder.getParentFolderId() == 0?"redirect:/files":"redirect:/files?fId="+folder.getParentFolderId();
    } 

    /**
     * @Description 迭代删除文件夹里面的所有文件和子文件夹
     * @Author xw
     * @Date 9:17 2020/2/29
     * @Param [folder]
     * @return void
     **/
    public void deleteFolderF(FileFolder folder){
        //获得当前文件夹下的所有子文件夹
        List<FileFolder> folders = fileFolderService.getFileFolderByParentFolderId(folder.getFileFolderId());
        //删除当前文件夹的所有的文件
        List<MyFile> files = myFileService.getFilesByParentFolderId(folder.getFileFolderId());
        if (files.size()!=0){
            for (int i = 0; i < files.size(); i++) {
                Integer fileId = files.get(i).getMyFileId();
                boolean b = FtpUtil.deleteFile(files.get(i).getMyFilePath(), files.get(i).getMyFileName() + files.get(i).getPostfix());
                if (b){


                    myFileService.deleteByFileId(fileId);
                    fileStoreService.subSize(folder.getFileStoreId(),Integer.valueOf(files.get(i).getSize()));
                }
            }

        }
        if (folders.size()!=0){
            for (int i = 0; i < folders.size(); i++) {
                deleteFolderF(folders.get(i));
            }
        }

        fileFolderService.deleteFileFolderById(folder.getFileFolderId());
    }

    /**
     * @Description 添加文件夹
     * @Author xw
     * @Date 23:16 2020/2/10
     * @Param [folder, map]
     * @return java.lang.String
     **/
    @PostMapping("/addFolder")
    public String addFolder(FileFolder folder,Map<String, Object> map) {
        //设置文件夹信息
        folder.setFileStoreId(loginUser.getFileStoreId());
        folder.setTime(new Date());
        //获得当前目录下的所有文件夹,检查当前文件夹是否已经存在
        List<FileFolder> fileFolders = null;
        if (folder.getParentFolderId() == 0){
            //向用户根目录添加文件夹
            fileFolders = fileFolderService.getRootFoldersByFileStoreId(loginUser.getFileStoreId());
        }else{
            //向用户的其他目录添加文件夹
            fileFolders = fileFolderService.getFileFolderByParentFolderId(folder.getParentFolderId());
        }
        for (int i = 0; i < fileFolders.size(); i++) {
            FileFolder fileFolder = fileFolders.get(i);
            if (fileFolder.getFileFolderName().equals(folder.getFileFolderName())){
                logger.info("添加文件夹失败!文件夹已存在...");
                return "redirect:/files?error=1&fId="+folder.getParentFolderId();
            }
        }
        //向数据库写入数据
        Integer integer = fileFolderService.addFileFolder(folder);
        logger.info("添加文件夹成功!"+folder);
        return "redirect:/files?fId="+folder.getParentFolderId();
    }
    
    /**
     * @Description 重命名文件夹
     * @Author xw
     * @Date 23:18 2020/2/10
     * @Param [folder, map]
     * @return java.lang.String
     **/
    @PostMapping("/updateFolder")
    public String updateFolder(FileFolder folder,Map<String, Object> map) {
        //获得文件夹的数据库信息
        FileFolder fileFolder = fileFolderService.getFileFolderByFileFolderId(folder.getFileFolderId());
        fileFolder.setFileFolderName(folder.getFileFolderName());
        //获得当前目录下的所有文件夹,用于检查文件夹是否已经存在
        List<FileFolder> fileFolders = fileFolderService.getFileFolderByParentFolderId(fileFolder.getParentFolderId());
        for (int i = 0; i < fileFolders.size(); i++) {
            FileFolder folder1 = fileFolders.get(i);
            if (folder1.getFileFolderName().equals(folder.getFileFolderName()) && folder1.getFileFolderId() != folder.getFileFolderId()){
                logger.info("重命名文件夹失败!文件夹已存在...");
                return "redirect:/files?error=2&fId="+fileFolder.getParentFolderId();
            }
        }
        //向数据库写入数据
        Integer integer = fileFolderService.updateFileFolderById(fileFolder);
        logger.info("重命名文件夹成功!"+folder);
        return "redirect:/files?fId="+fileFolder.getParentFolderId();
    }

    /**
     * @Description 重命名文件
     * @Author xw
     * @Date 12:47 2020/2/12
     * @Param [file, map]
     * @return java.lang.String
     **/
    @PostMapping("/updateFileName")
    public String updateFileName(MyFile file,Map<String, Object> map) {
        MyFile myFile = myFileService.getFileByFileId(file.getMyFileId());
        if (myFile != null){
            String oldName = myFile.getMyFileName();
            String newName = file.getMyFileName();
            if (!oldName.equals(newName)){
                boolean b = FtpUtil.reNameFile(myFile.getMyFilePath() + "/" + oldName+myFile.getPostfix(), myFile.getMyFilePath() + "/" + newName+myFile.getPostfix());
                if (b){
                    Integer integer = myFileService.updateFile(
                            MyFile.builder().myFileId(myFile.getMyFileId()).myFileName(newName).build());
                    if (integer == 1){
                        logger.info("修改文件名成功!原文件名:"+oldName+"  新文件名:"+newName);
                    }else{
                        logger.error("修改文件名失败!原文件名:"+oldName+"  新文件名:"+newName);
                    }
                }
            }
        }
        return "redirect:/files?fId="+myFile.getParentFolderId();
    }



    /**
     * @Description 根据文件的后缀名获得对应的类型
     * @Author xw
     * @Date 23:20 2020/2/10
     * @Param [type]
     * @return int 1:文本类型   2:图像类型  3:视频类型  4:音乐类型  5:其他类型
     **/
    public int getType(String type){
        if (".txt".equals(type)||".doc".equals(type)||".docx".equals(type)
                ||".wps".equals(type)||".word".equals(type)||".html".equals(type)||".pdf".equals(type)){
            return  1;
        }else if (".bmp".equals(type)||".gif".equals(type)||".jpg".equals(type)
                ||".pic".equals(type)||".png".equals(type)||".jepg".equals(type)||".webp".equals(type)
                ||".svg".equals(type)){
            return 2;
        } else if (".avi".equals(type)||".mov".equals(type)||".qt".equals(type)
                ||".asf".equals(type)||".rm".equals(type)||".navi".equals(type)||".wav".equals(type)
                ||".mp4".equals(type)){
            return 3;
        } else if (".mp3".equals(type)||".wma".equals(type)){
            return 4;
        } else {
            return 5;
        }
    }
    
    /**
     * @Description 正则验证文件名是否合法 [汉字,字符,数字,下划线,英文句号,横线]
     * @Author xw
     * @Date 23:22 2020/2/10
     * @Param [target]
     * @return boolean
     **/
    public boolean checkTarget(String target) {
        final String format = "[^\\u4E00-\\u9FA5\\uF900-\\uFA2D\\w-_.]";
        Pattern pattern = Pattern.compile(format);
        Matcher matcher = pattern.matcher(target);
        return !matcher.find();
    }

    /**
     * @Description 每天0、1、2 点清除缓存
     * @Author xw
     * @Date 21:56 2020/2/26
     * @Param []
     * @return void
     **/
    @Scheduled(cron = "0 0 1,2,3 * * ?")
    public void flushCache(){
        File file = new File("temp");
        if (file == null || !file.exists()){
            return;
        }
        File[] files = file.listFiles();
        if (files == null || files.length == 0){
            return;
        }else {
            for (File file1 : files) {
                boolean delete = file1.delete();
            }
            file.delete();
        }
    }
    
}
