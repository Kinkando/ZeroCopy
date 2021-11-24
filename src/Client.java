import java.io.*;
import java.nio.channels.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private SocketChannel socketChannel;
    private final String folder = "C:/Users/User/Downloads/Documents/Operating Systems/Client Folder/";
    private final String IpAddress = "192.168.56.1";
    private final int port = 3301;
    private final int portChannel = 3302;
    private final String[] fileList;

    public Client() {
        connection();
        fileList = getFileList();
        requestServer();
    }

    public final void connection() {
        try {
            socket = new Socket(IpAddress, port);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
            socketChannel = SocketChannel.open(new InetSocketAddress(IpAddress, portChannel));
        } catch (IOException e) {}
    }

    public final String[] getFileList() {
        try {
            String read, file = "";
            while (!(read = fromServer.readUTF()).equalsIgnoreCase("/EOF"))
                file += read + "/";
            return file.split("/");
        } catch (IOException e) {
            disconnect();
            return null;
        }
    }

    public final void printFile() {
        for (int i = 0; i < fileList.length; i++) 
            System.out.println("[" + (i + 1) + "] " + fileList[i]);
    }

    public final void requestServer() {
        printFile();
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.print("which file do you want to download => ");
            String request = scan.next();
            if(request.equalsIgnoreCase("EXIT"))
                break;
            try {
                int index = Integer.parseInt(request) - 1;
                if (index < 0 || index >= fileList.length) {
                    System.out.println("Invalid file no.\n");
                    continue;
                }
                System.out.print("1.Copy\n2.Zero Copy\nSelect type to copy => ");
                String type = scan.next();
                if (!type.equals("1") && !type.equals("2")) {
                    System.out.println("Invalid type select\n");
                    printFile();
                    continue;
                }
                toServer.writeInt(index);
                toServer.writeUTF(type);
                long size = fromServer.readLong();
                String filePath = folder + fileList[index];
                long start = System.currentTimeMillis();
                if(type.equals("1"))
                    copy(filePath, size);
                else
                    zeroCopy(filePath, size);
                long end = System.currentTimeMillis();
                long timeElaspe = end-start;
                System.out.println("Time Elaspe: "+timeElaspe+" ms\n");
                printFile();
            } catch (NumberFormatException e) {
                System.out.println("Invalid input\n");
            } catch (IOException ex) {}
        }
    }
    
    public void copy(String filePath, long size) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filePath);
            byte[] buffer = new byte[1024];
            int read;
            long currentRead = 0;
            while (currentRead < size && (read = fromServer.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                currentRead += read;
            }
            System.out.println("Copy Success");
        } catch (IOException e) { }
        finally {
            try {
                if (fos != null) 
                    fos.close();
            } catch (IOException e) {
                disconnect();
            }
        }
    }
    
    public final void zeroCopy(String filePath, long size){
        FileChannel destination = null;
        try{
            destination = new FileOutputStream(filePath).getChannel();
            long currentRead = 0;
            long read;
            while(currentRead < size && (read = destination.transferFrom(socketChannel, currentRead, size - currentRead)) != -1)
                currentRead += read;
            System.out.println("Zero Copy Success");
        } catch (IOException e){}
        finally{
            try{
                if(destination != null)
                    destination.close();
            } catch (IOException e){
                disconnect();
            }
        }
    }
    
    public void disconnect(){
        try{
            if(fromServer != null)
                fromServer.close();
            if(toServer != null)
                toServer.close();
            if(socket != null)
                socket.close();
            if(socketChannel != null)
                socketChannel.close();
        } catch (IOException e){ }
    }
    
    public static void main(String[] args) {
        Client client = new Client();
    }
}