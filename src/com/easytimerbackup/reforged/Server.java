package com.easytimerbackup.reforged;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Server {
    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    public static String UPLOAD_DIR = config_read.get_config("rev_path"); // Directory to save uploaded files
    private static int PORT = Integer.valueOf(config_read.get_config("target_server_port")); // Listening port

    public static void UploadServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Connection accepted from " + clientSocket.getInetAddress());

                // Handle the upload in a separate thread
                new Thread(() -> handleUpload(clientSocket)).start();
            }
        } catch (IOException e) {
            LOGGER.error("Server error: ", e);
        }
    }

    private static void handleUpload(Socket clientSocket) {
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

            // Read the expected MD5 hash from the client
            String clientMd5 = dis.readUTF();
            LOGGER.info("Received expected MD5 from client: " + clientMd5);

            // Read file name and create a new file in the upload directory
            String fileName = dis.readUTF();
            File file = new File(UPLOAD_DIR, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                // Read file data from client
                while ((bytesRead = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // Calculate the MD5 hash of the received file
            String serverMd5 = CalcMD5.calculateMD5(file);
            LOGGER.info("Calculated MD5 of received file: " + serverMd5);

            // Compare MD5 hashes
            if (clientMd5.equals(serverMd5)) {
                dos.writeUTF("ok");
                LOGGER.info("File uploaded and MD5 verified successfully.");
            } else {
                dos.writeUTF("fail");
                LOGGER.warn("MD5 mismatch. Expected: " + clientMd5 + ", but calculated: " + serverMd5);
                file.delete(); // Delete the file if MD5 doesn't match
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error("Error during file upload handling: ", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close client socket.", e);
            }
        }
    }


}
