/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  main.Decryptor
 *  org.apache.commons.net.ftp.FTPClientConfig
 *  org.apache.commons.net.ftp.FTPReply
 *  org.apache.commons.net.ftp.FTPSClient
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package app;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import main.Decryptor;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import Common.PropertiesLoader;
import Common.Logging;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.Properties;
import org.apache.commons.net.ftp.FTPClient;


public class app {
    public static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static ch.qos.logback.classic.Logger logger = loggerContext.getLogger("app");
    public static PropertiesLoader pl;
    
    public static void processXMLData(String delimiter, String serviceurl, String file_path) throws IOException, NoSuchAlgorithmException, KeyManagementException, CertificateException, FileNotFoundException, KeyStoreException, InterruptedException {
        String result = "";
        System.out.println("Reading " + file_path);
        logger.info("Reading " + file_path);

        // Read file
        try {
            try (BufferedReader br = Files.newBufferedReader(Paths.get(file_path))) {
                String line;
                while ((line = br.readLine()) != null) {
                    result = result + line;
                }
            } catch (IOException e) {
                System.out.println("Error while reading data: " + e.getMessage());
                logger.error("Error while reading data: " + e.getMessage());
                throw e;
            }
        } catch (IOException e) {
            System.out.println("Error while reading data: " + e.getMessage());
            logger.error("Error while reading data: " + e.getMessage());
            return; // Exit if reading the file fails
        }

        // Process data
        String[] dataResult = result.split("[" + delimiter + "]+");
        System.out.println("Processing " + file_path);
        logger.info("Processing " + file_path);

        if (result.length() > 0) {
            for (String xml : dataResult) {
                System.out.println("XML Sent: " + xml);
                logger.info("XML Sent: " + xml);
                try {
                    // Create SSL context for HTTPS requests
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }};
                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                    HostnameVerifier allHostsValid = new HostnameVerifier(){

                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    };

                    // Create HTTPS connection
                    URL obj1 = new URL(serviceurl);
                    /*HttpsURLConnection con1 = (HttpsURLConnection) obj1.openConnection();
                    con1.setHostnameVerifier(allHostsValid);
                    con1.setSSLSocketFactory(sc.getSocketFactory());
                    con1.setRequestMethod("POST");
                    con1.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
                    con1.setDoOutput(true);*/
                    if (obj1.getProtocol().equals("https")) {
                        // Jika HTTPS, gunakan HttpsURLConnection
                        HttpsURLConnection con1 = (HttpsURLConnection) obj1.openConnection();
                        con1.setHostnameVerifier(allHostsValid);
                        con1.setSSLSocketFactory(sc.getSocketFactory());
                        con1.setRequestMethod("POST");
                        con1.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
                        con1.setDoOutput(true);
                        
                        // Send the XML data
                        try (DataOutputStream wr = new DataOutputStream(con1.getOutputStream())) {
                            wr.writeBytes(xml);
                            wr.flush();
                        }

                        // Get the response
                        int respCode = con1.getResponseCode();
                        System.out.println("Response Code : " + respCode);
                        logger.info("Response Code : " + respCode);

                        // Read the response
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(con1.getInputStream()))) {
                            StringBuilder response = new StringBuilder();
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            String responseXML = response.toString();
                            System.out.println("XML Response : " + responseXML);
                            logger.info("XML Response : " + responseXML);
                        }
                        con1.disconnect();
                    } else if (obj1.getProtocol().equals("http")) {
                        // Jika HTTP, gunakan HttpURLConnection
                        HttpURLConnection con1 = (HttpURLConnection) obj1.openConnection();
                        con1.setRequestMethod("POST");
                        con1.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
                        con1.setDoOutput(true);
                        System.out.println("Using HTTP connection");
                        logger.info("Using HTTP connection");
                        
                        // Send the XML data
                        try (DataOutputStream wr = new DataOutputStream(con1.getOutputStream())) {
                            wr.writeBytes(xml);
                            wr.flush();
                        }

                        // Get the response
                        int respCode = con1.getResponseCode();
                        System.out.println("Response Code : " + respCode);
                        logger.info("Response Code : " + respCode);

                        // Read the response
                        if (respCode == HttpURLConnection.HTTP_INTERNAL_ERROR) { 
                            // Jika server mengembalikan HTTP 500 (Internal Server Error)
                            try (BufferedReader in = new BufferedReader(new InputStreamReader(con1.getErrorStream()))) {
                                StringBuilder errorResponse = new StringBuilder();
                                String inputLine;
                                while ((inputLine = in.readLine()) != null) {
                                    errorResponse.append(inputLine);
                                }
                                System.out.println("Error Response: " + errorResponse.toString());
                                logger.error("Error Response: " + errorResponse.toString());
                            }
                        }
                        con1.disconnect();
                    } else {
                        // Jika URL tidak menggunakan http atau https
                        System.out.println("Unsupported protocol: " + obj1.getProtocol());
                        logger.error("Unsupported protocol: " + obj1.getProtocol());
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("Error while processing XML: " + e.getMessage());
                    logger.error("Error while processing XML: " + e.getMessage());
                }

