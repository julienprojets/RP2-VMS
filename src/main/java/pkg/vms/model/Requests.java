package pkg.vms.model;

import javafx.beans.property.*;
import java.util.*;

/**
 * Requests model
 */
public class Requests {

    public int ref_request;
    public Date creation_date;
    public int num_voucher;
    public int val_voucher;
    public String status;
    public int ref_client;
    public Date init_payment;
    public String payment;
    public String status_payment;
    public Date date_payment;
    public int ref_payment;
    public Date date_approval;
    public int duration_voucher;
    public Date expiry_voucher;
    public String proof_of_request;
    public String proof_file;
    public String processed_by;
    public String approved_by;
    public String validated_by;

    // ===== Constructors =====

    // Default constructor
    public Requests() {
    }

    // Minimal constructor for TableView
    public Requests(int id, String status, int clientId) {
        this.ref_request = id;
        this.status = status;
        this.ref_client = clientId;
    }

    // Full constructor matching DB table
    public Requests(int ref_request, Date creation_date, int num_voucher, String status, String payment,
                    Date date_payment, int ref_payment, Date date_approval, int duration_voucher, int ref_client,
                    String processed_by, String approved_by, String validated_by) {
        this.ref_request = ref_request;
        this.creation_date = creation_date;
        this.num_voucher = num_voucher;
        this.status = status;
        this.payment = payment;
        this.date_payment = date_payment;
        this.ref_payment = ref_payment;
        this.date_approval = date_approval;
        this.duration_voucher = duration_voucher;
        this.ref_client = ref_client;
        this.processed_by = processed_by;
        this.approved_by = approved_by;
        this.validated_by = validated_by;
    }

    // ===== JavaFX properties for TableView =====
    public IntegerProperty refRequestProperty() {
        return new SimpleIntegerProperty(ref_request);
    }

    public IntegerProperty refClientProperty() {
        return new SimpleIntegerProperty(ref_client);
    }

    public StringProperty statusProperty() {
        return new SimpleStringProperty(status);
    }

    // ===== Getters and setters =====
    public int getRef_request() { return ref_request; }
    public void setRef_request(int ref_request) { this.ref_request = ref_request; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRef_client() { return ref_client; }
    public void setRef_client(int ref_client) { this.ref_client = ref_client; }

    public Date getCreation_date() { return creation_date; }
    public void setCreation_date(Date creation_date) { this.creation_date = creation_date; }

    public int getNum_voucher() { return num_voucher; }
    public void setNum_voucher(int num_voucher) { this.num_voucher = num_voucher; }

    public int getVal_voucher() { return val_voucher; }
    public void setVal_voucher(int val_voucher) { this.val_voucher = val_voucher; }

    public Date getInit_payment() { return init_payment; }
    public void setInit_payment(Date init_payment) { this.init_payment = init_payment; }

    public String getPayment() { return payment; }
    public void setPayment(String payment) { this.payment = payment; }

    public String getStatus_payment() { return status_payment; }
    public void setStatus_payment(String status_payment) { this.status_payment = status_payment; }

    public Date getDate_payment() { return date_payment; }
    public void setDate_payment(Date date_payment) { this.date_payment = date_payment; }

    public int getRef_payment() { return ref_payment; }
    public void setRef_payment(int ref_payment) { this.ref_payment = ref_payment; }

    public Date getDate_approval() { return date_approval; }
    public void setDate_approval(Date date_approval) { this.date_approval = date_approval; }

    public int getDuration_voucher() { return duration_voucher; }
    public void setDuration_voucher(int duration_voucher) { this.duration_voucher = duration_voucher; }

    public Date getExpiry_voucher() { return expiry_voucher; }
    public void setExpiry_voucher(Date expiry_voucher) { this.expiry_voucher = expiry_voucher; }

    public String getProof_of_request() { return proof_of_request; }
    public void setProof_of_request(String proof_of_request) { this.proof_of_request = proof_of_request; }

    public String getProof_file() { return proof_file; }
    public void setProof_file(String proof_file) { this.proof_file = proof_file; }

    public String getProcessed_by() { return processed_by; }
    public void setProcessed_by(String processed_by) { this.processed_by = processed_by; }

    public String getApproved_by() { return approved_by; }
    public void setApproved_by(String approved_by) { this.approved_by = approved_by; }

    public String getValidated_by() { return validated_by; }
    public void setValidated_by(String validated_by) { this.validated_by = validated_by; }

    // ===== Request actions =====
    public void submitRequest() { /* implement logic */ }

    public void processPayment(String paymentMethod, Date date, String proof) {
        this.payment = paymentMethod;
        this.date_payment = date;
        this.proof_of_request = proof;
    }

    public void approveRequest(Users approver) {
        this.status = "Approved";
        this.date_approval = new Date();
    }

    public void rejectRequest(Users approver, String reason) {
        this.status = "Rejected: " + reason;
        this.date_approval = new Date();
    }

    public List<Vouchers> generateVouchers() { return Collections.emptyList(); }
    public void generateProof() { /* implement logic */ }
    public void updateStatus(String action) { this.status = action; }
    public void nextStatus() { /* implement logic */ }
}
