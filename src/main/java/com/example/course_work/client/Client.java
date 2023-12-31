package com.example.course_work.client;
import com.example.course_work.encryption.CFB;
import com.example.course_work.encryption.Deal;
import com.example.course_work.encryption.NTRUEncrypt;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;

import com.example.course_work.entity.Message;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.google.gson.Gson;

public class Client {
    private Gson gson;
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private String host = "localhost";
    private Integer port = 8843;
    private Socket server = null;
    Scanner serverStream;
    byte[] symKey;
    byte[] iv;
    private Deal deal = null;

    public Client(String host, Integer port)
    {
        this.host = host;
        this.port = port;
        try
        {
            server = new Socket(host, port);
        }
        catch (UnknownHostException e) {
            throw  new RuntimeException("can't create server socket", e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        gson = new GsonBuilder().create();
        try {
            serverStream = new Scanner(server.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private byte[] generateSymKey()
    {
        // generate key
        byte[] key = new byte[16];
        new Random().nextBytes(key);
        return key;
    }
    private void initSymAlgorithm(byte[] key, byte[] iv)
    {
        deal = new Deal();
        deal.init(true, key);
    }

    public String getLocalAddress()
    {
        return server.getLocalAddress().getHostAddress() + ":" + server.getLocalPort();
    }
    public boolean requestSession(String address) throws Exception {
        // отправляем адрес желаемого собеседника
        sendMessage(new Message("address", List.of(address)), server);
        System.out.println("Отправили адрес");

        if (serverStream.hasNext())
        {
            Message keyRequest = parseMessage(serverStream.nextLine());
            if (!keyRequest.type.equals("key_request")) {
                // TODO: нужно обработать эту исключительную ситуацию
            }
            else
            {
                NTRUEncrypt NTRUEncrypt = new NTRUEncrypt();
                if (keyRequest.data.get(0).toString().equals("asym"))
                {
                    System.out.println("Нужно сгенерировать асимметричный ключ");
                    byte[] publicKey = NTRUEncrypt.generatePublicKey();
                    byte[] encodedPublicKey = Base64.getEncoder().encode(publicKey);
                    sendMessage(new Message("key", List.of(new String(encodedPublicKey))), server);
                    //System.out.println("Отправили асим ключ: " + new String(encodedPublicKey));
                    // принимаем зашифрованный симметричный
                    if (serverStream.hasNext()) {
                        Message symKeyAndIVMsg = parseMessage(serverStream.nextLine());
                        String[] keyAndIV = ((String) symKeyAndIVMsg.data.get(0)).split(":");
                        byte[] encryptedSymKey = keyAndIV[0].getBytes();
                        byte[] decodedSymKey = Base64.getDecoder().decode(encryptedSymKey);
                        byte[] d = NTRUEncrypt.generatePrivateKey(decodedSymKey);
                        symKey = NTRUEncrypt.decrypt(decodedSymKey, d);
                        //System.out.println("Приняли сим ключ: " + new String(symKey));

                        byte[] encryptedIV = keyAndIV[1].getBytes();
                        byte[] decodedIV = Base64.getDecoder().decode(encryptedIV);
                        byte[] _d = NTRUEncrypt.generatePrivateKey(decodedIV);
                        iv = NTRUEncrypt.decrypt(decodedIV, d);
                        //System.out.println("расшифровали iv: " + new String(iv));
                    }
                    // принимаем зашифрованный вектор инициализации


                }
                else
                {
                    // получаем открытый асимметричный ключ и шифруем им наш симметричный ключ
                    if (serverStream.hasNext()) {
                        Message asymKeyMsg = parseMessage(serverStream.nextLine());
                        byte[] asymKey =  Base64.getDecoder().decode(((String) asymKeyMsg.data.get(0)).getBytes());
                        System.out.println("Получили асим ключ: " + new String(asymKey));
                        symKey = generateSymKey();
                        byte[] encryptedSymKey = NTRUEncrypt.encrypt(symKey, asymKey);
                        byte[] encodedBufSymKey = Base64.getEncoder().encode(encryptedSymKey);

                        // гегенрируем iv, шифруем и отправляеи
                        iv = generateSymKey();
                        byte[] encryptedIV = NTRUEncrypt.encrypt(iv, asymKey);
                        byte[] encodedIV = Base64.getEncoder().encode(encryptedIV);
                        sendMessage(new Message("key", List.of(new String(encodedBufSymKey) + ":" + new String(encodedIV))), server);
                        //System.out.println("Отправили зашифр сим ключ: " + new String(encryptedSymKey));
                        //System.out.println("Отправили зашифр iv: " + new String(encodedIV));
                    }
                }
                initSymAlgorithm(symKey, iv);
            }
        }
        return true;
    }

    public boolean downloadFile(Path dir, String filename)
    {
        Path encryptedTargetFile = null;
        try {
            encryptedTargetFile = Files.createTempFile("encrypted", "temp");
        } catch (IOException e) {
            throw new RuntimeException("can't create temp file", e);
        }
        long downloadedBytes = 0;

        // посылаем сообщение что хотим скачать файл
        Message downloadRequest = new Message("download", List.of(filename));
        sendMessage(downloadRequest, server);

        // далее принимаем на вход файл по частям (по 1 мб)
        // если type - downloading - значит продолжаем скачивать, если finished - закончили
        try (OutputStream fileOutStream = new FileOutputStream(encryptedTargetFile.toFile())) {
            System.out.println("start downloading file");
            while(serverStream.hasNext())
            {
                System.out.println("still downloading file...");
                Message msg = parseMessage(serverStream.nextLine());
                if(msg.type.equals("fail"))
                {
                    // на сервере нет такого файла
                    Files.deleteIfExists(encryptedTargetFile);
                    return false;
                }
                else if(msg.type.equals("downloading"))
                {
                    byte[] bytes = ((String)msg.data.get(0)).getBytes();
                    byte[] decodedBytes = Base64.getDecoder().decode(bytes);
                    downloadedBytes = downloadedBytes + decodedBytes.length;
                    fileOutStream.write(decodedBytes);
                }
                else if(msg.type.equals("finished"))
                {
                    // закончили
                    System.out.println("finished downloading file");
                    break;
                }
                else
                {
                    throw new RuntimeException("Unknown returned message type while downloading file");
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        // now decrypt file
        String targetFile = dir.resolve(filename).toString();
        decryptFile(targetFile, encryptedTargetFile.toString());
        try {
            Files.deleteIfExists(encryptedTargetFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private void decryptFile(String targetFile, String encryptedFile)
    {
        try (InputStream iStream = new FileInputStream(encryptedFile);
            OutputStream oStream = new FileOutputStream(targetFile)) {
            CFB cfb = new CFB();
            cfb.decrypt(iStream, oStream, 16, deal, iv);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void encryptFile(String fileToEncrypt, String encryptedFile)
    {
        try (InputStream iStream = new FileInputStream(fileToEncrypt);
             OutputStream oStream = new FileOutputStream(encryptedFile)) {
            CFB cfb = new CFB();
            cfb.encrypt(iStream, oStream, 16, deal, iv);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadFile(Path file)
    {
        Message downloadRequest = new Message("load", List.of(file.getFileName().toString()));
        sendMessage(downloadRequest, server);
        Path encryptedFile = null;
        try {
            encryptedFile =  Files.createTempFile("encrypted", "tmp");
            encryptFile(file.toString(), encryptedFile.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //шифруем файл



        final int BUFSIZ = 1024 * 1024; // Mb
        byte[] buf;
        try (InputStream inputStream = new FileInputStream(encryptedFile.toFile())) {
            System.out.println("start loading file");
            do
            {
                System.out.println("still loading file...");
                buf = inputStream.readNBytes(BUFSIZ);

                if(buf.length != 0)
                {
                    byte[] encodedBuf = Base64.getEncoder().encode(buf);
                    String strBuf = new String(encodedBuf);
                    sendMessage(new Message("loading", List.of(strBuf)), server);
                }
            }
            while(buf.length == BUFSIZ);
            sendMessage(new Message("finished", List.of()), server);
            System.out.println("finished loading file");
            // удаляем временный зафированный файл
        } catch (FileNotFoundException e) {
            sendMessage(new Message("fail", List.of("file " + encryptedFile.getFileName() + " doesn't exist")), server);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.deleteIfExists(encryptedFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    void sendMessage(Message msg, Socket server){
        PrintWriter pr = null;
        try {
            pr = new PrintWriter(server.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        pr.println(gson.toJson(msg));
        System.out.println("message: " + gson.toJson(msg));
    }
    private Message parseMessage(String msg)
    {
        System.out.println("get message: " + msg);
        return gson.fromJson(msg, Message.class);
    }

    public enum STAGE
    {
        CONNECTION,
        GENERATING_KEY,
        MAIN_JOB
    }
}