                // Sleep between requests to avoid overloading the server
                TimeUnit.SECONDS.sleep(1L);
                }
            } else {
                System.out.println("Failed to process " + file_path + ", file is empty");
                logger.error("Failed to process " + file_path + ", file is empty");
            }
        }       

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
    public static void FTPdownload(String FTP_Host, String FTP_Username, String FTP_Password, String FTP_Directory, String Local_Directory, String delimiter, String serviceurl, int isDeletedFTPS, int FTP_Port, String processed_date) throws Exception {
        //FTPSClient ftp = new FTPSClient(true);
        FTPClient ftp = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();
        ftp.configure(config);
        boolean error = false;
        try {
            ftp.connect(FTP_Host, FTP_Port);
            String dec_pwd = FTP_Password;
            ftp.enterLocalPassiveMode();
            ftp.login(FTP_Username, dec_pwd);
            //ftp.execPBSZ(0L);
            //ftp.execPROT("P");
            //ftp.type(2);
            System.out.println("Connected to " + FTP_Host + ".");
            logger.info("Connected to " + FTP_Host + ".");
            System.out.print(ftp.getReplyString());
            ftp.changeWorkingDirectory(FTP_Directory);
            logger.info("Open Working Directory " + ftp.printWorkingDirectory());
            System.out.println("Change working directory to " + FTP_Directory);
            System.out.println("Current working directory " + ftp.printWorkingDirectory());
            String[] ftpFile = ftp.listNames(FTP_Directory);
            System.out.println("Length " + ftpFile.length);
            logger.info("Found " + ftpFile.length + " files on directory");
            for (String fileName : ftpFile) {
                String tmpOriginalFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                logger.info("Checking file " + tmpOriginalFileName + "...");
                if (fileNameChecker.checkingFileName(tmpOriginalFileName.toLowerCase()).booleanValue()) {
                    System.out.println("Downloading file " + fileName);
                    logger.info("File " + tmpOriginalFileName + " valid, processing download " + tmpOriginalFileName + "...");
                    String localFileName = Local_Directory + tmpOriginalFileName;
                    File downloadFile1 = new File(localFileName);
                    BufferedOutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1));
                    boolean success = ftp.retrieveFile(fileName, (OutputStream)outputStream1);
                    ((OutputStream)outputStream1).close();
                    if (success) {
                        System.out.println("File " + tmpOriginalFileName + " has been downloaded successfully.");
                        logger.info("File " + tmpOriginalFileName + " has been downloaded successfully.");
                        if (isDeletedFTPS == 1) {
                            TimeUnit.SECONDS.sleep(3L);
                            System.out.println("Processing delete " + tmpOriginalFileName + " on FTP Server...");
                            logger.info("Processing delete " + tmpOriginalFileName + " on FTP Server...");
                            ftp.deleteFile(tmpOriginalFileName);
                            System.out.println("File " + tmpOriginalFileName + " has been deleted successfully");
                            logger.info("File " + tmpOriginalFileName + " has been deleted successfully");
                        }
                    }
                } else {
                    logger.info("File " + tmpOriginalFileName + " not valid");
                }
                System.out.println("------------------------------------");
            }
            app.callProcessXML(Local_Directory, delimiter, serviceurl);
            app.archiveProcessedFile(processed_date, Local_Directory);
            int reply = ftp.getReplyCode();
            
            if (!FTPReply.isPositiveCompletion((int)reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                logger.error("FTP server refused connection.");
                System.exit(1);
            }
            ftp.logout();
        }
        catch (IOException e) {
            try {
                error = true;
                e.printStackTrace();
                logger.error("Error FTP " + e.getMessage());
            }
            catch (Throwable throwable) {
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                        logger.info("Disconnect from " + FTP_Host + ".");
                    }
                    catch (IOException ioe) {
                        ioe.printStackTrace();
                        logger.error("Error FTP " + ioe.getMessage());
                    }
                    logger.info("Finished run Batch Screening APP");
                }
                System.exit(error ? 1 : 0);
                throw throwable;
            }
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                    logger.info("Disconnect from " + FTP_Host + ".");
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                    logger.error("Error FTP " + ioe.getMessage());
                }
                logger.info("Finished run Batch Screening APP");
            }
            System.exit(error ? 1 : 0);
        }
        if (ftp.isConnected()) {
            try {
                ftp.disconnect();
                logger.info("Disconnect from " + FTP_Host + ".");
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                logger.error("Error FTP " + ioe.getMessage());
            }
            logger.info("Finished run Batch Screening APP");
        }
        System.exit(error ? 1 : 0);
    }

    public static void callProcessXML(String Local_Directory, String delimiter, String serviceurl) {
        ArrayList<String> filesInFolder = new ArrayList<String>();
        try {
            File folder = new File(Local_Directory);
            File[] fileArray = folder.listFiles();

            if (fileArray != null && fileArray.length > 0) {
                for (File file : fileArray) {
                    if (!file.isFile()) continue;

                    String fileName = file.toString();
                    // Menggunakan File.separator untuk path yang lebih aman lintas platform
                    String originalFileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);

                    System.out.println("Found file: " + originalFileName);
                    logger.info("ORI File Name: " + originalFileName);

                    // Pastikan file memenuhi syarat untuk diproses
                    if (!fileNameChecker.checkingFileName(originalFileName.toLowerCase()).booleanValue()) continue;

                    filesInFolder.add(Local_Directory + File.separator + originalFileName);
                }
            } else {
                System.out.println("No files found in the directory.");
                logger.info("No files found in the directory.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("System get error : " + e.getCause());
            logger.error("System get error : " + e.getCause());
        }

        if (filesInFolder.size() < 1) {
            System.out.println("There is no file need to process");
            logger.info("There is no file need to process");
        }

        try {
            for (String string : filesInFolder) {
                String tmpOriginalFileName = string.substring(string.lastIndexOf(File.separator) + 1);
                System.out.println("Processing XML file..");
                app.processXMLData(delimiter, serviceurl, string);

                logger.info("Move file " + tmpOriginalFileName + " to TEMP folder...");
                // Pastikan folder TEMP sudah ada dan bisa digunakan
                Path temp = Files.move(Paths.get(Local_Directory + File.separator + tmpOriginalFileName), 
                                       Paths.get(Local_Directory + File.separator + "TEMP" + File.separator + tmpOriginalFileName), 
                                       StandardCopyOption.REPLACE_EXISTING);

                if (temp != null) {
                    System.out.println("File " + tmpOriginalFileName + " has been moved successfully");
                    logger.info("File " + tmpOriginalFileName + " moved successfully");
                    continue;
                }
                System.out.println("Failed to move file " + tmpOriginalFileName);
                logger.error("Failed to move the file " + tmpOriginalFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error processing file XML : " + e.getMessage());
            logger.error("Error processing file XML : " + e.getMessage());
        }
    }


    public static void archiveProcessedFile(String filename, String Local_Directory) throws FileNotFoundException {
        try {
            logger.info("Archiving processed file...");
            String temp_path = Local_Directory + "/TEMP/";
            String arc_path = Local_Directory + "/ARCHIVE/";
            if (Files.exists(Paths.get(arc_path + filename + ".zip", new String[0]), new LinkOption[0])) {
                File file = new File(arc_path + filename + ".zip");
                file.delete();
            }
            File srcFile = new File(temp_path);
            File[] files = srcFile.listFiles();
            FileOutputStream fos = new FileOutputStream(arc_path + filename + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);
            for (int i = 0; i < files.length; ++i) {
                int length;
                byte[] buffer = new byte[1024];
                FileInputStream fis = new FileInputStream(files[i]);
                zos.putNextEntry(new ZipEntry(files[i].getName()));
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
            logger.info("Successfully archived processed file");
            for (File file : files) {
                file.delete();
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error("Process archive file error " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException, Exception {
        Properties prop = new Properties(); 
        pl = new PropertiesLoader(args[0]);
        Logging log = new Logging(); 
        log.configLog(pl,logger,loggerContext);
        
        String FTP_Host = pl.FTP_Host;
        String FTP_Username = pl.FTP_Username;
        String FTP_Password = pl.FTP_Password;
        String FTP_Directory = pl.FTP_Directory;
        String Local_Directory = pl.Local_Directory;
        String delimiter = pl.delimiter;
        String serviceurl = pl.serviceurl;
        int isDeletedFTPS = pl.isDeletedFTPS;
        int FTP_Port = pl.FTP_Port;
        System.out.println("Run IFMX Batch Screening APP");
        logger.info("Run IFMX Batch Screening APP");
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String processed_date = localDate.format(formatter);
        app.FTPdownload(FTP_Host, FTP_Username, FTP_Password, FTP_Directory, Local_Directory, delimiter, serviceurl, isDeletedFTPS, FTP_Port, processed_date);
    }
}
