package android.net;

import java.util.List;

public class StringUri extends AbstractHierarchicalUri {


    /** Used in parcelling. */
    static final int TYPE_ID = 1;

    /** URI string representation. */
    private final String uriString;

    public StringUri(String uriString) {
        if (uriString == null) {
            throw new NullPointerException("uriString");
        }

        this.uriString = uriString;
    }

    public int describeContents() {
        return 0;
    }


    /** Cached scheme separator index. */
    private volatile int cachedSsi = NOT_CALCULATED;

    /** Finds the first ':'. Returns -1 if none found. */
    private int findSchemeSeparator() {
        return cachedSsi == NOT_CALCULATED
                ? cachedSsi = uriString.indexOf(':')
                : cachedSsi;
    }

    /** Cached fragment separator index. */
    private volatile int cachedFsi = NOT_CALCULATED;

    /** Finds the first '#'. Returns -1 if none found. */
    private int findFragmentSeparator() {
        return cachedFsi == NOT_CALCULATED
                ? cachedFsi = uriString.indexOf('#', findSchemeSeparator())
                : cachedFsi;
    }

    public boolean isHierarchical() {
        int ssi = findSchemeSeparator();

        if (ssi == NOT_FOUND) {
            // All relative URIs are hierarchical.
            return true;
        }

        if (uriString.length() == ssi + 1) {
            // No ssp.
            return false;
        }

        // If the ssp starts with a '/', this is hierarchical.
        return uriString.charAt(ssi + 1) == '/';
    }

    public boolean isRelative() {
        // Note: We return true if the index is 0
        return findSchemeSeparator() == NOT_FOUND;
    }

    private volatile String scheme = NOT_CACHED;

    public String getScheme() {
        @SuppressWarnings("StringEquality")
        boolean cached = (scheme != NOT_CACHED);
        return cached ? scheme : (scheme = parseScheme());
    }

    private String parseScheme() {
        int ssi = findSchemeSeparator();
        return ssi == NOT_FOUND ? null : uriString.substring(0, ssi);
    }

    private Part ssp;

    private Part getSsp() {
        return ssp == null ? ssp = Part.fromEncoded(parseSsp()) : ssp;
    }

    public String getEncodedSchemeSpecificPart() {
        return getSsp().getEncoded();
    }

    public String getSchemeSpecificPart() {
        return getSsp().getDecoded();
    }

    private String parseSsp() {
        int ssi = findSchemeSeparator();
        int fsi = findFragmentSeparator();

        // Return everything between ssi and fsi.
        return fsi == NOT_FOUND
                ? uriString.substring(ssi + 1)
                : uriString.substring(ssi + 1, fsi);
    }

    private Part authority;

    private Part getAuthorityPart() {
        if (authority == null) {
            String encodedAuthority
                    = parseAuthority(this.uriString, findSchemeSeparator());
            return authority = Part.fromEncoded(encodedAuthority);
        }

        return authority;
    }

    public String getEncodedAuthority() {
        return getAuthorityPart().getEncoded();
    }

    public String getAuthority() {
        return getAuthorityPart().getDecoded();
    }

    private PathPart path;

    private PathPart getPathPart() {
        return path == null
                ? path = PathPart.fromEncoded(parsePath())
                : path;
    }

    public String getPath() {
        return getPathPart().getDecoded();
    }

    public String getEncodedPath() {
        return getPathPart().getEncoded();
    }

    public List<String> getPathSegments() {
        return getPathPart().getPathSegments();
    }

    private String parsePath() {
        String uriString = this.uriString;
        int ssi = findSchemeSeparator();

        // If the URI is absolute.
        if (ssi > -1) {
            // Is there anything after the ':'?
            boolean schemeOnly = ssi + 1 == uriString.length();
            if (schemeOnly) {
                // Opaque URI.
                return null;
            }

            // A '/' after the ':' means this is hierarchical.
            if (uriString.charAt(ssi + 1) != '/') {
                // Opaque URI.
                return null;
            }
        } else {
            // All relative URIs are hierarchical.
        }

        return parsePath(uriString, ssi);
    }

