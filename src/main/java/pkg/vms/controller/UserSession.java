package pkg.vms.controller;

/**
 * Singleton class to store current user session information
 */
public class UserSession {
    private static UserSession instance;
    private String username;
    private String role;

    private UserSession() {
        // Private constructor to prevent instantiation
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setUser(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isSuperuser() {
        return role != null && role.toLowerCase().trim().equals("superuser");
    }

    public void clear() {
        this.username = null;
        this.role = null;
    }
}

