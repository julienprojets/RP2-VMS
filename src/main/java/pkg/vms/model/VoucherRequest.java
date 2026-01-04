package pkg.vms.model;

import javafx.beans.property.*;
import java.sql.Timestamp;

/**
 * Model for Voucher Request
 */
public class VoucherRequest {
    
    private final IntegerProperty requestId;
    private final StringProperty requestReference;
    private final IntegerProperty refClient;
    private final StringProperty clientName;
    private final IntegerProperty numVouchers;
    private final DoubleProperty unitValue;
    private final DoubleProperty totalValue;
    private final StringProperty status;
    private final StringProperty paymentStatus;
    private final ObjectProperty<Timestamp> paymentDate;
    private final StringProperty approvedBy;
    private final ObjectProperty<Timestamp> approvalDate;
    private final StringProperty processedBy;
    private final ObjectProperty<Timestamp> createdAt;
    private final ObjectProperty<Timestamp> updatedAt;
    private final ObjectProperty<java.sql.Date> expirationDate;
    
    public VoucherRequest() {
        this.requestId = new SimpleIntegerProperty();
        this.requestReference = new SimpleStringProperty();
        this.refClient = new SimpleIntegerProperty();
        this.clientName = new SimpleStringProperty();
        this.numVouchers = new SimpleIntegerProperty();
        this.unitValue = new SimpleDoubleProperty();
        this.totalValue = new SimpleDoubleProperty();
        this.status = new SimpleStringProperty();
        this.paymentStatus = new SimpleStringProperty();
        this.paymentDate = new SimpleObjectProperty<>();
        this.approvedBy = new SimpleStringProperty();
        this.approvalDate = new SimpleObjectProperty<>();
        this.processedBy = new SimpleStringProperty();
        this.createdAt = new SimpleObjectProperty<>();
        this.updatedAt = new SimpleObjectProperty<>();
        this.expirationDate = new SimpleObjectProperty<>();
    }
    
    // Getters and Setters
    public int getRequestId() { return requestId.get(); }
    public void setRequestId(int value) { requestId.set(value); }
    public IntegerProperty requestIdProperty() { return requestId; }
    
    public String getRequestReference() { return requestReference.get(); }
    public void setRequestReference(String value) { requestReference.set(value); }
    public StringProperty requestReferenceProperty() { return requestReference; }
    
    public int getRefClient() { return refClient.get(); }
    public void setRefClient(int value) { refClient.set(value); }
    public IntegerProperty refClientProperty() { return refClient; }
    
    public String getClientName() { return clientName.get(); }
    public void setClientName(String value) { clientName.set(value); }
    public StringProperty clientNameProperty() { return clientName; }
    
    public int getNumVouchers() { return numVouchers.get(); }
    public void setNumVouchers(int value) { numVouchers.set(value); }
    public IntegerProperty numVouchersProperty() { return numVouchers; }
    
    public double getUnitValue() { return unitValue.get(); }
    public void setUnitValue(double value) { unitValue.set(value); }
    public DoubleProperty unitValueProperty() { return unitValue; }
    
    public double getTotalValue() { return totalValue.get(); }
    public void setTotalValue(double value) { totalValue.set(value); }
    public DoubleProperty totalValueProperty() { return totalValue; }
    
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }
    
    public String getPaymentStatus() { return paymentStatus.get(); }
    public void setPaymentStatus(String value) { paymentStatus.set(value); }
    public StringProperty paymentStatusProperty() { return paymentStatus; }
    
    public Timestamp getPaymentDate() { return paymentDate.get(); }
    public void setPaymentDate(Timestamp value) { paymentDate.set(value); }
    public ObjectProperty<Timestamp> paymentDateProperty() { return paymentDate; }
    
    public String getApprovedBy() { return approvedBy.get(); }
    public void setApprovedBy(String value) { approvedBy.set(value); }
    public StringProperty approvedByProperty() { return approvedBy; }
    
    public Timestamp getApprovalDate() { return approvalDate.get(); }
    public void setApprovalDate(Timestamp value) { approvalDate.set(value); }
    public ObjectProperty<Timestamp> approvalDateProperty() { return approvalDate; }
    
    public String getProcessedBy() { return processedBy.get(); }
    public void setProcessedBy(String value) { processedBy.set(value); }
    public StringProperty processedByProperty() { return processedBy; }
    
    public Timestamp getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(Timestamp value) { createdAt.set(value); }
    public ObjectProperty<Timestamp> createdAtProperty() { return createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt.get(); }
    public void setUpdatedAt(Timestamp value) { updatedAt.set(value); }
    public ObjectProperty<Timestamp> updatedAtProperty() { return updatedAt; }
    
    public java.sql.Date getExpirationDate() { return expirationDate.get(); }
    public void setExpirationDate(java.sql.Date value) { expirationDate.set(value); }
    public ObjectProperty<java.sql.Date> expirationDateProperty() { return expirationDate; }
}

