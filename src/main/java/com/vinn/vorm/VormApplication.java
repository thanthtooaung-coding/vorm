package com.vinn.vorm;

import com.vinn.vorm.config.DataSource;
import com.vinn.vorm.core.Vorm;
import com.vinn.vorm.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class VormApplication {

	public static void main(String[] args) {
		String jdbcUrl = "jdbc:mysql://localhost:3306/vorm_db";
		String username = "root";
		String password = "mysql";
		try (DataSource dataSource = new DataSource(jdbcUrl, username, password)) {

			try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
				System.out.println("Setting up database schema...");
				stmt.execute("DROP TABLE IF EXISTS users;");
				stmt.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, username VARCHAR(255), email VARCHAR(255));");
				System.out.println("Schema created successfully.");
			} catch (SQLException e) {
				throw new RuntimeException("Failed to set up database schema", e);
			}
			// --- Use the ORM ---
			final Vorm vorm = new Vorm(dataSource);

			System.out.println("\n--- 1. Saving new users (INSERT) ---");
			vorm.save(new User(1L, "john.doe", "john.doe@example.com"));
			vorm.save(new User(2L, "jane.doe", "jane.doe@example.com"));

			System.out.println("\n--- 2. Finding all users (findAll) ---");
			vorm.findAll(User.class).forEach(System.out::println);

			System.out.println("\n--- 3. Updating a user (UPDATE) ---");
			final User userToUpdate = vorm.findById(User.class, 2L);
			System.out.println("Original user: " + userToUpdate);
			userToUpdate.setEmail("jane.d.updated@example.com");
			vorm.save(userToUpdate);

			final User updatedUser = vorm.findById(User.class, 2L);
			System.out.println("Updated user: " + updatedUser);

			System.out.println("\n--- 4. Deleting a user (deleteById) ---");
			vorm.deleteById(User.class, 1L);

			System.out.println("\n--- Verifying deletion ---");
			final List<User> remainingUsers = vorm.findAll(User.class);
			System.out.println("Remaining users in DB:");
			remainingUsers.forEach(System.out::println);

		} catch (Exception e) {
			System.err.println("An error occurred: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("\nApplication finished and connection pool closed.");
	}

}
