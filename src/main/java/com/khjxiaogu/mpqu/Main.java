package com.khjxiaogu.mpqu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.FileMessage;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.BotConfiguration.MiraiProtocol;
import net.mamoe.mirai.utils.ExternalResource;
import net.mamoe.mirai.utils.RemoteFile;
import net.mamoe.mirai.utils.RemoteFile.ProgressionCallback;

public class Main {
	static List<String> ignores=new ArrayList<>();
    public static void main(String[] args) throws IOException {
        
        File ziptemp=new File("compressed.zip");
        Properties pf=new Properties();

        pf.load(new InputStreamReader(new FileInputStream(new File("upload.properties")), StandardCharsets.UTF_8));
        
        ignores.addAll(Arrays.asList(pf.getProperty("ignores","").split(";")));
        ignores.removeIf(s->s.length()==0);
        String sourceFile = pf.getProperty("dir");
        
        FileOutputStream fos = new FileOutputStream(ziptemp);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);
        if(!fileToZip.exists()) {
        	System.out.println("file to compress not found");
        	return;
        }
        zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
        BotConfiguration bc=BotConfiguration.getDefault();
        bc.setAutoReconnectOnForceOffline(false);
        
        bc.fileBasedDeviceInfo("qqdevice.json");
        bc.setProtocol(MiraiProtocol.ANDROID_PHONE);
        
        Bot bot = BotFactory.INSTANCE.newBot(Long.parseLong(pf.getProperty("qq")),pf.getProperty("password"),bc);
		
        bot.login();
		Group g=bot.getGroup(Long.parseLong(pf.getProperty("group")));
		
		FileMessage fm=g.getFilesRoot().resolve("/"+pf.getProperty("filename")+".zip").upload(ziptemp,new ProgressConsole());
		g.sendMessage(fm);
		bot.close();
		
		ziptemp.delete();
		System.exit(0);
    }
    public static class ProgressConsole implements ProgressionCallback{

		public void onBegin(RemoteFile file, ExternalResource resource) {
			System.out.println("==============");
			System.out.print("Uploading...");
			System.out.println("==============");
			System.out.println("Do Not Shut Down!");
		}

		public void onFailure(RemoteFile file, ExternalResource resource, Throwable exception) {
			System.out.println("[error]"+exception.getClass().getName()+":"+exception.getMessage());
			exception.printStackTrace();
			System.out.println("==============");
			System.out.print("Upload Failure");
			System.out.println("==============");
		}

		public void onProgression(RemoteFile file, ExternalResource resource, long downloadedSize) {
			System.out.println("Uploading..."+downloadedSize+"/"+resource.getSize());
		}

		public void onSuccess(RemoteFile file, ExternalResource resource) {
			System.out.println("==============");
			System.out.print("Upload Succeed");
			System.out.println("==============");
		}
    	
    }
    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        for(String s:ignores) {
        	if(fileName.startsWith(s))
        		return;
        }
    	if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

}
