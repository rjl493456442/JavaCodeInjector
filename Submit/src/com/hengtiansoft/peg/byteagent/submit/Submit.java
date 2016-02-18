package com.hengtiansoft.peg.byteagent.submit;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by jialeirong on 2/2/2016.
 */
public class Submit {
    public static final String DEFAULT_ADDRESS = "localhost";
    public static final int DEFAULT_PORT= 9091;

    private final int port;
    private final String address;

    private PrintStream out;
    public Submit(String address, int port, PrintStream out) {
        if (address == null) {
            address = DEFAULT_ADDRESS;
        }

        if (port <= 0) {
            port = DEFAULT_PORT;
        }

        if (out == null) {
            out = System.out;
        }

        this.address = address;
        this.port = port;
        this.out = out;
    }
    public static void main(String[] args)
    {
        String outfile = null;
        int port = DEFAULT_PORT;
        String hostname = DEFAULT_ADDRESS;
        int startIdx = 0;
        int maxIdx = args.length;
        boolean deleteRules = false;
        int optionCount = 0;
        PrintStream out = System.out;

        while (startIdx < maxIdx && args[startIdx].startsWith("-")) {
            if (maxIdx >= startIdx + 2 && args[startIdx].equals("-o")) {
                outfile = args[startIdx+1];
                File file =  new File(outfile);
                if (file.exists()) {
                    // open for append
                    if (file.isDirectory() || !file.canWrite()) {
                        out.println("Submit : invalid output file " + outfile);
                        System.exit(1);
                    }
                    FileOutputStream fos = null;
                    try {
                        fos =  new FileOutputStream(file, true);
                    } catch (FileNotFoundException e) {
                        out.println("Submit : error opening output file " + outfile);
                    }
                    out = new PrintStream(fos);
                } else {
                    FileOutputStream fos = null;
                    try {
                        fos =  new FileOutputStream(file, true);
                    } catch (FileNotFoundException e) {
                        out.println("Submit : error opening output file " + outfile);
                    }
                    out = new PrintStream(fos);
                }
                startIdx += 2;
            } else if (maxIdx >= startIdx + 2 && args[startIdx].equals("-p")) {
                try {
                    port = Integer.valueOf(args[startIdx+1]);
                } catch (NumberFormatException e) {
                    out.println("Submit : invalid port " + args[startIdx+1]);
                    System.exit(1);
                }
                if (port <= 0) {
                    out.println("Submit : invalid port " + args[startIdx+1]);
                    System.exit(1);
                }
                startIdx += 2;
            } else if (maxIdx >= startIdx + 2 && args[startIdx].equals("-h")) {
                hostname = args[startIdx+1];
                startIdx += 2;
            } else if (args[startIdx].equals("-u")) {
                deleteRules = true;
                startIdx++;
                optionCount++;
            } else if (args[startIdx].equals("-l")) {
                startIdx++;
                optionCount++;
            } else {
                break;
            }
        }

        // not support delete rule and load rule together
        if (startIdx < maxIdx && args[startIdx].startsWith("-") || optionCount > 1) {
            usage(out, 1);
        }
        Submit client = new Submit(hostname, port, out);
        String results = null;
        List<String> argsList = null;

        try {
            if (startIdx == maxIdx) {
                // no args means list or delete all current scripts
                if (deleteRules) {
                    results = client.deleteAllRules();
                }  else {
                    // the default behavior (or if -l was explicitly specified) is to do this
                    results = client.listAllRules();
                }
            } else {
                argsList = new ArrayList<String>();
                for (int i = startIdx; i < maxIdx; i++) {
                    argsList.add(args[i]);
                }
                if (deleteRules) {
                    results = client.deleteRules(argsList);
                } else {
                    // the default behavior (or if -l was explicitly specified) is to do this\
                    System.err.println("add rule: " + argsList);
                    results = client.addRules(argsList);
                }
            }
        } catch (Exception e) {
            out.println("Failed to process request: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        out.println(results);
        if (out != System.out) {
            out.close();
        }
    }

    /**
     *  send request to agent delete all rule loaded
     * @return
     * @throws Exception
     */
    public String deleteAllRules() throws Exception {
        return submitRequest("DELETEALL\n");
    }

    /**
     *  send request to agent list all rule loaded
     * @return
     * @throws Exception
     */
    public String listAllRules() throws Exception {
        return submitRequest("LIST\n");
    }

    /**
     *  send request to agent to load specific rule
     * @param rules
     * @return
     * @throws Exception
     */
    public String addRules(List<String> rules) throws Exception {
        if (rules == null || rules.size() == 0) {
            return "";
        }

        StringBuilder str = new StringBuilder("LOAD\n");
        for (String rule : rules) {
            str.append(rule + '\n');
        }
        str.append("ENDLOAD\n");
        return submitRequest(str.toString());
    }

    /**
     * send request to agent to delete specific rule
     * @param rules
     * @return
     * @throws Exception
     */
    public String deleteRules(List<String> rules) throws Exception {
        if (rules == null || rules.size() == 0) {
            return "";
        }

        StringBuilder str = new StringBuilder("DELETE\n");
        for (String rule : rules) {
            str.append(rule + '\n');
        }
        str.append("ENDDELETE\n");

        return submitRequest(str.toString());
    }

    /**
     * communicate with remote agent via socket
     * @param request
     * @return
     * @throws Exception
     */
    public String submitRequest(String request) {
        Comm comm = null;
        try {
            comm = new Comm(this.address, this.port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // important
            // using print otherwise println
            comm.print(request);
            String results = comm.readResponse();
            return results;
        } catch (Exception e)
        {
            System.err.println("ERROR response");
            e.printStackTrace();
        }
        finally {
            comm.close();
        }
    }

    /**
     * communication class
     */
    private class Comm {
        private Socket commSocket;
        private BufferedReader commInput;
        private PrintWriter commOutput;

        public Comm(String address, int port) throws Exception {
            this.commSocket = new Socket(address, port);

            InputStream is;
            try {
                is = this.commSocket.getInputStream();
            } catch (Exception e) {
                // oops. cannot handle this
                try {
                    this.commSocket.close();
                } catch (Exception e1) {
                }
                throw e;
            }

            OutputStream os;
            try {
                os = this.commSocket.getOutputStream();
            } catch (Exception e) {
                // oops. cannot handle this
                try {
                    this.commSocket.close();
                } catch (Exception e1) {
                }
                throw e;
            }

            this.commInput = new BufferedReader(new InputStreamReader(is));
            this.commOutput = new PrintWriter(new OutputStreamWriter(os));
        }

        public void close() {
            try {
                this.commSocket.close(); // also closes the in/out streams
            } catch (Exception e) {
                // TODO what should I do here? no need to abort, we are closing this object anyway
            } finally {
                // this object cannot be reused anymore, therefore, null everything out
                // which will force NPEs if attempts to reuse this object occur later
                this.commSocket = null;
                this.commInput = null;
                this.commOutput = null;
            }
        }

        public void println(String line) {
            this.commOutput.println(line);
            this.commOutput.flush();
        }

        public void print(String line) {
            this.commOutput.print(line);
            this.commOutput.flush();
        }

        /**
         * get response from the remote agent
         * @return
         * @throws Exception
         */
        public String readResponse() throws Exception {
            StringBuilder str = new StringBuilder();
            StringBuilder errorStr = null; // will be non-null if an error was reported by the agent

            String line = this.commInput.readLine();
            // OK is the end flag of a communication
            while (line != null && !line.trim().equals("OK")) {
                line = line.trim();

                if (line.startsWith("ERROR") || line.startsWith("EXCEPTION")) {
                    if (errorStr == null) {
                        errorStr = new StringBuilder();
                    }
                }

                // if an error was detected, gobble up the text coming over the wire as part of the error message
                if (errorStr != null) {
                    errorStr.append(line).append('\n');
                }

                str.append(line).append('\n');
                line = this.commInput.readLine();
            }

            if (errorStr != null) {
                StringBuilder msg = new StringBuilder();
                msg.append("The remote agent reported an error:\n").append(errorStr);
                if (!errorStr.toString().equals(str.toString())) {
                    msg.append("\nThe full response received from the agent follows:\n").append(str);
                }
                throw new Exception(msg.toString());
            }
            return str.toString();
        }
    }
    /**
     * print usage to out stream
     * @param out
     * @param exitCode
     */
    private static void usage(PrintStream out, int exitCode)
    {
        out.println("usage : Submit [-o outfile] [-p port] [-h hostname] [-l|-u] [scriptfile . . .]");
        out.println("        -o redirects output from System.out to outfile");
        out.println("        -p specifies listener port");
        out.println("        -h specifies listener host");
        out.println("        -l (default) with scriptfile(s) means load/reload all rules in scriptfile(s)");
        out.println("                     with no scriptfile means list all currently loaded rules");
        out.println("        -u with scriptfile(s) means unload all rules in scriptfile(s)");
        out.println("           with no scriptfile means unload all currently loaded rules");
        if (out != System.out) {
            out.close();
        }
        System.exit(exitCode);
    }
}
