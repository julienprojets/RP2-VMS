package pkg.vms.DAO;

import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Users;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsersDAO {

    // Fetch all users
    public List<Users> getAllUsers() {
        List<Users> usersList = new ArrayList<>();
        String query = "SELECT * FROM users";

        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String company = null;
                try {
                    company = rs.getString("company");
                } catch (SQLException e) {
                    // Column doesn't exist yet
                }
                if (company == null) {
                    company = "";
                }
                
                Users user = new Users(
                        rs.getString("username"),
                        rs.getString("first_name_user"),
                        rs.getString("last_name_user"),
                        rs.getString("email_user"),
                        rs.getString("role"),
                        rs.getString("password"),
                        rs.getString("ddl"),
                        rs.getString("titre"),
                        rs.getString("status"),
                        company
                );
                usersList.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usersList;
    }

    // Add a new user
    public boolean addUser(Users user) {
        String query = "INSERT INTO users(username, first_name_user, last_name_user, email_user, role, password, ddl, titre, status, company) VALUES(?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFirstName());
            ps.setString(3, user.getLastName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole());
            ps.setString(6, user.getPassword());
            ps.setString(7, user.getDdl());
            ps.setString(8, user.getTitre());
            ps.setString(9, user.getStatus());
            ps.setString(10, user.getCompany() != null ? user.getCompany() : "");

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Update user status
    public boolean updateUserStatus(String username, String status) {
        String query = "UPDATE users SET status=? WHERE username=?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, status);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Delete a user
    public boolean deleteUser(String username) {
        String query = "DELETE FROM users WHERE username=?";

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, username);
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

}
