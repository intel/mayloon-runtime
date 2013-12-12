package android.net;

import java.util.List;

public abstract class AbstractHierarchicalUri  extends Uri{


    public String getLastPathSegment() {
        // TODO: If we haven't parsed all of the segments already, just
        // grab the last one directly so we only allocate one string.

        List<String> segments = getPathSegments();
        int size = segments.size();
        if (size == 0) {
            return null;
        }
        return segments.get(size - 1);
    }

    private Part userInfo;

    private Part getUserInfoPart() {
        return userInfo == null
                ? userInfo = Part.fromEncoded(parseUserInfo()) : userInfo;
    }

    public final String getEncodedUserInfo() {
        return getUserInfoPart().getEncoded();
    }

    private String parseUserInfo() {
        String authority = getEncodedAuthority();
        if (authority == null) {
            return null;
        }

        int end = authority.indexOf('@');
        return end == NOT_FOUND ? null : authority.substring(0, end);
    }

    public String getUserInfo() {
        return getUserInfoPart().getDecoded();
    }

    private volatile String host = NOT_CACHED;

    public String getHost() {
        @SuppressWarnings("StringEquality")
        boolean cached = (host != NOT_CACHED);
        return cached ? host
                : (host = parseHost());
    }

    private String parseHost() {
        String authority = getEncodedAuthority();
        if (authority == null) {
            return null;
        }

        // Parse out user info and then port.
        int userInfoSeparator = authority.indexOf('@');
        int portSeparator = authority.indexOf(':', userInfoSeparator);

        String encodedHost = portSeparator == NOT_FOUND
                ? authority.substring(userInfoSeparator + 1)
                : authority.substring(userInfoSeparator + 1, portSeparator);

        return decode(encodedHost);
    }

    private volatile int port = NOT_CALCULATED;

    public int getPort() {
        return port == NOT_CALCULATED
                ? port = parsePort()
                : port;
    }

    private int parsePort() {
        String authority = getEncodedAuthority();
        if (authority == null) {
            return -1;
        }

        // Make sure we look for the port separtor *after* the user info
        // separator. We have URLs with a ':' in the user info.
        int userInfoSeparator = authority.indexOf('@');
        int portSeparator = authority.indexOf(':', userInfoSeparator);

        if (portSeparator == NOT_FOUND) {
            return -1;
        }

        String portString = decode(authority.substring(portSeparator + 1));
        try {
            return Integer.parseInt(portString);
        } catch (NumberFormatException e) {
//            Log.w(LOG, "Error parsing port string.", e);
            return -1;
        }
    }

}
