package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    public static final String UPLOAD_DIR = config_read.get_config("rev_path"); // Directory to save uploaded files
    private static final int PORT = Integer.valueOf(config_read.get_config("server_port")); // Listening port

    public static void handleClient(Socket clientSocket) {

        try (InputStream in = clientSocket.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(in);
             DataInputStream dis = new DataInputStream(bis)) {

            // Read the filename from the input stream
            String fileName = dis.readUTF();
            File outputFile = new File(UPLOAD_DIR, fileName);

            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytes = 0;

                // Receive file data
                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                bos.flush();
                LOGGER.info("File received: " + fileName + ", size: " + totalBytes + " bytes");

                // Send confirmation message to client
                OutputStream out = clientSocket.getOutputStream();
                String confirmationMessage = "File upload successful, size: " + totalBytes + " bytes";
                out.write(confirmationMessage.getBytes());
                out.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void UploadServer() {

        new File(UPLOAD_DIR).mkdirs();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server started, waiting for client connections...");
            LOGGER.info("Listening on port " + PORT);
            LOGGER.info("File Path: " + UPLOAD_DIR);


            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Client connected: " + clientSocket.getInetAddress());

                // Handle client requests
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
