package com.hengtiansoft.peg.byteagent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jialeirong on 2/1/2016.
 */
public class TransformerListener extends Thread{
    public static int DEFAULT_PORT = 9091;
    public static String DEFAULT_HOST = "localhost";
    private static TransformerListener theTransformListener = null;
    private static ServerSocket theServerSocket;
    private Retransformer retransformer;

    private TransformerListener(Retransformer retransformer)
    {
        this.retransformer = retransformer;
        setDaemon(true);
    }
    public static synchronized boolean initialize(Retransformer retransformer, String hostname, Integer port)
    {
        if (theTransformListener == null) {
            try {
                if (hostname == null) {
                    hostname = DEFAULT_HOST;
                }
                if (port == null) {
                    port = Integer.valueOf(DEFAULT_PORT);
                }
                theServerSocket = new ServerSocket();
                theServerSocket.bind(new InetSocketAddress(hostname, port.intValue()));
            } catch (IOException e) {
                System.out.println("TransformListener() : unexpected exception opening server socket " + e);
                e.printStackTrace();
                return false;
            }

            theTransformListener = new TransformerListener(retransformer);

            theTransformListener.start();
        }

        return true;
    }
    public static synchronized boolean terminate()
    {
        boolean enabled = true;
        try {
            if (theTransformListener != null) {
                try {
                    theServerSocket.close();
                } catch (IOException e) {
                    // ignore -- the thread should exit anyway
                }
                try {
                    theTransformListener.join();
                } catch (InterruptedException e) {
                    // ignore
                }
                theTransformListener = null;
                theServerSocket = null;
            }
            return true;
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    @Override
    public void run()
    {
        while (true) {
            if (theServerSocket.isClosed()) {
                return;
            }
            Socket socket = null;
            try {
                socket = theServerSocket.accept();
            } catch (IOException e) {
                if (!theServerSocket.isClosed()) {
                    System.out.println("TransformListener.run : exception from server socket accept " + e);
                    e.printStackTrace();
                }
                return;
            }
            try {
                handleConnection(socket);
            } catch (Exception e) {
                System.out.println("TransformListener() : error handling connection on port " + socket.getLocalPort());
                try {
                    socket.close();
                } catch (IOException e1) {
                    // do nothing
                }
            }
        }
    }

    /**
     * process the socket sent by remote client
     * @param socket
     */
    private void handleConnection(Socket socket)
    {
        InputStream is = null;
        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            // oops. cannot handle this
            System.out.println("TransformListener.run : error opening socket input stream " + e);
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                System.out.println("TransformListener.run : exception closing socket after failed input stream open" + e1);
                e1.printStackTrace();
            }
            return;
        }

        OutputStream os = null;
        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            // oops. cannot handle this
            System.out.println("TransformListener.run : error opening socket output stream " + e);
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                System.out.println("TransformListener.run : exception closing socket after failed output stream open" + e1);
                e1.printStackTrace();
            }
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(os));

        String line = null;
        try {
            // get the first line
            line = in.readLine();
        } catch (IOException e) {
            System.out.println("TransformListener.run : exception " + e + " while reading command");
            e.printStackTrace();
        }
        try {
            if (line == null) {
                out.println("ERROR");
                out.println("Expecting input command");
                out.println("OK");
            } else if (line.equals("LOAD")) {
                // TODO debug whether communication is sufficient
                loadRules(in, out);
            } else if (line.equals("DELETE")) {
                deleteRules(in, out);
            } else if (line.equals("LIST")) {
                listRules(in, out);
            } else if (line.equals("DELETEALL")) {
                purgeRules(in, out);
            } else {
                out.println("ERROR");
                out.println("Unexpected command " + line);
                out.println("OK");
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("TransformListener.run : exception " + e + " processing command " + line);
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                System.out.println("TransformListener.run : exception closing socket " + e1);
                e.printStackTrace();
            }
        }
    }

    private void listRules(BufferedReader in, PrintWriter out) throws Exception
    {
        retransformer.listAllRules(out);
        out.println("OK");
        out.flush();
    }
    private void purgeRules(BufferedReader in, PrintWriter out) throws Exception
    {
        retransformer.removeAllScripts(out);
        out.println("OK");
        out.flush();
    }
    private void loadRules(BufferedReader in, PrintWriter out) throws IOException
    {
        handleRules(in, out, false);
    }
    private void deleteRules(BufferedReader in, PrintWriter out) throws IOException
    {
        handleRules(in, out, true);
    }
    private void handleRules(BufferedReader in, PrintWriter out, boolean doDelete) throws IOException
    {
        List<String> rules = new ArrayList<String>();
        String rule = in.readLine().trim();
        while (!rule.equals("ENDLOAD") &&  !rule.equals("ENDDELETE"))
        {
            rules.add(rule);
            rule = in.readLine().trim();
        }
        try {
            if (doDelete) {
                retransformer.removeScripts(rules, out);
            } else {
                retransformer.installScript(rules, out);
            }
        } catch (Exception e) {
            out.append("EXCEPTION ");
            out.append(e.toString());
            out.append('\n');
            e.printStackTrace(out);
        }
        out.println("OK");
        out.flush();
    }
}
