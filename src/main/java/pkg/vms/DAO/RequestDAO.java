package pkg.vms.DAO;

import pkg.vms.model.Requests;
import pkg.vms.DBconnection.DBconnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    // Get all requests for a client
    public List<Requests> getRequestsByClient(int ref_client) throws SQLException {
        List<Requests> list = new ArrayList<>();

        String sql = "SELECT * FROM requests WHERE ref_client = ?";
        PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql);
        ps.setInt(1, ref_client);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {

            Requests r = new Requests(rs.getInt("id"), rs.getString("status"), rs.getInt("client_id"));
            r.ref_client = rs.getInt("ref_client");
            r.num_voucher = rs.getInt("num_voucher");
            r.duration_voucher = rs.getInt("duration_voucher");
            r.status = rs.getString("status");
            r.creation_date = rs.getTimestamp("creation_date");
            r.expiry_voucher = rs.getTimestamp("expiry_voucher");

            list.add(r);
        }

        return list;
    }

    // Insert a request
    public void insert(Requests r) throws SQLException {
        String sql = """
            INSERT INTO requests 
            (ref_client, num_voucher, duration_voucher, status, creation_date, expiry_voucher)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql);
        ps.setInt(1, r.ref_client);
        ps.setInt(2, r.num_voucher);
        ps.setInt(3, r.duration_voucher);
        ps.setString(4, r.status);
        ps.setTimestamp(5, new Timestamp(r.creation_date.getTime()));
        ps.setTimestamp(6, new Timestamp(r.expiry_voucher.getTime()));

        ps.executeUpdate();
    }
}
