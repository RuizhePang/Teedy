package com.sismics.docs.core.util;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.sismics.docs.core.constant.Constants;
import com.sismics.docs.core.dao.FileDao;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.event.DocumentUpdatedAsyncEvent;
import com.sismics.docs.core.event.FileCreatedAsyncEvent;
import com.sismics.docs.core.model.context.AppContext;
import com.sismics.docs.core.model.jpa.File;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.util.ImageDeskew;
import com.sismics.util.Scalr;
import com.sismics.util.context.ThreadLocalContext;
import com.sismics.util.io.InputStreamReaderThread;
import com.sismics.util.mime.MimeType;
import com.sismics.util.mime.MimeTypeUtil;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * File entity utilities.
 * 
 * @author bgamard
 */
public class FileUtil {
    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    /**
     * File ID of files currently being processed.
     */
    private static final Set<String> processingFileSet = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * Optical character recognition on an image.
     *
     * @param language Language to OCR
     * @param image Buffered image
     * @return Content extracted
     * @throws Exception e
     */
    public static String ocrFile(String language, BufferedImage image) throws Exception {
        // Upscale, grayscale and deskew the image
        BufferedImage resizedImage = Scalr.resize(image, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, 3500, Scalr.OP_ANTIALIAS, Scalr.OP_GRAYSCALE);
        image.flush();
        ImageDeskew imageDeskew = new ImageDeskew(resizedImage);
        BufferedImage deskewedImage = Scalr.rotate(resizedImage, - imageDeskew.getSkewAngle(), Scalr.OP_ANTIALIAS, Scalr.OP_GRAYSCALE);
        resizedImage.flush();
        Path tmpFile = AppContext.getInstance().getFileService().createTemporaryFile();
        ImageIO.write(deskewedImage, "tiff", tmpFile.toFile());

        List<String> result = Lists.newLinkedList(Arrays.asList("tesseract", tmpFile.toAbsolutePath().toString(), "stdout", "-l", language));
        ProcessBuilder pb = new ProcessBuilder(result);
        Process process = pb.start();

        // Consume the process error stream
        final String commandName = pb.command().get(0);
        new InputStreamReaderThread(process.getErrorStream(), commandName).start();

        // Consume the data as text
        try (InputStream is = process.getInputStream()) {
            return CharStreams.toString(new InputStreamReader(is, StandardCharsets.UTF_8));
        }
    }

    /**
     * Remove a file from the storage filesystem.
     * 
     * @param fileId ID of file to delete
     */
    public static void delete(String fileId) throws IOException {
        Path storedFile = DirectoryUtil.getStorageDirectory().resolve(fileId);
        Path webFile = DirectoryUtil.getStorageDirectory().resolve(fileId + "_web");
        Path thumbnailFile = DirectoryUtil.getStorageDirectory().resolve(fileId + "_thumb");
        
        if (Files.exists(storedFile)) {
            Files.delete(storedFile);
        }
        if (Files.exists(webFile)) {
            Files.delete(webFile);
        }
        if (Files.exists(thumbnailFile)) {
            Files.delete(thumbnailFile);
        }
    }

