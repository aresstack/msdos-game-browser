package de.bund.zrb.msdosgames.domain;

public final class GameSearchCriteria {

    public static final int DEFAULT_PAGE_SIZE = 100;

    private final String query;
    private final String cursor;
    private final int pageSize;

    private GameSearchCriteria(String query, String cursor, int pageSize) {
        this.query = normalize(query);
        this.cursor = normalize(cursor);
        this.pageSize = normalizePageSize(pageSize);
    }

    public static GameSearchCriteria browseFirstPage(int pageSize) {
        return new GameSearchCriteria(null, null, pageSize);
    }

    public static GameSearchCriteria browseNextPage(String cursor, int pageSize) {
        return new GameSearchCriteria(null, cursor, pageSize);
    }

    public static GameSearchCriteria searchFirstPage(String query, int pageSize) {
        return new GameSearchCriteria(requireQuery(query), null, pageSize);
    }

    public static GameSearchCriteria searchNextPage(String query, String cursor, int pageSize) {
        return new GameSearchCriteria(requireQuery(query), cursor, pageSize);
    }

    public String getQuery() {
        return query;
    }

    public String getCursor() {
        return cursor;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean hasQuery() {
        return query != null && query.length() > 0;
    }

    public boolean hasCursor() {
        return cursor != null && cursor.length() > 0;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() == 0) {
            return null;
        }
        return trimmedValue;
    }

    private static String requireQuery(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery == null) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return normalizedQuery;
    }

    private static int normalizePageSize(int pageSize) {
        if (pageSize < DEFAULT_PAGE_SIZE) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(1000, pageSize);
    }
}
