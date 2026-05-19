package server.test;

import server.database.DatabaseConnection;

import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {

        try (Connection connection = DatabaseConnection.connect()) {
            System.out.println("CONNECTED SUCCESSFULLY");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