    /**
     * Create a new file.
     *
     * @param name File name, can be null
     * @param previousFileId ID of the previous version of the file, if the new file is a new version
     * @param unencryptedFile Path to the unencrypted file
     * @param fileSize File size
     * @param language File language, can be null if associated to no document
     * @param userId User ID creating the file
     * @param documentId Associated document ID or null if no document
     * @return File ID
     * @throws Exception e
     */
    public static String createFile(String name, String previousFileId, Path unencryptedFile, long fileSize, String language, String userId, String documentId) throws Exception {
        // Validate mime type
        String mimeType;
        try {
            mimeType = MimeTypeUtil.guessMimeType(unencryptedFile, name);
        } catch (IOException e) {
            throw new IOException("ErrorGuessMime", e);
        }

        // Validate user quota
        UserDao userDao = new UserDao();
        User user = userDao.getById(userId);
        if (user.getStorageCurrent() + fileSize > user.getStorageQuota()) {
            throw new IOException("QuotaReached");
        }

        // Validate global quota
        String globalStorageQuotaStr = System.getenv(Constants.GLOBAL_QUOTA_ENV);
        if (!Strings.isNullOrEmpty(globalStorageQuotaStr)) {
            long globalStorageQuota = Long.parseLong(globalStorageQuotaStr);
            long globalStorageCurrent = userDao.getGlobalStorageCurrent();
            if (globalStorageCurrent + fileSize > globalStorageQuota) {
                throw new IOException("QuotaReached");
            }
        }

        // Prepare the file
        File file = new File();
        file.setOrder(0);
        file.setVersion(0);
        file.setLatestVersion(true);
        file.setDocumentId(documentId);
        file.setName(StringUtils.abbreviate(name, 200));
        file.setMimeType(mimeType);
        file.setUserId(userId);
        file.setSize(fileSize);

        // Get files of this document
        FileDao fileDao = new FileDao();
        if (documentId != null) {
            if (previousFileId == null) {
                // It's not a new version, so put it in last order
                file.setOrder(fileDao.getByDocumentId(userId, documentId).size());
            } else {
                // It's a new version, update the previous version
                File previousFile = fileDao.getActiveById(previousFileId);
                if (previousFile == null || !previousFile.getDocumentId().equals(documentId)) {
                    throw new IOException("Previous version mismatch");
                }

                if (previousFile.getVersionId() == null) {
                    previousFile.setVersionId(UUID.randomUUID().toString());
                }

                // Copy the previous file metadata
                file.setOrder(previousFile.getOrder());
                file.setVersionId(previousFile.getVersionId());
                file.setVersion(previousFile.getVersion() + 1);

                // Update the previous file
                previousFile.setLatestVersion(false);
                fileDao.update(previousFile);
            }
        }

        // Create the file
        String fileId = fileDao.create(file, userId);

        // Save the file
        Cipher cipher = EncryptionUtil.getEncryptionCipher(user.getPrivateKey());
        Path path = DirectoryUtil.getStorageDirectory().resolve(file.getId());
        try (InputStream inputStream = Files.newInputStream(unencryptedFile)) {
            Files.copy(new CipherInputStream(inputStream, cipher), path);
        }

        // Update the user quota
        user.setStorageCurrent(user.getStorageCurrent() + fileSize);
        userDao.updateQuota(user);

        // Raise a new file created event and document updated event if we have a document
        startProcessingFile(fileId);
        FileCreatedAsyncEvent fileCreatedAsyncEvent = new FileCreatedAsyncEvent();
        fileCreatedAsyncEvent.setUserId(userId);
        fileCreatedAsyncEvent.setLanguage(language);
        fileCreatedAsyncEvent.setFileId(file.getId());
        fileCreatedAsyncEvent.setUnencryptedFile(unencryptedFile);
        ThreadLocalContext.get().addAsyncEvent(fileCreatedAsyncEvent);

        if (documentId != null) {
            DocumentUpdatedAsyncEvent documentUpdatedAsyncEvent = new DocumentUpdatedAsyncEvent();
            documentUpdatedAsyncEvent.setUserId(userId);
            documentUpdatedAsyncEvent.setDocumentId(documentId);
            ThreadLocalContext.get().addAsyncEvent(documentUpdatedAsyncEvent);
        }

        return fileId;
    }

    /**
     * Start processing a file.
     *
     * @param fileId File ID
     */
    public static void startProcessingFile(String fileId) {
        processingFileSet.add(fileId);
        log.info("Processing started for file: " + fileId);
    }

    /**
     * End processing a file.
     *
     * @param fileId File ID
     */
    public static void endProcessingFile(String fileId) {
        processingFileSet.remove(fileId);
        log.info("Processing ended for file: " + fileId);
    }

    /**
     * Return true if a file is currently processing.
     *
     * @param fileId File ID
     * @return True if the file is processing
     */
    public static boolean isProcessingFile(String fileId) {
        return processingFileSet.contains(fileId);
    }

    /**
     * Get the size of a file on disk.
     *
     * @param fileId the file id
     * @param user   the file owner
     * @return the size or -1 if something went wrong
     */
    public static long getFileSize(String fileId, User user) {
        // To get the size we copy the decrypted content into a null output stream
        // and count the copied byte size.
        Path storedFile = DirectoryUtil.getStorageDirectory().resolve(fileId);
        if (! Files.exists(storedFile)) {
            log.debug("File does not exist " + fileId);
            return File.UNKNOWN_SIZE;
        }
        try (InputStream fileInputStream = Files.newInputStream(storedFile);
             InputStream inputStream = EncryptionUtil.decryptInputStream(fileInputStream, user.getPrivateKey());
             CountingInputStream countingInputStream = new CountingInputStream(inputStream);
        ) {
            IOUtils.copy(countingInputStream, NullOutputStream.NULL_OUTPUT_STREAM);
            return countingInputStream.getByteCount();
        } catch (Exception e) {
            log.debug("Can't find size of file " + fileId, e);
            return File.UNKNOWN_SIZE;
        }
    }

