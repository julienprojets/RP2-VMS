package pkg.vms.model;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Clients {

    // JavaFX Properties
    private IntegerProperty ref_client;
    private StringProperty nom_client;
    private StringProperty email_client;
    private StringProperty address_client;
    private StringProperty phone_client;

    // Associated requests + vouchers
    private List<Requests> requests = new ArrayList<>();
    private List<Vouchers> vouchers = new ArrayList<>();


    // =============================
    // Constructors
    // =============================

    public Clients() {
        this(0, "", "", "", "");
    }

    public Clients(int ref_client, String nom_client, String email_client, String address_client, String phone_client) {
        this.ref_client = new SimpleIntegerProperty(ref_client);
        this.nom_client = new SimpleStringProperty(nom_client);
        this.email_client = new SimpleStringProperty(email_client);
        this.address_client = new SimpleStringProperty(address_client);
        this.phone_client = new SimpleStringProperty(phone_client);
    }


    // =============================
    // Standard Getters & Setters
    // (Used by DAO + business logic)
    // =============================

    public int getRef_client() { return ref_client.get(); }
    public void setRef_client(int ref_client) { this.ref_client.set(ref_client); }

    public String getNom_client() { return nom_client.get(); }
    public void setNom_client(String nom_client) { this.nom_client.set(nom_client); }

    public String getEmail_client() { return email_client.get(); }
    public void setEmail_client(String email_client) { this.email_client.set(email_client); }

    public String getAddress_client() { return address_client.get(); }
    public void setAddress_client(String address_client) { this.address_client.set(address_client); }

    public String getPhone_client() { return phone_client.get(); }
    public void setPhone_client(String phone_client) { this.phone_client.set(phone_client); }


    // =============================
    // JavaFX Property Getters
    // (Used by TableView)
    // =============================

    public IntegerProperty ref_clientProperty() { return ref_client; }
    public StringProperty nom_clientProperty() { return nom_client; }
    public StringProperty email_clientProperty() { return email_client; }
    public StringProperty address_clientProperty() { return address_client; }
    public StringProperty phone_clientProperty() { return phone_client; }


    // =============================
    // Business Logic (UNCHANGED)
    // =============================

    public List<Requests> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    public List<Vouchers> getVouchers() {
        return Collections.unmodifiableList(vouchers);
    }

    public Requests createRequest(int voucherCount, int duration) {

        Requests req = new Requests();
        req.ref_client = this.getRef_client();
        req.num_voucher = voucherCount;
        req.duration_voucher = duration;

        req.creation_date = new Date();

        // expiry = creation_date + duration days
        long expiryMillis = req.creation_date.getTime() + (duration * 24L * 60L * 60L * 1000L);
        req.expiry_voucher = new Date(expiryMillis);

        req.setStatus("Created");

        this.requests.add(req);

        // Attempt to generate vouchers (if implemented)
        try {
            List<Vouchers> generated = req.generateVouchers();
            if (generated != null && !generated.isEmpty()) {
                this.vouchers.addAll(generated);
            }
        } catch (Exception ignored) {}

        return req;
    }


    public void updateClientInfo(String name, String email, String address, String phone) {
        if (name != null) setNom_client(name);
        if (email != null) setEmail_client(email);
        if (address != null) setAddress_client(address);
        if (phone != null) setPhone_client(phone);
    }

    public List<Requests> viewRequests() {
        return Collections.unmodifiableList(this.requests);
    }

    public List<Vouchers> viewVouchers() {
        return Collections.unmodifiableList(this.vouchers);
    }

}
