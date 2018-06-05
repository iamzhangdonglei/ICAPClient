/**
 *
 */
package com.trendmicro.icap.client;

/**
 * Command to send RESPMOD request to AV scan server.
 *
 * @author kraman
 *
 */
public class IcapRespmod {


    /** The ICAP message. */
    private final String icapMessage;


    /**
     *
     * <p>
     * <b>
     * Constructs a ICAP RESPMODE command.
     * </b>
     * </p>
     *
     * @param server
     * @param port
     * @param filename
     * @param serviceName
     * @param fileSize
     * @param previewSize
     */
    public IcapRespmod(final String server, final int port, final String filename, final String serviceName, final int fileSize, final int previewSize) {
        final StringBuffer buf = new StringBuffer();
        buf.append("RESPMOD icap://");
        buf.append(server);
        buf.append(":");
        buf.append(port);
        buf.append("/" + serviceName + " ICAP/1.0\r\n");
        buf.append("Host: ");
        buf.append(server);
        buf.append("\r\n");
        buf.append("Connection:  close\r\n");
        buf.append("User-Agent: IT-Kartellet ICAP Client/1.1\r\n");
        buf.append("Allow: 204\r\n");
        buf.append("Preview: ");
        buf.append(previewSize);
        buf.append("\r\n");

        // res header
        final StringBuffer resHdr = new StringBuffer();
        resHdr.append("GET ");
        resHdr.append("/" + filename);
        resHdr.append(" HTTP/1.1\r\n");
        resHdr.append("Host: ");
        resHdr.append(server);
        resHdr.append(":");
        resHdr.append(port);
        resHdr.append("\r\n");
        resHdr.append("\r\n");

        // res body
        final StringBuffer resBody = new StringBuffer();
        resBody.append("HTTP/1.1 200 OK");
        resBody.append("\r\n");
        resBody.append("Transfer-Encoding: chunked\r\n");
        resBody.append("Content-Length: " + fileSize + "\r\n");
        resBody.append("\r\n");

        int resHdrLen = resHdr.length();
        int resBodyLen = resHdrLen + resBody.length();

        // encapsulated header
        buf.append("Encapsulated: req-hdr=0, res-hdr=");
        buf.append(resHdrLen);
        buf.append(", res-body=");
        buf.append(resBodyLen);
        buf.append("\r\n");
        buf.append("\r\n");

        buf.append(resHdr);

        buf.append(resBody);

        this.icapMessage = buf.toString();
    }

    /**
     * Return the ICAP headers as string.
     *
     * @return ICAP RESPMOD message
     */
    public String getIcapMessage() {

        return this.icapMessage;
    }

    /**
     * Return the bytes to be scanned.
     *
     * @param inBuffer
     *
     * @return byte stream to be scanned
     */
    public byte[] getInStream(final byte[] inBuffer) {

        // TODO - avoid array copy???
        byte[] copiedStream = new byte[inBuffer.length + IcapRespmod.TRAILER_BYTES.length];
        System.arraycopy(inBuffer, 0, copiedStream, 0, inBuffer.length);
        System.arraycopy(IcapRespmod.TRAILER_BYTES, 0, copiedStream, inBuffer.length, IcapRespmod.TRAILER_BYTES.length);
        return copiedStream;
    }

    /** Bytes denoting the end of the message. */
    private static final byte[] TRAILER_BYTES = {'\r', '\n', '0', '\r', '\n'};

    /**
     * Returns the trailer bytes.
     *
     * @return byte stream denoting the endof the message.
     */
    public byte[] getTrailerBytes() {

        return IcapRespmod.TRAILER_BYTES;
    }

}