    private Part query;

    private Part getQueryPart() {
        return query == null
                ? query = Part.fromEncoded(parseQuery()) : query;
    }

    public String getEncodedQuery() {
        return getQueryPart().getEncoded();
    }

    private String parseQuery() {
        // It doesn't make sense to cache this index. We only ever
        // calculate it once.
        int qsi = uriString.indexOf('?', findSchemeSeparator());
        if (qsi == NOT_FOUND) {
            return null;
        }

        int fsi = findFragmentSeparator();

        if (fsi == NOT_FOUND) {
            return uriString.substring(qsi + 1);
        }

        if (fsi < qsi) {
            // Invalid.
            return null;
        }

        return uriString.substring(qsi + 1, fsi);
    }

    public String getQuery() {
        return getQueryPart().getDecoded();
    }

    private Part fragment;

    private Part getFragmentPart() {
        return fragment == null
                ? fragment = Part.fromEncoded(parseFragment()) : fragment;
    }

    public String getEncodedFragment() {
        return getFragmentPart().getEncoded();
    }

    private String parseFragment() {
        int fsi = findFragmentSeparator();
        return fsi == NOT_FOUND ? null : uriString.substring(fsi + 1);
    }

    public String getFragment() {
        return getFragmentPart().getDecoded();
    }

    public String toString() {
        return uriString;
    }

    /**
     * Parses an authority out of the given URI string.
     *
     * @param uriString URI string
     * @param ssi scheme separator index, -1 for a relative URI
     *
     * @return the authority or null if none is found
     */
    static String parseAuthority(String uriString, int ssi) {
        int length = uriString.length();

        // If "//" follows the scheme separator, we have an authority.
        if (length > ssi + 2
                && uriString.charAt(ssi + 1) == '/'
                && uriString.charAt(ssi + 2) == '/') {
            // We have an authority.

            // Look for the start of the path, query, or fragment, or the
            // end of the string.
            int end = ssi + 3;
            LOOP: while (end < length) {
                switch (uriString.charAt(end)) {
                    case '/': // Start of path
                    case '?': // Start of query
                    case '#': // Start of fragment
                        break LOOP;
                }
                end++;
            }

            return uriString.substring(ssi + 3, end);
        } else {
            return null;
        }

    }

    /**
     * Parses a path out of this given URI string.
     *
     * @param uriString URI string
     * @param ssi scheme separator index, -1 for a relative URI
     *
     * @return the path
     */
    static String parsePath(String uriString, int ssi) {
        int length = uriString.length();

        // Find start of path.
        int pathStart;
        if (length > ssi + 2
                && uriString.charAt(ssi + 1) == '/'
                && uriString.charAt(ssi + 2) == '/') {
            // Skip over authority to path.
            pathStart = ssi + 3;
            LOOP: while (pathStart < length) {
                switch (uriString.charAt(pathStart)) {
                    case '?': // Start of query
                    case '#': // Start of fragment
                        return ""; // Empty path.
                    case '/': // Start of path!
                        break LOOP;
                }
                pathStart++;
            }
        } else {
            // Path starts immediately after scheme separator.
            pathStart = ssi + 1;
        }

        // Find end of path.
        int pathEnd = pathStart;
        LOOP: while (pathEnd < length) {
            switch (uriString.charAt(pathEnd)) {
                case '?': // Start of query
                case '#': // Start of fragment
                    break LOOP;
            }
            pathEnd++;
        }

        return uriString.substring(pathStart, pathEnd);
    }

    public Builder buildUpon() {
        if (isHierarchical()) {
            return new Builder()
                    .scheme(getScheme())
                    .authority(getAuthorityPart())
                    .path(getPathPart())
                    .query(getQueryPart())
                    .fragment(getFragmentPart());
        } else {
            return new Builder()
                    .scheme(getScheme())
                    .opaquePart(getSsp())
                    .fragment(getFragmentPart());
        }
    }

}
