package pkg.vms.model;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

public class Users {
    private final StringProperty username;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty email;
    private final StringProperty role;
    private final StringProperty password;
    private final StringProperty ddl;
    private final StringProperty titre;
    private final StringProperty status;

    // Constructors
    public Users() {
        this("", "", "", "", "", "", "", "", "");
    }

    public Users(String username, String firstName, String lastName, String email, String role,
                 String password, String ddl, String titre, String status) {
        this.username = new SimpleStringProperty(username);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.email = new SimpleStringProperty(email);
        this.role = new SimpleStringProperty(role);
        this.password = new SimpleStringProperty(password);
        this.ddl = new SimpleStringProperty(ddl);
        this.titre = new SimpleStringProperty(titre);
        this.status = new SimpleStringProperty(status);
    }

    // ===== JavaFX properties for TableView =====
    public StringProperty usernameProperty() { return username; }
    public StringProperty firstNameProperty() { return firstName; }
    public StringProperty lastNameProperty() { return lastName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty roleProperty() { return role; }
    public StringProperty statusProperty() { return status; }
    public StringProperty titreProperty() { return titre; }

    // ===== Getters and Setters =====
    public String getUsername() { return username.get(); }
    public void setUsername(String username) { this.username.set(username); }

    public String getFirstName() { return firstName.get(); }
    public void setFirstName(String firstName) { this.firstName.set(firstName); }

    public String getLastName() { return lastName.get(); }
    public void setLastName(String lastName) { this.lastName.set(lastName); }

    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }

    public String getRole() { return role.get(); }
    public void setRole(String role) { this.role.set(role); }

    public String getPassword() { return password.get(); }
    public void setPassword(String password) { this.password.set(password); }

    public String getDdl() { return ddl.get(); }
    public void setDdl(String ddl) { this.ddl.set(ddl); }

    public String getTitre() { return titre.get(); }
    public void setTitre(String titre) { this.titre.set(titre); }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }

}