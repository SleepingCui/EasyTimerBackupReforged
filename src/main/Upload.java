package main;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;

import static main.config_read.get_config;

public class Upload {
    static Logger logger = Logger.getLogger(Upload.class.getName());
    private static void UploadBk(String ip,int port,File ZipPath) throws IOException {
        Socket socket = new Socket(ip,port);
        OutputStream out = socket.getOutputStream();
        FileInputStream fis = new FileInputStream(ZipPath);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) != -1){
            out.write(buffer,0,length);
        }
        socket.shutdownOutput();
        InputStream in = socket.getInputStream();
        byte[] bufMsg = new byte[1024];
        int num = in.read(bufMsg);
        String Msg = new String(bufMsg,0,num);
        logger.info(Msg);
        fis.close();
        socket.close();

    }
    public static void UploadBackup() throws IOException {
        String Enabled = get_config("uploadenabled");
        if (Enabled.equals("y")){
            String ip = get_config("uploadip");
            File ZipPath = new File(get_config("backup_dir"));
            int port = Integer.valueOf(get_config("uploadport")).intValue();
            UploadBk(ip,port,ZipPath);
        }
        else {
            logger.warning("Upload is not enabled");
        }
    }

}
