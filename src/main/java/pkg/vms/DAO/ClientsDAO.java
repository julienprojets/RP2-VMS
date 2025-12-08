package pkg.vms.DAO;

import pkg.vms.model.Clients;
import pkg.vms.DBconnection.DBconnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientsDAO {

    // CREATE
    public void insert(Clients c) throws SQLException {
        String sql = "INSERT INTO clients (nom_client, email_client, address_client, phone_client) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getNom_client());
            ps.setString(2, c.getEmail_client());
            ps.setString(3, c.getAddress_client());
            ps.setString(4, c.getPhone_client());
            ps.executeUpdate();
        }
    }

    // READ by ID
    public Clients getById(int ref_client) throws SQLException {
        String sql = "SELECT * FROM clients WHERE ref_client = ?";
        try (PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, ref_client);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Clients(
                            rs.getInt("ref_client"),
                            rs.getString("nom_client"),
                            rs.getString("email_client"),
                            rs.getString("address_client"),
                            rs.getString("phone_client")
                    );
                }
            }
        }
        return null;
    }

    // READ all
    public List<Clients> getAll() throws SQLException {
        List<Clients> list = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY ref_client";
        try (Statement st = DBconnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Clients(
                        rs.getInt("ref_client"),
                        rs.getString("nom_client"),
                        rs.getString("email_client"),
                        rs.getString("address_client"),
                        rs.getString("phone_client")
                ));
            }
        }
        return list;
    }

    // UPDATE
    public void update(Clients c) throws SQLException {
        String sql = "UPDATE clients SET nom_client = ?, email_client = ?, address_client = ?, phone_client = ? WHERE ref_client = ?";
        try (PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getNom_client());
            ps.setString(2, c.getEmail_client());
            ps.setString(3, c.getAddress_client());
            ps.setString(4, c.getPhone_client());
            ps.setInt(5, c.getRef_client());
            ps.executeUpdate();
        }
    }

    // DELETE
    public void delete(int ref_client) throws SQLException {
        String sql = "DELETE FROM clients WHERE ref_client = ?";
        try (PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, ref_client);
            ps.executeUpdate();
        }
    }
}