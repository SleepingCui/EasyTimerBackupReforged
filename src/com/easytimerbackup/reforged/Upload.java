package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Upload {
    private static final Logger LOGGER = LogManager.getLogger(Upload.class);

    // 上传文件方法
    private static void UploadBk(String ip, int port, File zipPath, boolean DelBackup) {
        Socket socket = new Socket();
        try {
            // 设置连接超时时间为10秒（10000毫秒）
            socket.connect(new InetSocketAddress(ip, port), 10000); // 连接到服务器，超时设置为10秒

            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 FileInputStream fis = new FileInputStream(zipPath)) {

                // 发送文件名
                dos.writeUTF(zipPath.getName());

                long totalSize = zipPath.length(); // 获取文件总大小
                long uploadedSize = 0; // 已上传的字节数
                byte[] buffer = new byte[1024];
                int length;

                // 进度条参数
                int barLength = 50; // 进度条长度

                // 发送文件数据
                while ((length = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, length);
                    uploadedSize += length; // 更新已上传的字节数

                    // 计算上传进度
                    int progressPercentage = (int) ((uploadedSize * 100) / totalSize);
                    int progressBars = (int) ((double) progressPercentage / 100 * barLength); // 进度条长度比例
                    String progressBar = "[" + "=".repeat(progressBars) + " ".repeat(barLength - progressBars) + "]";

                    // 格式化进度信息
                    String progressMessage = String.format("\r%s %d%% | Uploaded: %dMB / %dMB",
                            progressBar,
                            progressPercentage,
                            uploadedSize / (1024 * 1024),
                            totalSize / (1024 * 1024));

                    // 显示进度条
                    System.out.print(progressMessage);
                }

                dos.flush(); // 确保所有数据都被发送
                socket.shutdownOutput(); // 关闭输出流以指示文件发送完毕

                // 接收服务器返回的消息
                InputStream in = socket.getInputStream();
                byte[] bufMsg = new byte[1024];
                int num = in.read(bufMsg);
                String msg = new String(bufMsg, 0, num);
                System.out.println(); // 换行
                LOGGER.info(msg);
            }
        } catch (SocketTimeoutException e) {
            LOGGER.error("Socket timed out");
            LOGGER.error(e.getMessage(),e); // 输出堆栈跟踪
        } catch (IOException e) {
            LOGGER.error("I/O exception");
            LOGGER.error(e.getMessage(),e); // 输出堆栈跟踪
        } finally {
            try {
                socket.close(); // 确保 socket 被关闭
            } catch (IOException e) {
                LOGGER.error(e.getMessage(),e); // 输出堆栈跟踪
            }
            if (DelBackup) {  // 删除备份文件
                try {
                    zipPath.delete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void UploadBackup(File ZipPath) throws IOException {
        String Enabled = config_read.get_config("uploadenabled");
        if (Enabled.equals("y")) {
            String ip = config_read.get_config("server_ip");
            int port = Integer.parseInt(config_read.get_config("server_port"));
            String delbackup = config_read.get_config("delbackup");
            LOGGER.info("Uploading...");
            if (delbackup.equals("y")) {
                UploadBk(ip, port, ZipPath, true);
            } else {
                UploadBk(ip, port, ZipPath, false);
            }
        } else {
            LOGGER.warn("Upload is not enabled");
        }
    }
}
