package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class Upload {
    private static final Logger LOGGER = LogManager.getLogger(Upload.class);

    // 上传文件方法
    public static void UploadBk(String ip, int port, File zipPath, boolean DelBackup, boolean VerifyMD5) {
        Socket socket = new Socket();
        boolean uploadSuccess = false; // 上传成功标志

        try {
            String fileMd5 = CalcMD5.calculateMD5(zipPath);  // 计算文件的MD5
            LOGGER.info("Calculated MD5: " + fileMd5);

            while (!uploadSuccess) { // 循环直到上传成功或失败
                socket.connect(new InetSocketAddress(ip, port), 5000);

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // 发送MD5值和文件名
                dos.writeUTF(fileMd5);
                dos.writeUTF(zipPath.getName());

                // 发送文件内容
                FileInputStream fileInputStream = new FileInputStream(zipPath);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.flush();
                fileInputStream.close();

                // 接收服务端响应
                String response = dis.readUTF();
                if ("ok".equals(response)) {
                    uploadSuccess = true;  // 上传成功
                    LOGGER.info("Upload successful. Server response: " + response);

                    if (DelBackup && "y".equals(config_read.get_config("delbackup"))) {  // 删除文件依据配置
                        if (zipPath.delete()) {
                            LOGGER.info("Backup file deleted successfully.");
                        } else {
                            LOGGER.warn("Failed to delete backup file.");
                        }
                    }

                } else if ("fail".equals(response)) {
                    LOGGER.warn("Server response: fail. Retrying upload...");
                    socket.close(); // 关闭连接，准备重试
                } else {
                    LOGGER.warn("Unexpected response from server: " + response);
                    break; // 非预期响应，退出循环
                }
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("Upload failed", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                LOGGER.error("Socket close failed", e);
            }
        }
    }


    public static void UploadBackup(File zipPath) throws IOException {
        if (!"y".equals(config_read.get_config("uploadenabled"))) {
            LOGGER.info("Upload was not enabled.");
            return;
        }

        String ip = config_read.get_config("server_ip");
        int port = Integer.parseInt(config_read.get_config("server_port"));
        boolean deleteBackup = "y".equals(config_read.get_config("delbackup"));
        boolean verifyMd5 = "y".equals(config_read.get_config("verifymd5"));

        LOGGER.info("Uploading...");
        UploadBk(ip, port, zipPath, deleteBackup, verifyMd5);
    }



}
