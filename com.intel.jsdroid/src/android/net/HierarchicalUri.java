package android.net;

import java.util.List;

public class HierarchicalUri extends AbstractHierarchicalUri {


    /** Used in parcelling. */
    static final int TYPE_ID = 3;

    private final String scheme; // can be null
    private final Part authority;
    private final PathPart path;
    private final Part query;
    private final Part fragment;

    public HierarchicalUri(String scheme, Part authority, PathPart path,
            Part query, Part fragment) {
    	System.out.println("???");
        this.scheme = scheme;
        this.authority = Part.nonNull(authority);
        this.path = path == null ? PathPart.NULL : path;
        this.query = Part.nonNull(query);
        this.fragment = Part.nonNull(fragment);
        System.out.println("!!!");
    }

    public int describeContents() {
        return 0;
    }

    public boolean isHierarchical() {
        return true;
    }

    public boolean isRelative() {
        return scheme == null;
    }

    public String getScheme() {
        return scheme;
    }

    private Part ssp;

    private Part getSsp() {
        return ssp == null
                ? ssp = Part.fromEncoded(makeSchemeSpecificPart()) : ssp;
    }

    public String getEncodedSchemeSpecificPart() {
        return getSsp().getEncoded();
    }

    public String getSchemeSpecificPart() {
        return getSsp().getDecoded();
    }

    /**
     * Creates the encoded scheme-specific part from its sub parts.
     */
    private String makeSchemeSpecificPart() {
        StringBuilder builder = new StringBuilder();
        appendSspTo(builder);
        return builder.toString();
    }

    private void appendSspTo(StringBuilder builder) {
        String encodedAuthority = authority.getEncoded();
        if (encodedAuthority != null) {
            // Even if the authority is "", we still want to append "//".
            builder.append("//").append(encodedAuthority);
        }

        String encodedPath = path.getEncoded();
        if (encodedPath != null) {
            builder.append(encodedPath);
        }

        if (!query.isEmpty()) {
            builder.append('?').append(query.getEncoded());
        }
    }

    public String getAuthority() {
        return this.authority.getDecoded();
    }

    public String getEncodedAuthority() {
        return this.authority.getEncoded();
    }

    public String getEncodedPath() {
        return this.path.getEncoded();
    }

    public String getPath() {
        return this.path.getDecoded();
    }

    public String getQuery() {
        return this.query.getDecoded();
    }

    public String getEncodedQuery() {
        return this.query.getEncoded();
    }

    public String getFragment() {
        return this.fragment.getDecoded();
    }

    public String getEncodedFragment() {
        return this.fragment.getEncoded();
    }

    public List<String> getPathSegments() {
        return this.path.getPathSegments();
    }

    private volatile String uriString = NOT_CACHED;

    @Override
    public String toString() {
        @SuppressWarnings("StringEquality")
        boolean cached = (uriString != NOT_CACHED);
        return cached ? uriString
                : (uriString = makeUriString());
    }

    private String makeUriString() {
        StringBuilder builder = new StringBuilder();

        if (scheme != null) {
            builder.append(scheme).append(':');
        }

        appendSspTo(builder);

        if (!fragment.isEmpty()) {
            builder.append('#').append(fragment.getEncoded());
        }

        return builder.toString();
    }

    public Builder buildUpon() {
        return new Builder()
                .scheme(scheme)
                .authority(authority)
                .path(path)
                .query(query)
                .fragment(fragment);
    }

}
