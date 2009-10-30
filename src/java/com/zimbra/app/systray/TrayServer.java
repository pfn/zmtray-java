package com.zimbra.app.systray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.UUID;

import com.zimbra.app.systray.options.OptionsDialog;

public class TrayServer implements Runnable {
    
    private final static String OK_RESPONSE    = "OK";
    private final static String ERROR_RESPONSE = "ERROR";
    
    private final static String CHECK_REQUEST = "CHECK";
    
    private final static InetAddress LOCALHOST;
    
    private boolean started = false;
    
    private final Prefs prefs;
    private final ZimbraTray zmtray;
    
    private final HashMap<String,Command> commandMap =
            new HashMap<String,Command>();
    
    static {
        try {
            LOCALHOST = InetAddress.getByName(null);
        }
        catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        
    }

    public TrayServer(ZimbraTray zt) {
        zmtray = zt;
        prefs = Prefs.getPrefs();
        initializeCommands();
    }
    
    private void initializeCommands() {
        commandMap.put(CHECK_REQUEST, new Command() {
            public boolean execute(String arg) {
                zmtray.pollNow();
                OptionsDialog.showForm(zmtray);
                return true;
            }
        });
    }
    
    public void start() {
        if (started) return;
        
        Thread t = new Thread(this, "Tray Server");
        t.setDaemon(true);
        t.start();
        started = true;
    }
    
    public void run() {
        Prefs prefs = Prefs.getPrefs();
        ServerSocket socket = null;
        String ipcKey = UUID.randomUUID().toString();
        try {
            socket = new ServerSocket(0, 0, LOCALHOST);
            int port = socket.getLocalPort();
            prefs.setPort(port);
            prefs.setIPCKey(ipcKey);
            prefs.flush();
            
            while (true) {
                Socket s = socket.accept();
                new SocketProcessor(s, ipcKey);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
    
    public boolean checkIfRunning() {
        return OK_RESPONSE.equals(sendCommand(CHECK_REQUEST));
    }

    public String sendCommand(String command) {
        int port = prefs.getPort();
        if (port == -1)
            return ERROR_RESPONSE;
        String ipcKey = prefs.getIPCKey();
        if (ipcKey == null)
            return ERROR_RESPONSE;
        
        String response = null;
        Socket s = null;
        PrintWriter    out = null;
        BufferedReader in  = null;
        try {
            s = new Socket(LOCALHOST, port);
            out = new PrintWriter(s.getOutputStream());
            in  = new BufferedReader(
                    new InputStreamReader(s.getInputStream()));
            
            out.println(ipcKey + " " + command);
            out.flush();
            response = in.readLine();
        } catch (IOException e) {
            return ERROR_RESPONSE;
        }
        finally {
            if (out != null)
                out.close();
            try {
                if (in != null)
                    in.close();
            }
            catch (IOException e) { }
            try {
                if (s != null)
                    s.close();
            }
            catch (IOException e) { }
        }
        return response;
    }
    
    private class SocketProcessor implements Runnable {
        private Socket s;
        private String ipcKey;
        SocketProcessor(Socket socket, String key) {
            s = socket;
            ipcKey = key;
            Thread t = new Thread(
                    this, "SocketProcessor: " + s.getInetAddress());
            t.setDaemon(true);
            t.start();
        }
        public void run() {

            PrintWriter    out = null;
            BufferedReader in  = null;
            try {
                out = new PrintWriter(s.getOutputStream());
                in  = new BufferedReader(
                        new InputStreamReader(s.getInputStream()));
                
                String cmd = in.readLine();
                int idx = cmd.indexOf(" ");
                if (idx == -1) {
                    out.println(ERROR_RESPONSE);
                    return;
                }
                String key = cmd.substring(0, idx);
                if (!ipcKey.equals(key)) {
                    out.println(ERROR_RESPONSE);
                    return;
                }
                
                cmd = cmd.substring(idx + 1);
                Command c = commandMap.get(cmd);
                String response;
                if (c == null) {
                    response = ERROR_RESPONSE;
                } else {
                    response = c.execute(null) ? OK_RESPONSE : ERROR_RESPONSE;
                }
                out.println(response);
                out.flush();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            finally {
                if (out != null)
                    out.close();
                try {
                    if (in != null)
                        in.close();
                }
                catch (IOException e) { }
                try {
                    s.close();
                }
                catch (IOException e) { }
            }
        }
    }
    private interface Command {
        boolean execute(String arg);
    }
}
