package com.adtime.http.resource.url;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class URLCanonicalizer {

    public static URL toUrl(String url) {
        try {
            return new URL(UrlResolver.resolveUrl("", url));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCanonicalURL(String url) {
        return getCanonicalURL(url, null);
    }

    public static String getHost(String url) {
        try {
            URL _url = new URL(UrlResolver.resolveUrl("", url));
            return _url.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDomain(String url) {
        try {
            URL _url = new URL(url);
            int port = _url.getPort();
            if (port > 0 && port != 80 && port != 443) {
                return _url.getProtocol() + "://" + _url.getHost() + ":" + port;
            } else {
                return _url.getProtocol() + "://" + _url.getHost();
            }
        } catch (Exception e) {
            return null;
        }
    }


    public static String getReferer(String url) {
        try {
            URL _url = new URL(url);
            int port = _url.getPort();

            String path = _url.getPath();

            if (null == path || path.length() < 1) {
                path = "";
            }/* else {
                path = path.substring(1);
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                int last = path.lastIndexOf("/");
                if (last > 0) {
                    path = "/" + path.substring(0, last);
                } else {
                    path = "";
                }
            }*/

            if (port > 0 && port != 80 && port != 443) {
                return _url.getProtocol() + "://" + _url.getHost() + ":" + port + path;
            } else {
                return _url.getProtocol() + "://" + _url.getHost() + path;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String getPath(String url, boolean pre) {
        try {
            URL _url = new URL(UrlResolver.resolveUrl("", url));
            int port = _url.getPort();
            String path = new URI(_url.getPath()).normalize().toString().trim();
            int idx = path.indexOf("//");
            while (idx >= 0) {
                path = path.replace("//", "/");
                idx = path.indexOf("//");
            }
            while (path.startsWith("/../")) {
                path = path.substring(3);
            }
            if (pre) {
                idx = path.lastIndexOf("/");
                if (idx > 0) {
                    path = path.substring(0, idx);
                } else {
                    path = "";
                }
            }
            if (path.length() > 0 && !path.startsWith("/")) {
                path = "/" + path;
            }
            if (port > 0 && port != 80 && port != 443) {
                return _url.getProtocol() + "://" + _url.getHost() + ":" + port + path;
            } else {
                return _url.getProtocol() + "://" + _url.getHost() + path;
            }
        } catch (RuntimeException ignore) {
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static String mergePathUrl(String url, String newUrl) {
        if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
            String domain = getDomain(url);
            if (newUrl.length() < 1) {
                return null;
            }
            switch (newUrl.substring(0, 1)) {
                case "/":
                    return domain + newUrl;
                case "?":
                    return getPath(url, false) + newUrl;
                default:
                    return getPath(url, true) + "/" + newUrl;
            }
        } else {
            return newUrl;
        }
    }

    public static String resolveRedirect(String url, String newUrl) {
        if (newUrl.startsWith("//")) {
            return (url.startsWith("http://") ? "http:" : "https:") + newUrl;
        } else {
            return mergePathUrl(url, newUrl);
        }
    }

    public static String getCanonicalURL(String href, String context) {
        return getCanonicalURL(href, context, null);
    }

    public static String getCanonicalURL(String href, String context, Set<String> excludeParameter) {

        try {
            URL canonicalURL = new URL(UrlResolver.resolveUrl(context == null ? "" : context, href));

            String host = canonicalURL.getHost().toLowerCase();
            if (host.trim().length() < 1) {
                // This is an invalid Url.
                return null;
            }

            String path = canonicalURL.getPath();

            if (StringUtils.isNotBlank(path)) {
                path = StringUtils.replace(path, " ", "%20");
            }

			/*
             * Normalize: no empty segments (i.e., "//"), no segments equal to
			 * ".", and no segments equal to ".." that are preceded by a segment
			 * not equal to "..".
			 */
            path = new URI(path).normalize().toString();

			/*
             * Convert '//' -> '/'
			 */
            int idx = path.indexOf("//");
            while (idx >= 0) {
                path = path.replace("//", "/");
                idx = path.indexOf("//");
            }

			/*
             * Drop starting '/../'
			 */
            while (path.startsWith("/../")) {
                path = path.substring(3);
            }

			/*
             * Trim
			 */
            path = path.trim();

            final List<String[]> params = createParameterMap(canonicalURL.getQuery(), excludeParameter);
            final String queryString;

            if (params != null && params.size() > 0) {
                String canonicalParams = canonicalize(params);
                queryString = (canonicalParams.isEmpty() ? "" : "?" + canonicalParams);
            } else {
                queryString = "";
            }

			/*
             * Add starting slash if needed
			 */
            if (path.length() == 0) {
                path = "/" + path;
            }

			/*
             * Drop default port: example.com:80 -> example.com
			 */
            int port = canonicalURL.getPort();
            if (port == canonicalURL.getDefaultPort()) {
                port = -1;
            }

            String protocol = canonicalURL.getProtocol().toLowerCase();
            String pathAndQueryString = normalizePath(path) + queryString;

            URL result = new URL(protocol, host, port, pathAndQueryString);
            return result.toExternalForm();

        } catch (MalformedURLException ex) {
            return null;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Takes a query string, separates the constituent name-value pairs
     *
     * @return Null if there is no query string.
     */
    private static List<String[]> createParameterMap(final String queryString, final Set<String> excludeParameter) {

        if (queryString == null || queryString.isEmpty()) {
            return null;
        }

        final String[] pairs = queryString.split("&");
        List<String[]> pairList = new ArrayList<>(pairs.length);


        for (final String pair : pairs) {
            if (pair.length() == 0) {
                continue;
            }

            String[] tokens = pair.split("=", 2);
            switch (tokens.length) {
                case 1:
                    if (pair.charAt(0) == '=') {
                        pairList.add(new String[]{"", tokens[0]});
                    } else {
                        if (null != excludeParameter && excludeParameter.contains(tokens[0])) {
                            continue;
                        }
                        pairList.add(new String[]{tokens[0], ""});
                    }
                    break;
                case 2:
                    if (null != excludeParameter && excludeParameter.contains(tokens[0])) {
                        continue;
                    }
                    pairList.add(new String[]{tokens[0], tokens[1]});
                    break;
                default:
                    break;
            }
        }
        return pairList;
    }

    /**
     * Canonicalize the query string.
     *
     * @param sortedParamMap Parameter name-value pairs in lexicographical order.
     * @return Canonical form of query string.
     */
    private static String canonicalize(final List<String[]> sortedParamMap) {
        if (sortedParamMap == null || sortedParamMap.isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder(100);
        for (String[] pair : sortedParamMap) {
            final String key = pair[0].toLowerCase();
            if (key.equals("jsessionid") || key.equals("phpsessid") || key.equals("aspsessionid")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(percentEncodeRfc3986(pair[0]));
            if (!pair[1].isEmpty()) {
                sb.append('=');
                sb.append(percentEncodeRfc3986(pair[1]));
            }
        }
        return sb.toString();
    }

    /**
     * Percent-encode values according the RFC 3986. The built-in Java
     * URLEncoder does not encode according to the RFC, so we make the extra
     * replacements.
     *
     * @param string Decoded string.
     * @return Encoded string per RFC 3986.
     */
    private static String percentEncodeRfc3986(String string) {
        try {
            string = string.replaceAll("\\+", "%2B").replaceAll("\\*", "%2A").replaceAll("%7E", "~");//.replaceAll("%","%25");
            //string = URLDecoder.decode(string, "UTF-8");
            //string = URLEncoder.encode(string, "UTF-8");
            return string;
        } catch (Exception e) {
            return string;
        }
    }

    private static String normalizePath(final String path) {
        return path.replace("%7E", "~").replace(" ", "%20");
    }
}
