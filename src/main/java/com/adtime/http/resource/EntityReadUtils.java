package com.adtime.http.resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.TruncatedChunkException;
import org.apache.http.util.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

public class EntityReadUtils {

    private static final Logger logger = LoggerFactory.getLogger(EntityReadUtils.class);

    //页面大小最大为256K
    private static final int MAX_PAGE_SIZE = 1024 * 1024;
    private static final int MAX_DOWNLOAD_LIMIT = 5 * 1024 * 1024;

    private static boolean isGzip(String conEncoding) {
        return null != conEncoding && conEncoding.equalsIgnoreCase("gzip");
    }

    private static boolean isDeflate(String conEncoding) {
        return null != conEncoding && conEncoding.equalsIgnoreCase("deflate");
    }

    public static Entity read(final HttpEntity entity, final String charSet, boolean checkBodySize) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException("HttpEntity may not be null");
        }
        Header header = entity.getContentEncoding();
        String conEncoding = null != header ? header.getValue() : null;
        return streamAsByte(entity.getContentLength(), charSet, entity.getContent(), isGzip(conEncoding), isDeflate(conEncoding), checkBodySize);
    }

    public static Entity read(final HttpURLConnection con, final boolean error, final String charSet, boolean checkBodySize) throws IOException {
        if (con == null) {
            throw new IllegalArgumentException("HttpURLConnection may not be null");
        }
        String conEncoding = con.getContentEncoding();
        InputStream is, _is;
        if (error) {
            _is = con.getErrorStream();
        } else {
            _is = con.getInputStream();
        }
        is = _is;
        boolean checkInflaterInputStream = false, unCompress = false;
        if (isGzip(conEncoding) && !(_is instanceof GZIPInputStream)) {
            unCompress = true;
        } else if (isDeflate(conEncoding)) {
            checkInflaterInputStream = true;
        }

        return streamAsByte(con.getContentLengthLong(), charSet, is, unCompress, checkInflaterInputStream, checkBodySize);
    }

    private static String contentLength(long pre, int cl) {
        long length = pre > cl ? pre : cl;
        long v = length / 1024l;
        int s = 0;
        while (v > 1024) {
            s++;
            if (s > 2) {
                break;
            }
            v = v / 1024l;
        }
        switch (s) {
            case 0:
                return v + "k";
            case 1:
                return v + "m";
            case 2:
                return v + "g";
            default:
                return v + "t";

        }
    }

    @SuppressWarnings("all")
    private static Entity streamAsByte(long contentLength, String charSet, InputStream is, boolean uncompress, boolean checkInflaterInputStream, boolean checkBodySize) throws IOException {

        if (is == null) {
            return new Entity(false, "Http InputStream is null!!");
        }

        if (checkBodySize && contentLength > MAX_PAGE_SIZE) {
            return new Entity(false, "流大小:[" + contentLength(contentLength, 0) + "] max than [" + MAX_PAGE_SIZE + "] as a download Stream");
        }

        ByteArrayBuffer byteArrayBuffer = null;
        int contentLengthCurrent = 0;
        String warningMsg = null;
        try {
            try {
                byte[] tmp = new byte[4096];
                int l;
                boolean isPageSizeOut = false;
                int i = (int) contentLength;
                if (i < 0) {
                    i = 4096;
                }
                byteArrayBuffer = new ByteArrayBuffer(i);

                while (true) {
                    if ((l = is.read(tmp)) != -1) {
                        if (!isPageSizeOut) {
                            byteArrayBuffer.append(tmp, 0, l);
                        }

                        contentLengthCurrent += l;
                        if (checkBodySize) {
                            if (contentLengthCurrent > MAX_PAGE_SIZE && !isPageSizeOut) {
                                isPageSizeOut = true;
                            }
                            if (contentLengthCurrent > MAX_DOWNLOAD_LIMIT) {
                                return new Entity(false, "流大小:[" + contentLength(contentLength, contentLengthCurrent) + "] max than [" + MAX_DOWNLOAD_LIMIT + "] as a download Stream");
                            }
                        }
                    } else {
                        break;
                    }
                }

                if (isPageSizeOut) {
                    return new Entity(false, "流大小:[" + contentLength(contentLength, contentLengthCurrent) + "] max than [" + MAX_PAGE_SIZE + "] as a not regular html page");
                }


            } catch (Exception e) {
                if (e instanceof EOFException) {
                    logger.warn("页面流 EOF");
                } else if (e instanceof SocketException) {
                    if (e.getMessage() != null && e.getMessage().contains("Software caused connection abort: recv failed")) {
                        logger.warn("[connection abort recv failed]数据读取可能不完整！！！！");
                    } else {
                        e.printStackTrace();
                        return new Entity(false, e.toString());
                    }
                } else if (e instanceof TruncatedChunkException) {
                    logger.warn("[TruncatedChunkException]数据读取可能不完整！！！！");
                } else {
                    e.printStackTrace();
                    return new Entity(false, e.toString());
                }
            }

            byte[] bytes = byteArrayBuffer.buffer();
            if (checkInflaterInputStream) {
                bytes = tranInflaterInputStream(byteArrayBuffer.toByteArray());
            } else if (uncompress) {
                bytes = uncompressGzip(byteArrayBuffer.toByteArray());
            }
            return getEntity(charSet, contentLengthCurrent, bytes, warningMsg);
        } finally {
            try {
                is.close();
            } catch (Exception ignore) {
            }

            try {
                if (null != byteArrayBuffer)
                    byteArrayBuffer.clear();
            } catch (Exception ignore) {
            }
        }
    }

    private static boolean isZlibHeader(byte[] bytes) {
        //deal with java stupidity : convert to signed int before comparison
        char byte1 = (char) (bytes[0] & 0xFF);
        char byte2 = (char) (bytes[1] & 0xFF);

        return byte1 == 0x78 && (byte2 == 0x01 || byte2 == 0x9c || byte2 == 0xDA);
    }

    private static byte[] tranInflaterInputStream(byte[] encBytes) throws IOException {
        Inflater inflator = new Inflater(true);
        boolean isZlibHeader = isZlibHeader(encBytes);
        inflator.setInput(encBytes, isZlibHeader ? 2 : 0, isZlibHeader ? encBytes.length - 2 : encBytes.length);
        byte[] buf = new byte[4096];
        int nbytes = 0;
        ByteArrayBuffer byteArrayBuffer = new ByteArrayBuffer(isZlibHeader ? encBytes.length - 4 : encBytes.length);
        do {
            try {
                nbytes = inflator.inflate(buf);
                if (nbytes > 0) {
                    byteArrayBuffer.append(buf, 0, nbytes);
                }
            } catch (DataFormatException e) {
                //handle error
            }
        } while (nbytes > 0);
        inflator.end();
        return byteArrayBuffer.buffer();
    }


    public static byte[] uncompressGzip(byte[] b) {
        if (b == null || b.length == 0) {
            return null;
        }
        ByteArrayBuffer byteArrayBuffer = new ByteArrayBuffer(b.length);
        ByteArrayInputStream in = new ByteArrayInputStream(b);

        try {
            GZIPInputStream gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                byteArrayBuffer.append(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return byteArrayBuffer.buffer();
    }

    private static Entity getEntity(String charSet, long contentLength, byte[] bytes, String warningMsg) {
        Entity entity = new Entity(bytes, charSet);
        entity.length = contentLength;
        entity.warningMsg = warningMsg;
        return entity;
    }


    public static class Entity {

        private byte[] bytes;
        private boolean valid;
        private String msg;
        private String warningMsg;
        private String charSet;
        private String finalCharSet;
        private long length;
        private long unCompressLength;
        private String content;
        private boolean hasParse = false;

        public Entity(boolean valid, String msg) {
            this.valid = valid;
            this.msg = msg;
        }

        public Entity(byte[] bytes, String charSet) {
            this.bytes = bytes;
            this.valid = true;
            this.charSet = charSet;
            this.unCompressLength = bytes.length;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMsg() {
            return msg;
        }

        public String getFinalCharSet() {
            return finalCharSet;
        }

        public long getLength() {
            return length;
        }

        public long getUnCompressLength() {
            return unCompressLength;
        }

        public String toString(String url) throws Exception {
            if (!hasParse) {
                try {
                    if (valid) {
                        if (StringUtils.isBlank(this.charSet)) {
                            CharsetDetector.CharsetInfo charsetInfo = CharsetDetector.getCharSet(bytes, this.charSet);
                            String charSet = charsetInfo.getCharset();
                            String[] proCharSet = charsetInfo.getPropCharset();
                            String value = new String(bytes, charSet).trim();
                            finalCharSet = charSet;
                            if (charSet.toLowerCase().contains("ascii")) {
                                value = UnicodeUtil.unicode2string(value);
                            }
                            this.content = value;
                            if (logger.isInfoEnabled()) {
                                if (null != warningMsg) {
                                    logger.info("数据解码可能的编码有{} UseCharSet: [{}] [{}] Url:{} ", proCharSet, charSet, warningMsg, url);
                                } else {
                                    logger.info("数据解码可能的编码有{} UseCharSet: [{}] Url:{} ", proCharSet, charSet, url);
                                }
                            }
                        } else {
                            this.content = new String(bytes, charSet).trim();
                        }
                        return this.content;
                    } else {
                        String ret = String.format("数据不合法,不能解码 url:%s msg:%s", url, msg);
                        throw new Exception(ret);
                    }
                } finally {
                    destroy();
                    hasParse = true;
                }
            } else {
                return this.content;
            }
        }

        private void destroy() {
            bytes = null;
        }
    }
}
