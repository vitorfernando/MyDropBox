/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mydropbox;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Vitor
 */
public class MyDropBox {
static Vector namefiles;
static Timer timer;
static int seconds;
    private static void sendPOST(String pathfile,String type) throws IOException{
        final String POST_URL = "http://localhost:8080/dropboxserver/processRequests.php";
        final File uploadFile = new File(pathfile);

        String boundary = Long.toHexString(System.currentTimeMillis()); 
        String CRLF = "\r\n";
        String charset = "UTF-8";
        URLConnection connection = new URL(POST_URL).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (
            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
        ) {
            //writer.append("--" + boundary).append(CRLF);
            if(type.equals("created;")){
                writer.append("created;"+uploadFile.getName());
                writer.append(CRLF).flush();
                Files.copy(uploadFile.toPath(), output);
                output.flush();
            }else if(type.equals("deleted;")){
                writer.append("deleted;" + uploadFile.getName());
                writer.append(CRLF).flush();
            }

            int responseCode = ((HttpURLConnection) connection).getResponseCode();
            System.out.println("Response code: [" + responseCode + "]");
        }
    }

    public static void watchDirectoryPath(String pathDir) throws IOException {
        System.out.println("Monitorando diretorio: " + pathDir);
        File dir = new File(pathDir);
        Path path= dir.toPath();
        // We obtain the file system of the Path
        WatchService whatcher = FileSystems.getDefault().newWatchService();
        path.register(whatcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);
        // We create the new WatchService using the new try() block
        while(true){
            WatchKey key = null;
            seconds = 8;
            timer = new Timer();
            timer.schedule(new RemindTask1(), seconds*1000);
            
            try {
                key = whatcher.take();
            } catch (InterruptedException ex) {
                Logger.getLogger(MyDropBox.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            for (WatchEvent<?> watchEvent : key.pollEvents()){
                // Get the type of the event
                WatchEvent.Kind<?> kind = watchEvent.kind();
                
                // get file name
                WatchEvent<Path> ev = (WatchEvent<Path>) watchEvent;
                Path fileName = ev.context();
    
                if (OVERFLOW == kind) {
                    continue; // loop
                } 
                else if (ENTRY_CREATE == kind) {
                    System.out.println("Arquivo criado: " + fileName);
                    namefiles.addElement(pathDir+"\\"+fileName.getFileName());
                    sendPOST(pathDir+"\\"+fileName.getFileName(), "created;");
                }
                else if (ENTRY_DELETE == kind){
                    int index = namefiles.indexOf(pathDir+"\\"+fileName);
                    if(index != -1){
                        System.out.println("Arquivo deletado: " + fileName);
                        namefiles.removeElementAt(namefiles.indexOf(pathDir+"\\"+fileName));
                        sendPOST(pathDir+"\\"+fileName.getFileName(),"deleted;");
                    }
                }        
            }          
            if (!key.reset()){
                break; // loop
            }
        }
    }
    
    public static void inicVector(String dirPath) throws IOException{
        namefiles = new Vector();
        File dir = new File(dirPath);
        File[] listOfFiles = dir.listFiles();
        for(int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                namefiles.addElement(listOfFiles[i].getCanonicalPath());
            }
        }
    }
    
    public static void main(String[] args) throws IOException,
            InterruptedException {
        String path = "C:\\Users\\Vitor\\Documents\\MyDropBox";
        inicVector(path);
        watchDirectoryPath(path);
    }
    
    static class RemindTask1 extends TimerTask {

        @Override
        public void run() {
            try {
                final String POST_URL = "http://192.168.0.30:8080";

                String boundary = Long.toHexString(System.currentTimeMillis()); 
                String CRLF = "\r\n";
                String charset = "UTF-8";
                URLConnection connection = new URL(POST_URL).openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (
                    OutputStream output = connection.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
                ){
                    writer.append("list;");
                    writer.append(CRLF).flush();

                    String responseCode = ((HttpURLConnection) connection).getResponseMessage();
                    System.out.println("Response code: [" + responseCode + "]");
                }
            } catch (IOException ex) {
                Logger.getLogger(MyDropBox.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

