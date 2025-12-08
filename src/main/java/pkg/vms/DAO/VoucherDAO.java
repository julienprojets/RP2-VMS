package pkg.vms.DAO;

import pkg.vms.model.Vouchers;
import pkg.vms.DBconnection.DBconnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoucherDAO {

    // Get all vouchers for a client
    public List<Vouchers> getVouchersByClient(int ref_client) throws SQLException {
        List<Vouchers> list = new ArrayList<>();

        String sql = "SELECT * FROM vouchers WHERE ref_client = ?";
        PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql);
        ps.setInt(1, ref_client);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {

            Vouchers v = new Vouchers(rs.getInt("id"), rs.getInt("quantity"), rs.getDouble("price"), rs.getString("status"), rs.getInt("request_id"));
            v.setRef_client(rs.getInt("ref_client"));
            v.setCode_voucher(rs.getString("code_voucher"));
            v.setPrice(rs.getDouble("price"));
            v.setRedeemed(rs.getBoolean("redeemed"));

            list.add(v);
        }

        return list;
    }

    // Insert generated voucher
    public void insert(Vouchers v) throws SQLException {
        String sql = """
            INSERT INTO vouchers (ref_client, code_voucher, price, redeemed)
            VALUES (?, ?, ?, ?)
        """;

        PreparedStatement ps = DBconnection.getConnection().prepareStatement(sql);
        ps.setInt(1, v.getRef_client());
        ps.setString(2, v.getCode_voucher());
        ps.setDouble(3, v.getPrice());
        ps.setBoolean(4, v.isRedeemed());

        ps.executeUpdate();
    }
}
