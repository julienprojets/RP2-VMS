package pkg.vms.model;

import java.util.Date;

public class Vouchers {

    private int ref_client;
    private String code_voucher;
    private double price;
    private boolean redeemed;

    private Date init_date;
    private Date expiry_date;
    private Date date_redeemed;
    private String bearer;
    private String status_voucher;

    //constructor
    public Vouchers(int id, int quantity, double price, String status, int requestId) {}

    //constructor
    public Vouchers(int ref_client, String code_voucher, double price, boolean redeemed) {
        this.ref_client = ref_client;
        this.code_voucher = code_voucher;
        this.price = price;
        this.redeemed = redeemed;
    }

    public Vouchers(int refVoucher, int valVoucher, java.sql.Date initDate, java.sql.Date expiryDate, String statusVoucher, java.sql.Date dateRedeemed, String bearer, int refRequest, String redeemedBy, String redeemedBranch) {
    }

    // Getters & Setters

    public int getRef_client() {
        return ref_client;
    }

    public void setRef_client(int ref_client) {
        this.ref_client = ref_client;
    }

    public String getCode_voucher() {
        return code_voucher;
    }

    public void setCode_voucher(String code_voucher) {
        this.code_voucher = code_voucher;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isRedeemed() {
        return redeemed;
    }

    public void setRedeemed(boolean redeemed) {
        this.redeemed = redeemed;
    }

    public Date getInit_date() {
        return init_date;
    }

    public void setInit_date(Date init_date) {
        this.init_date = init_date;
    }

    public Date getExpiry_date() {
        return expiry_date;
    }

    public void setExpiry_date(Date expiry_date) {
        this.expiry_date = expiry_date;
    }

    public Date getDate_redeemed() {
        return date_redeemed;
    }

    public void setDate_redeemed(Date date_redeemed) {
        this.date_redeemed = date_redeemed;
    }

    public String getBearer() {
        return bearer;
    }

    public void setBearer(String bearer) {
        this.bearer = bearer;
    }

    public String getStatus_voucher() {
        return status_voucher;
    }

    public void setStatus_voucher(String status_voucher) {
        this.status_voucher = status_voucher;
    }

    // Voucher logic

    /** Activate the voucher */
    public void activateVoucher() {
        if (!this.redeemed) {
            this.status_voucher = "Active";
            if (this.init_date == null) {
                this.init_date = new Date();
            }
        }
    }

    /** Redeem the voucher */
    public void redeemVoucher(Branch branch, Users users) {
        this.redeemed = true;
        this.status_voucher = "Redeemed";
        this.date_redeemed = new Date();
    }

    /** Check if voucher is valid for a given date */
    public boolean checkValidity(Date currentDate) {
        if (currentDate == null || init_date == null || expiry_date == null) return false;

        boolean inRange =
                !currentDate.before(init_date) &&
                        !currentDate.after(expiry_date);

        return inRange && !redeemed;
    }

    /** Display voucher info */
    public String getVoucherInfo() {
        return "Voucher: " + code_voucher +
                ", price: " + price +
                ", redeemed: " + redeemed +
                ", init: " + init_date +
                ", expiry: " + expiry_date +
                ", bearer: " + bearer;
    }
}
