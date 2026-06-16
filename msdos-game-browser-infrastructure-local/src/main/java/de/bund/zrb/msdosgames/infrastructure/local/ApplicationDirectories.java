package de.bund.zrb.msdosgames.infrastructure.local;

import java.io.File;

public final class ApplicationDirectories {

    private static final String APPLICATION_DIRECTORY_NAME = ".MS-DOS Game Browser";
    private static final String FALLBACK_DIRECTORY_NAME = ".msdos-game-browser";

    private ApplicationDirectories() {
    }

    public static File defaultApplicationDirectory() {
        String appDataDirectory = System.getenv("APPDATA");
        if (appDataDirectory != null && appDataDirectory.trim().length() > 0) {
            return ensureDirectory(new File(appDataDirectory, APPLICATION_DIRECTORY_NAME));
        }
        return ensureDirectory(new File(System.getProperty("user.home"), FALLBACK_DIRECTORY_NAME));
    }

    public static File defaultSettingsFile() {
        return new File(defaultApplicationDirectory(), "settings.properties");
    }

    public static File defaultLicenseStoreFile() {
        return new File(defaultApplicationDirectory(), "license-acceptance.properties");
    }

    public static File defaultDatabaseDirectory() {
        return ensureDirectory(new File(defaultApplicationDirectory(), "db"));
    }

    public static File defaultDownloadDirectory() {
        File userHome = new File(System.getProperty("user.home"));
        return ensureDirectory(new File(new File(userHome, "Downloads"), "msdos-games"));
    }

    private static File ensureDirectory(File directory) {
        if (directory.isDirectory()) {
            return directory;
        }
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Cannot create directory " + directory.getAbsolutePath());
        }
        return directory;
    }
}
