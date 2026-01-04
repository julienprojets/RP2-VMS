# VMS - Voucher Management System

A comprehensive JavaFX-based desktop application for managing vouchers, clients, requests, and redemptions. The system includes QR code generation, PDF voucher printing, email notifications, and a mobile-friendly web interface for voucher redemption via QR code scanning.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [Usage Guide](#usage-guide)
- [Project Structure](#project-structure)
- [Configuration](#configuration)

## Features

### Core Modules

1. **User Management**
   - User authentication with role-based access control (Admin, Manager, Staff)
   - User creation, editing, and management
   - Secure login system

2. **Client Management**
   - Add, edit, and manage client information
   - Client search and filtering
   - Client-voucher relationship tracking

3. **Voucher Management**
   - Create vouchers individually or in bulk
   - Voucher status management (Active, Reserved, Redeemed, Expired)
   - QR code generation for each voucher
   - PDF generation for vouchers
   - Voucher search and filtering

4. **Request Management**
   - Create voucher requests for clients
   - Request workflow: Draft → Paid → Approved → Generated → Dispatched
   - Automatic voucher generation from approved requests
   - PDF summary generation for requests
   - Email notifications for voucher dispatch

5. **Branch Management**
   - Manage company branches
   - Branch assignment for users
   - Branch-based voucher redemption tracking

6. **Reports & Analytics**
   - Comprehensive voucher statistics
   - Status distribution charts (Pie Chart)
   - Redemptions over time (Bar Chart)
   - Filterable reports by status, date range, and search terms
   - Excel/CSV export functionality

7. **QR Code Redemption Server**
   - Built-in HTTP server for mobile voucher redemption
   - Camera-based QR code scanning on mobile devices
   - Manual voucher code entry option
   - Real-time voucher validation
   - Network-accessible redemption interface (port 8080)

### Key Capabilities

- **QR Code Generation**: Automatic QR code generation for vouchers using ZXing library
- **PDF Generation**: Professional PDF vouchers with QR codes using Apache PDFBox
- **Email Integration**: SMTP email support for sending voucher PDFs to clients
- **Database Auto-Schema**: Automatic database schema creation and updates
- **Performance Optimized**: Background threading for data loading to prevent UI freezing
- **Role-Based Access**: Different access levels for different user roles
- **Audit Logging**: Tracks system activities and changes

## Technologies Used

- **Java**: JDK 21
- **JavaFX**: 22 (UI framework)
- **PostgreSQL**: Database (remote PostgreSQL server)
- **Maven**: Build tool and dependency management
- **Libraries**:
  - Apache PDFBox 3.0.1 (PDF generation)
  - ZXing 3.5.2 (QR code generation)
  - JavaMail 1.6.2 (Email functionality)
  - Apache POI 5.2.5 (Excel export)
  - ControlsFX 11.2.1 (UI components)
  - FormsFX 11.6.0 (Form components)

## Prerequisites

Before running the application, ensure you have:

1. **Java Development Kit (JDK) 21** or higher
   - Download from: https://adoptium.net/ or Oracle JDK
   - Verify installation: `java -version`

2. **Maven 3.6+**
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`

3. **PostgreSQL Database**
   - Remote PostgreSQL database (configured in code)
   - Or local PostgreSQL installation
   - Database connection details (see Configuration section)

4. **Network Access**
   - For redemption server functionality (mobile QR scanning)
   - Port 8080 should be available and accessible on your network

5. **Email Account** (Optional, for email notifications)
   - Gmail account with App Password (recommended)
   - Or any SMTP server credentials

## Setup Instructions

### 1. Database Configuration

The application uses PostgreSQL database. Update the database connection settings in:

**File**: `src/main/java/pkg/vms/DBconnection/DBconnection.java`

```java
private static final String URL = "jdbc:postgresql://your-host:5432/your-database";
private static final String USER = "your-username";
private static final String PASS = "your-password";
```

**Note**: The database schema is automatically created on first run. The application will create all necessary tables using the `DatabaseSchema` utility class.

If you need to manually create the database, you can use the `dbtablecreate.php` script as a reference (update it with your database credentials).

### 2. Email Configuration (Optional)

To enable email notifications for voucher dispatch:

**File**: `email_config.properties`

```properties
smtp.host=smtp.gmail.com
smtp.port=587
smtp.user=your-email@gmail.com
smtp.password=your-app-password
```

**For Gmail:**
1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password: https://myaccount.google.com/apppasswords
3. Use the generated app password in the config file

**Note**: Email configuration can also be set through the application's Email Configuration dialog (if available in your version).

### 3. Build the Project

1. Clone or download the project
2. Open terminal/command prompt in the project root directory
3. Build the project:

```bash
mvn clean compile
```

This will download all dependencies and compile the project.

## Running the Application

### Option 1: Using Maven (Recommended)

```bash
mvn clean javafx:run
```

### Option 2: Using IDE (IntelliJ IDEA, Eclipse, etc.)

1. Import the project as a Maven project
2. Navigate to: `src/main/java/pkg/vms/HelloApplication.java`
3. Run the `main` method

### Option 3: Build and Run JAR

```bash
# Build executable JAR
mvn clean package

# Run the JAR
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar target/VMS-1.0-SNAPSHOT.jar
```

## Usage Guide

### Initial Setup

1. **First Login**: 
   - The application starts with a login screen
   - Default credentials may need to be set up in the database
   - Create an admin user through database or initial setup

2. **Dashboard**:
   - After login, you'll see the main dashboard
   - Navigation menu on the left provides access to all modules
   - The redemption server starts automatically (check console for server URL)

### Managing Clients

1. Navigate to **Clients** from the sidebar
2. Click **Add Client** to create new clients
3. Fill in client details (name, contact information, etc.)
4. Use search to find existing clients
5. Click **Edit** or **Delete** to manage clients

### Creating Vouchers

1. Navigate to **Vouchers** from the sidebar
2. Click **Add Voucher** button
3. Enter voucher details:
   - Voucher Code (or leave blank for auto-generation)
   - Quantity (for bulk creation)
   - Status: Active or Reserved (only these options available for new vouchers)
   - Init Date and Expiry Date
4. Click **Save** to create vouchers
5. Vouchers will automatically have QR codes generated

### Creating Requests

1. Navigate to **Requests** from the sidebar
2. Click **New Request** button
3. Select a client and fill in request details
4. Specify number of vouchers and value
5. Save as Draft, mark as Paid, then Approve
6. Once approved, click **Generate Vouchers** to create vouchers
7. Click **Generate Summary PDF** to create a summary document
8. Use **Email Vouchers** to send PDFs via email (if configured)

### Redeeming Vouchers (Mobile)

1. Ensure the application is running (redemption server starts automatically)
2. Check the console output for the server URL (e.g., `http://192.168.1.100:8080`)
3. On your mobile device:
   - Ensure your phone is on the same Wi-Fi network as the computer
   - Open a web browser on your phone
   - Navigate to the server URL shown in the console
4. On the redemption page:
   - **Option 1**: Scan the QR code using the camera button
   - **Option 2**: Enter the voucher code manually
5. Fill in Branch and Redeemed By fields
6. Click **Redeem Voucher** to complete redemption

**Troubleshooting Mobile Access**:
- Ensure both devices are on the same network
- Check Windows Firewall allows port 8080
- Try using the local IP address instead of localhost
- Check the console for any error messages

### Reports & Analytics

1. Navigate to **Reports & Analytics** from the sidebar
2. View statistics cards: Total Vouchers, Redeemed, Active, Expired, Total Value
3. Use filters:
   - Status filter dropdown
   - Search field (voucher code or client name)
   - Date range pickers (Start Date and End Date)
4. View charts:
   - Pie Chart: Voucher Status Distribution
   - Bar Chart: Redemptions Over Time
5. Export data: Click **Export Excel** to save reports as CSV

### User Management

1. Navigate to **Users** from the sidebar
2. Add new users with roles: Admin, Manager, or Staff
3. Assign users to branches
4. Edit or delete existing users

### Branch Management

1. Navigate to **Branches** from the sidebar
2. Add, edit, or manage company branches
3. Assign branches to users
4. Track voucher redemptions by branch

## Project Structure

```
VMS/
├── src/
│   └── main/
│       ├── java/
│       │   ├── pkg/vms/
│       │   │   ├── controller/          # JavaFX controllers
│       │   │   │   ├── BranchController.java
│       │   │   │   ├── ClientsController.java
│       │   │   │   ├── DashboardController.java
│       │   │   │   ├── LoginController.java
│       │   │   │   ├── ReportsController.java
│       │   │   │   ├── RequestsController.java
│       │   │   │   ├── UsersController.java
│       │   │   │   ├── VouchersController.java
│       │   │   │   └── UserSession.java
│       │   │   ├── DAO/                 # Data Access Objects
│       │   │   │   ├── ClientsDAO.java
│       │   │   │   ├── RequestDAO.java
│       │   │   │   ├── UsersDAO.java
│       │   │   │   └── VoucherDAO.java
│       │   │   ├── DBconnection/        # Database connection
│       │   │   │   └── DBconnection.java
│       │   │   ├── model/               # Data models
│       │   │   │   ├── Branch.java
│       │   │   │   ├── Clients.java
│       │   │   │   ├── Requests.java
│       │   │   │   ├── Users.java
│       │   │   │   ├── VoucherRequest.java
│       │   │   │   └── Vouchers.java
│       │   │   ├── util/                # Utility classes
│       │   │   │   ├── AuditLogger.java
│       │   │   │   ├── DatabaseSchema.java
│       │   │   │   ├── EmailConfig.java
│       │   │   │   ├── EmailService.java
│       │   │   │   ├── PDFGenerator.java
│       │   │   │   ├── QRCodeGenerator.java
│       │   │   │   └── RedemptionServer.java
│       │   │   └── HelloApplication.java # Main entry point
│       │   └── module-info.java
│       └── resources/
│           └── pkg/vms/
│               ├── css/                 # Stylesheets
│               │   ├── dashboard.css
│               │   ├── login.css
│               │   ├── reports.css
│               │   └── ...
│               └── fxml/                # FXML UI files
│                   ├── Dashboard.fxml
│                   ├── loginpage.fxml
│                   ├── reports.fxml
│                   └── ...
├── vouchers/                            # Generated voucher PDFs
├── pom.xml                              # Maven configuration
├── email_config.properties              # Email settings
└── README.md                            # This file
```

## Configuration

### Database Connection

Edit `src/main/java/pkg/vms/DBconnection/DBconnection.java` to configure database connection.

### Email Settings

Edit `email_config.properties` in the project root for email configuration.

### Redemption Server Port

The redemption server runs on port **8080** by default. To change this, modify the `port` variable in `RedemptionServer.java` (before starting the server).

### PDF Output Directory

PDF vouchers are saved in the `vouchers/` directory in the project root. This is hardcoded in `PDFGenerator.java`.

## Troubleshooting

### Database Connection Issues

- Verify PostgreSQL server is running and accessible
- Check database credentials in `DBconnection.java`
- Ensure database server allows remote connections (if using remote DB)
- Check firewall settings

### Redemption Server Not Accessible from Mobile

- Ensure both devices are on the same Wi-Fi network
- Check Windows Firewall: Allow port 8080
- Verify the IP address shown in console matches your computer's local IP
- Try disabling antivirus/firewall temporarily to test

### Email Not Sending

- Verify email credentials in `email_config.properties`
- For Gmail: Use App Password (not regular password)
- Check SMTP server settings
- Verify internet connection

### Application Won't Start

- Verify JDK 21 is installed: `java -version`
- Check Maven dependencies: `mvn dependency:resolve`
- Review console output for error messages
- Ensure database is accessible

### UI Freezing/Slow Loading

- The application uses background threading for data loading
- If issues persist, check database connection performance
- Review console for any error messages

## License

This project is proprietary software. All rights reserved.

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review console output for error messages
3. Verify all configuration settings
4. Check database connection and schema

---

**Version**: 1.0  
**Last Updated**: December 2025