    public static java.io.File decryptFile(String fileId, User user) throws IOException {
        Path storedFile = DirectoryUtil.getStorageDirectory().resolve(fileId);
        if (! Files.exists(storedFile)) {
            log.debug("File does not exist " + fileId);
            return null;
        }
        try (InputStream fileInputStream = Files.newInputStream(storedFile);
             InputStream inputStream = EncryptionUtil.decryptInputStream(fileInputStream, user.getPrivateKey());
        ) {
            Path tmpFile = AppContext.getInstance().getFileService().createTemporaryFile();
            Files.copy(inputStream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            return tmpFile.toFile();
        } catch (Exception e) {
            log.debug("Can't decrypt file " + fileId, e);
            return null;
        }
    }

    public static java.io.File translateFile(File file, String language, User user) throws Exception {
        java.io.File ioFile = decryptFile(file.getId(), user);
        
        String accessToken = "24.0de9e87730bb10a866bf3dd3d1ac3e9d.2592000.1748674788.282335-118716685";
        String from = "auto";
        String to = language;
        String domain = "general";
        String inputFileName = ioFile.getName();
        String inputFormat = file.getMimeType();
        if (inputFormat.equals(MimeType.APPLICATION_PDF)) {
            inputFormat = "pdf";
        } else if (inputFormat.equals(MimeType.TEXT_PLAIN)) {
            inputFormat = "txt";
        } else if (inputFormat.equals(MimeType.OFFICE_DOCUMENT)) {
            inputFormat = "docx";
        } else if (inputFormat.equals(MimeType.OFFICE_PRESENTATION)) {
            inputFormat = "pptx";
        } else if (inputFormat.equals(MimeType.OFFICE_SHEET)) {
            inputFormat = "xlsx";
        } else {
            inputFormat = "txt";
        }

        String outputFormats = inputFormat;
        // String outputFilenamePrefix = file.getName().substring(0, file.getName().lastIndexOf('.')) + "_" + language;
        String outputFilenamePrefix = "translated";
        byte[] fileBytes = fileBytes = Files.readAllBytes(ioFile.toPath());
        String inputContent = Base64.getEncoder().encodeToString(fileBytes);

        JsonObject inputJson = Json.createObjectBuilder()
                .add("content", inputContent)
                .add("format", "pdf")
                .add("filename", inputFileName)
                .build();


        JsonArrayBuilder outputFormatsArray = Json.createArrayBuilder().add(outputFormats);
        JsonObject outputJson = Json.createObjectBuilder()
                .add("formats", outputFormatsArray)
                .add("filename_prefix", outputFilenamePrefix)
                .build();

        JsonObject requestBody = Json.createObjectBuilder()
                .add("from", from)
                .add("to", to)
                .add("domain", domain)
                .add("input", inputJson)
                .add("output", outputJson)
                .build();

        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(requestBody);
        String jsonRequest = writer.toString();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/create?access_token=" + accessToken))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        JsonReader jsonReader = Json.createReader(new StringReader(responseBody));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();

        String id = jsonObject
                .getJsonObject("result")
                .getString("id");
        String url = "https://aip.baidubce.com/rpc/2.0/mt/v2/doc-translation/query?access_token=" + accessToken;
        String filename = null;
        while (true) {
            requestBody = Json.createObjectBuilder()
                    .add("id", id)
                    .build();

            String requestJson = requestBody.toString();

            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();

            JsonReader reader = Json.createReader(new StringReader(responseBody));
            jsonObject = reader.readObject();
            reader.close();

            JsonObject data = jsonObject
                    .getJsonObject("result")
                    .getJsonObject("data");

            String status = data.getString("status");
            System.out.println("Current status: " + status);

            if ("Succeeded".equals(status)) {
                for (var fileJsonValue : data.getJsonObject("output").getJsonArray("files")) {
                    JsonObject fileObj = (JsonObject) fileJsonValue;
                    filename = fileObj.getString("filename");
                    String downloadUrl = fileObj.getString("url");

                    URL urlD = new URL(downloadUrl);
                    String encodedPath = URLEncoder.encode(urlD.getPath(), StandardCharsets.UTF_8)
                            .replace("+", "%20")
                            .replace("%2F", "/");

                    URI safeUri = new URI(
                            urlD.getProtocol(),
                            urlD.getHost(),
                            encodedPath,
                            urlD.getQuery(),
                            null
                    );

                    HttpRequest fileRequest = HttpRequest.newBuilder()
                            .uri(safeUri)
                            .build();

                    HttpResponse<InputStream> fileResponse = client.send(fileRequest, HttpResponse.BodyHandlers.ofInputStream());

                    try (InputStream in = fileResponse.body();
                        FileOutputStream out = new FileOutputStream(filename)) {
                        in.transferTo(out);
                        System.out.println("已保存文件: " + filename);
                    }
                }
                break;

            } else if ("Failed".equals(status)) {
                System.out.println("翻译失败: " + data.getString("reason", "未知原因"));
                break;
            }
            Thread.sleep(1000);
        }
        java.io.File myFile = new java.io.File(filename);
        myFile.renameTo(new java.io.File(file.getName().substring(0, file.getName().lastIndexOf('.')) + "_" + language + "." + inputFormat));
        myFile = new java.io.File(file.getName().substring(0, file.getName().lastIndexOf('.')) + "_" + language + "." + inputFormat);
        ioFile.delete();
        return myFile;
    }
}
