package de.bund.zrb.msdosgames.backend;

import com.google.gson.Gson;
import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameIdentifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class H2InMemoryGameDetailsStore {

    private final Connection connection;
    private final Gson gson = new Gson();
    private final GameDetailsRecordMapper mapper = new GameDetailsRecordMapper();

    H2InMemoryGameDetailsStore() {
        try {
            connection = DriverManager.getConnection("jdbc:h2:mem:msdos_game_browser;DB_CLOSE_DELAY=-1");
            createSchema();
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot create in-memory cache", exception);
        }
    }

    synchronized GameDetails findDetails(GameIdentifier identifier) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("select json_value from game_details where identifier = ?");
        try {
            statement.setString(1, identifier.getValue());
            ResultSet resultSet = statement.executeQuery();
            try {
                if (!resultSet.next()) {
                    return null;
                }
                GameDetailsRecord record = gson.fromJson(resultSet.getString(1), GameDetailsRecord.class);
                return mapper.toDetails(record);
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    synchronized void saveDetails(GameDetails details) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("merge into game_details key(identifier) values (?, ?)");
        try {
            statement.setString(1, details.getIdentifier().getValue());
            statement.setString(2, gson.toJson(mapper.toRecord(details)));
            statement.executeUpdate();
        } finally {
            statement.close();
        }
    }

    synchronized byte[] findImage(String url) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("select data from image_cache where url = ?");
        try {
            statement.setString(1, url);
            ResultSet resultSet = statement.executeQuery();
            try {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getBytes(1);
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    synchronized void saveImage(String url, byte[] data) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("merge into image_cache key(url) values (?, ?)");
        try {
            statement.setString(1, url);
            statement.setBytes(2, data);
            statement.executeUpdate();
        } finally {
            statement.close();
        }
    }

    byte[] loadImage(String url) throws IOException, SQLException {
        byte[] cachedImage = findImage(url);
        if (cachedImage != null) {
            return cachedImage;
        }

        byte[] loadedImage = downloadBytes(url);
        saveImage(url, loadedImage);
        return loadedImage;
    }

    synchronized void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    private void createSchema() throws SQLException {
        Statement statement = connection.createStatement();
        try {
            statement.executeUpdate("create table if not exists game_details (identifier varchar(512) primary key, json_value clob not null)");
            statement.executeUpdate("create table if not exists image_cache (url varchar(2048) primary key, data blob not null)");
        } finally {
            statement.close();
        }
    }

    private byte[] downloadBytes(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "msdos-game-browser/1.0");
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("HTTP " + statusCode + " while loading image " + url);
        }

        InputStream inputStream = connection.getInputStream();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        } finally {
            inputStream.close();
        }
    }
}
