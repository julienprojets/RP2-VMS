package pkg.vms.model;

import javafx.beans.property.*;

public class Branch {

    private int branchId;
    private String location;
    private String responsibleUser;

    // Constructors
    public Branch() {
        // empty constructor
    }

    public Branch(int branchId, String location, String responsibleUser) {
        this.branchId = branchId;
        this.location = location;
        this.responsibleUser = responsibleUser;
    }

    // ===== JavaFX properties for TableView =====
    public IntegerProperty branchIdProperty() { return new SimpleIntegerProperty(branchId); }
    public StringProperty locationProperty() { return new SimpleStringProperty(location); }
    public StringProperty responsibleUserProperty() { return new SimpleStringProperty(responsibleUser); }

    // ===== Getters and Setters =====
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(String responsibleUser) { this.responsibleUser = responsibleUser; }
}
