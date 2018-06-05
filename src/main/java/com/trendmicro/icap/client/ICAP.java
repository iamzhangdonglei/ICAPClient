package com.trendmicro.icap.client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ICAP {

    private static final Charset StandardCharsetsUTF8 = Charset.forName("UTF-8");

    private Socket client = null;

    private DataOutputStream out;

    private DataInputStream in;

    private final String VERSION = "1.0";

    private final String USERAGENT = "IT-Kartellet ICAP Client/1.1";

    private final String ICAPTERMINATOR = "\r\n\r\n";

    private final String HTTPTERMINATOR = "0\r\n\r\n";

    private int stdPreviewSize = 4096;

    private final int stdRecieveLength = 8192;

    private final int stdSendLength = 8192;

    private String icapService;

    private Logger logger = Logger.getLogger(ICAP.class);


    /**
     * Initializes the socket connection and IO streams. It asks the server for
     * the available options and changes settings to match it.
     *
     * @param s
     *            The IP address to connect to.
     * @param p
     *            The port in the host to use.
     * @param icapService
     *            The service to use (fx "avscan").
     * @throws IOException
     * @throws ICAPException
     */
    public ICAP(final String serverIP, final int port, final String icapService) throws IOException, ICAPException {
        // Initialize connection
        initICAPClient(serverIP, port, icapService);

    }

    private void initICAPClient(final String serverIP, final int port, final String icapService) throws IOException, ICAPException {

        this.logger.debug("initICAPClient start");
        this.icapService = icapService;
        if ((this.client = new Socket(serverIP, port)) == null) {
            throw new ICAPException("Could not open socket connection[server:" + serverIP + ",port:" + port + "]");
        }
        // Openening out stream
        OutputStream outToServer = this.client.getOutputStream();
        this.out = new DataOutputStream(outToServer);
        // Openening in stream
        InputStream inFromServer = this.client.getInputStream();
        this.in = new DataInputStream(inFromServer);
        String parseMe = getOptions(serverIP, icapService);
        Map<String, String> responseMap = parseHeader(parseMe);
        if (responseMap.get("StatusCode") != null) {
            int status = Integer.parseInt(responseMap.get("StatusCode"));
            switch (status) {
            case 200:
                String tempString = responseMap.get("Preview");
                try {
                    this.stdPreviewSize = Integer.parseInt(tempString);
                } catch (Exception e) {
                    this.logger.info("Could not get preview size from server");
                }
                break;
            default:
                throw new ICAPException("Could not get preview size from server");
            }
        } else {
            throw new ICAPException("Could not get options from server");
        }
    }


    public static void main(final String[] args) {

        try {
            System.setProperty("user.dir", "E:/chinasystems/Projects/BankMandiri/BM_Parameter/EE_PARA");
            // System.setProperty("CURRENT_RUNNING_MODE", "JUNIT");
            // EECacheHook.getInstance().start();
            // try {
            // ClusteringCacheManager.getInstance();
            // } catch (Exception e1) {
            // WebLogExec.writeEEError(e1);
            // }
            File fnew = new File("D:\\axis.log");
            // BufferedImage originalImage = ImageIO.read(fnew);
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // ImageIO.write(originalImage, "png", baos);
            // byte[] imageInByte = baos.toByteArray();
            fnew = new File("c:\\test.png");
            byte[] imageInByte = IOUtils.toByteArray(new FileInputStream(fnew));

            ICAP icap = new ICAP("10.39.201.63", 1344, "av/respmod");
            // boolean test = icap.scanByte(imageInByte);
            boolean test = icap.scanFileByByte(imageInByte, "c:\\test.png");
            System.out.println(test);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ICAPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Given a filepath, it will send the file to the server and return true,
     * if the server accepts the file. Visa-versa, false if the server rejects
     * it.
     *
     * @param filename
     *
     * @param filename
     *            Relative or absolute filepath to a file.
     * @return Returns true when no infection is found.
     */
    private boolean scanFileByByteStream(final byte[] fileContent, final String filename) throws IOException, ICAPException {

        this.logger.info("scanFileByByteStream method start.");
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(fileContent);
            int fileSize = fileContent.length;
            // First part of header
            // String resBody = "Content-Length: " + fileSize + "\r\n\r\n";
            boolean hasNextPart = true;
            int previewSize = this.stdPreviewSize;
            if (fileSize <= this.stdPreviewSize) {
                previewSize = fileSize;
                hasNextPart = false;
            }
            // String requestBuffer = buildRequestBuffer( previewSize);
            // sendString(requestBuffer);

            final IcapRespmod scanReq = new IcapRespmod(this.client.getInetAddress().getHostAddress(), this.client.getPort(), filename, this.icapService, fileSize, previewSize);
            this.logger.debug("Send ICAP RESPMOD Request:\r\n" + scanReq.getIcapMessage());
            sendString(scanReq.getIcapMessage());
            // Sending preview or, if smaller than previewSize, the whole file.

            byte[] chunk = new byte[previewSize];
            inputStream.read(chunk);
            // System.arraycopy(fileContent,0,chunk,0,previewSize);
            sendString(Integer.toHexString(previewSize) + "\r\n");
            sendBytes(chunk);

            // String sleeptime =
            // CommServiceUtils.getCSServicePara("VIRUSSCAN_NEEDSLEEP",
            // cntyCode, WebServiceConstant.SERVICE_VIRUSSCAN);
            //
            // if (StringUtil.isTrimNotEmpty(sleeptime)) {
            // try {
            // Thread.sleep(Long.valueOf(sleeptime));
            // } catch (InterruptedException e) {
            // throw new ICAPException("failed sent the request to ICAP
            // server");
            // }
            // }
            sendString("\r\n");
            if (!hasNextPart) {
                sendString("0; ieof\r\n\r\n");
            } else {
                sendString("0\r\n\r\n");
            }

            // Parse the response! It might not be "100 continue"
            // if fileSize<previewSize, then this is acutally the respond
            // otherwise it is a "go" for the rest of the file.
            Map<String, String> responseMap = new HashMap<String, String>();
            int status;
            if (hasNextPart) {
                String parseMe = getHeader(this.ICAPTERMINATOR);
                responseMap = parseHeader(parseMe);
                String tempString = responseMap.get("StatusCode");
                if (tempString != null) {
                    status = Integer.parseInt(tempString);
                    // this.logger.info("scanFileByByteStream:" + status + "
                    // Whether or not Continue transfer");
                    switch (status) {
                    case 100:
                        break; // Continue transfer
                    case 200:
                        return false;
                    case 204:
                        return true;
                    case 404:
                        throw new ICAPException("404: ICAP Service not found");
                    default:
                        throw new ICAPException("Server returned unknown status code:" + status);
                    }
                }
                // Sending remaining part of file
                byte[] buffer = new byte[this.stdSendLength];
                // InputStream fileInStream = new
                // ByteArrayInputStream(fileContent);
                while ((inputStream.read(buffer)) != -1) {
                    sendString(Integer.toHexString(buffer.length) + "\r\n");
                    sendBytes(buffer);
                    sendString("\r\n");
                }
                // Closing file transfer.
                sendString("0\r\n\r\n");
            }
            // fileInStream.close();
            responseMap.clear();
            String response = getHeader(this.ICAPTERMINATOR);
            responseMap = parseHeader(response);
            String tempString = responseMap.get("StatusCode");
            if (tempString != null) {
                status = Integer.parseInt(tempString);
                // this.logger.info("The ICAP status is " + status);
                if (status == 204) {
                    return true;
                } // Unmodified
                if (status == 200) {
                    // OK - The ICAP status is ok, but the encapsulated HTTP
                    // status will likely
                    // be different
                    // response = getHeader(this.HTTPTERMINATOR);
                    // int x = response.indexOf("<title>", 0);
                    // int y = response.indexOf("</title>", x);
                    // String statusCode = response.substring(x + 7, y);
                    //
                    // if (statusCode.equals("ProxyAV: Access Denied")) {
                    // return false;
                    // }
                    return false;
                }
                throw new ICAPException("Unrecognized status code in response header:" + status);
            }
            throw new ICAPException("no status code in response header.");
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public boolean scanFileByByte(final byte[] fileContent, final String filename) throws IOException, ICAPException {

        try {
            return scanFileByByteStream(fileContent, filename);
        } catch (IOException ioe) {
            throw ioe;
        } catch (ICAPException icape) {
            throw icape;
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {

        try {
            this.disconnect();
            if (this.out != null) {
                this.out.close();
            }
            if (this.in != null) {
                this.in.close();
            }
        } catch (Exception e) {
            this.client = null;
            this.out = null;
            this.in = null;
        }
    }

    /**
     * Automatically asks for the servers available options and returns the raw
     * response as a String.
     *
     * @return String of the servers response.
     * @throws IOException
     * @throws ICAPException
     */
    private String getOptions(final String serverIP, final String icapService) throws IOException, ICAPException {

        // Send OPTIONS header and receive response
        // Sending and recieving
        StringBuilder sbHeader = new StringBuilder();
        sbHeader.append("OPTIONS icap://").append(serverIP).append("/").append(icapService);
        sbHeader.append(" ICAP/").append(this.VERSION).append("\r\nHost: ").append(serverIP).append("\r\nUser-Agent: ");
        sbHeader.append(this.USERAGENT).append("\r\nEncapsulated: null-body=0\r\n\r\n");
        String optionReq = sbHeader.toString();
        this.logger.debug("Send ICAP OPTIONS Request:\r\n" + optionReq);
        sendString(optionReq);
        return getHeader(this.ICAPTERMINATOR);
    }

    /**
     * Receive an expected ICAP header as response of a request. The returned
     * String should be parsed with parseHeader()
     *
     * @param terminator
     * @return String of the raw response
     * @throws IOException
     * @throws ICAPException
     */
    private String getHeader(final String terminator) throws IOException, ICAPException {

        byte[] endofheader = terminator.getBytes(ICAP.StandardCharsetsUTF8);
        byte[] buffer = new byte[this.stdRecieveLength];

        int n;
        int offset = 0;
        // stdRecieveLength-offset is replaced by '1' to not receive the next
        // (HTTP) header.
        while ((offset < this.stdRecieveLength) && ((n = this.in.read(buffer, offset, 1)) != -1)) {
            // first part is to secure against DOS
            offset += n;
            if (offset > endofheader.length + 13) {
                // 13 is the smallest possible message ICAP/1.0 xxx
                byte[] lastBytes = Arrays.copyOfRange(buffer, offset - endofheader.length, offset);
                if (Arrays.equals(endofheader, lastBytes)) {
                    String resp = new String(buffer, 0, offset, ICAP.StandardCharsetsUTF8);
                    this.logger.debug("Response from ICAP server:\r\n" + resp);
                    // System.out.println("Response from ICAP server:\r\n" + resp);
                    return resp;
                }
            }
        }
        throw new ICAPException("Error in getHeader() method");
    }

    /**
     * Given a raw response header as a String, it will parse through it and
     * return a HashMap of the result
     *
     * @param response
     *            A raw response header as a String.
     * @return HashMap of the key,value pairs of the response
     */
    private Map<String, String> parseHeader(final String response) {

        Map<String, String> headers = new HashMap<String, String>();

        /****
         * SAMPLE:**** ICAP/1.0 204 Unmodified Server: C-ICAP/0.1.6 Connection:
         * keep-alive ISTag: CI0001-000-0978-6918203
         */
        // The status code is located between the first 2 whitespaces.
        // Read status code
        int x = response.indexOf(" ", 0);
        int y = response.indexOf(" ", x + 1);
        String statusCode = response.substring(x + 1, y);
        headers.put("StatusCode", statusCode);

        // Each line in the sample is ended with "\r\n".
        // When (i+2==response.length()) The end of the header have been
        // reached.
        // The +=2 is added to skip the "\r\n".
        // Read headers
        int i = response.indexOf("\r\n", y);
        i += 2;
        while (i + 2 != response.length() && response.substring(i).contains(":")) {

            int n = response.indexOf(":", i);
            String key = response.substring(i, n);

            n += 2;
            i = response.indexOf("\r\n", n);
            String value = response.substring(n, i);

            headers.put(key, value);
            i += 2;
        }

        return headers;
    }

    /**
     * Sends a String through the socket connection. Used for sending ICAP/HTTP
     * headers.
     *
     * @param requestHeader
     * @throws IOException
     */
    private void sendString(final String requestHeader) throws IOException {

        this.out.write(requestHeader.getBytes(ICAP.StandardCharsetsUTF8));
    }

    /**
     * Sends bytes of data from a byte-array through the socket connection.
     * Used to send filedata.
     *
     * @param chunk
     *            The byte-array to send.
     * @throws IOException
     */
    private void sendBytes(final byte[] chunk) throws IOException {

        for (int i = 0; i < chunk.length; i++) {
            this.out.write(chunk[i]);
        }
    }

    /**
     * Terminates the socket connecting to the ICAP server.
     *
     * @throws IOException
     */
    private void disconnect() throws IOException {

        if (this.client != null) {
            this.client.close();
            this.client = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {

        try {
            disconnect();
        } finally {
            super.finalize();
        }
    }
}
