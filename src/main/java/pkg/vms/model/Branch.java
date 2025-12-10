package pkg.vms.model;

import javafx.beans.property.*;

public class Branch {

    private IntegerProperty branchId;
    private StringProperty location;
    private StringProperty responsibleUser;
    private StringProperty company;
    private StringProperty phone;
    private StringProperty industry;

    // Constructors
    public Branch() {
        this(0, "", "", "", "", "");
    }

    public Branch(int branchId, String location, String responsibleUser, String company, String phone, String industry) {
        this.branchId = new SimpleIntegerProperty(branchId);
        this.location = new SimpleStringProperty(location);
        this.responsibleUser = new SimpleStringProperty(responsibleUser);
        this.company = new SimpleStringProperty(company);
        this.phone = new SimpleStringProperty(phone);
        this.industry = new SimpleStringProperty(industry);
    }

    // ===== JavaFX properties for TableView =====
    public IntegerProperty branchIdProperty() { return branchId; }
    public StringProperty locationProperty() { return location; }
    public StringProperty responsibleUserProperty() { return responsibleUser; }
    public StringProperty companyProperty() { return company; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty industryProperty() { return industry; }

    // ===== Getters and Setters =====
    public int getBranchId() { return branchId.get(); }
    public void setBranchId(int branchId) { this.branchId.set(branchId); }

    public String getLocation() { return location.get(); }
    public void setLocation(String location) { this.location.set(location); }

    public String getResponsibleUser() { return responsibleUser.get(); }
    public void setResponsibleUser(String responsibleUser) { this.responsibleUser.set(responsibleUser); }
    
    public String getCompany() { return company.get(); }
    public void setCompany(String company) { this.company.set(company); }
    
    public String getPhone() { return phone.get(); }
    public void setPhone(String phone) { this.phone.set(phone); }
    
    public String getIndustry() { return industry.get(); }
    public void setIndustry(String industry) { this.industry.set(industry); }
}
