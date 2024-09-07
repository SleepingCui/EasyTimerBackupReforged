package main;

import java.io.*;
import java.net.*;


import static main.config_read.get_config;

public class Upload {
    private static void UploadBk(String ip, int port, File zipPath, boolean DelBackup) throws IOException {
        Socket socket = new Socket(ip, port);

        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(zipPath)) {

            // 发送文件名
            dos.writeUTF(zipPath.getName());

            long totalSize = zipPath.length(); // 获取文件总大小
            long uploadedSize = 0; // 已上传的字节数
            byte[] buffer = new byte[1024];
            int length;

            // 发送文件数据
            while ((length = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, length);
                uploadedSize += length; // 更新已上传的字节数

                // 计算并显示进度
                int progressPercentage = (int) ((uploadedSize * 100) / totalSize);
                String progressMessage = String.format("%d%% %dMB/%dMB",
                        progressPercentage,
                        uploadedSize / (1024 * 1024),
                        totalSize / (1024 * 1024));
                System.out.print("\rINFO: " + progressMessage);
            }

            dos.flush(); // 确保所有数据都被发送
            socket.shutdownOutput(); // 关闭输出流以指示文件发送完毕

            // 接收服务器返回的消息
            InputStream in = socket.getInputStream();
            byte[] bufMsg = new byte[1024];
            int num = in.read(bufMsg);
            String msg = new String(bufMsg, 0, num);
            System.out.println(msg);
        } finally {
            socket.close(); // 确保 socket 被关闭
            if (DelBackup) {  // DeleteBackupFile
                try {
                    zipPath.delete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public static void UploadBackup(File ZipPath) throws IOException {
        String Enabled = get_config("uploadenabled");
        if (Enabled.equals("y")){
            String ip = get_config("server_ip");
            int port = Integer.valueOf(get_config("server_port"));
            String delbackup = get_config("delbackup");
            System.out.println("INFO: Uploading...");
            if (delbackup.equals("y")){
                UploadBk(ip,port,ZipPath,true);
            }
            else{
                UploadBk(ip,port,ZipPath,false);
            }


        }
        else {
            System.out.println("WARN: Upload is not enabled");
        }
    }

}
