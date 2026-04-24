<?php
/**
 * Database and Table Creation Script for VMS (Voucher Management System)
 * PostgreSQL Database Setup
 */

// Database connection parameters
$host = 'to host always data postgrepsql';
$port = '5432---port mo pencE li pareil';
$dbname = 'couma to pou appel to db';
$user = 'user pour loging';
$password = 'passwordd';

// Connect to PostgreSQL server (without specifying database first)
try {
    $conn = new PDO("pgsql:host=$host;port=$port", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "Connected to PostgreSQL server successfully.\n";
} catch (PDOException $e) {
    die("Connection failed: " . $e->getMessage() . "\n");
}

// Create database if it doesn't exist
try {
    // Check if database exists
    $result = $conn->query("SELECT 1 FROM pg_database WHERE datname = '$dbname'");
    if ($result->rowCount() == 0) {
        // Note: PostgreSQL doesn't allow creating database in a transaction
        // We need to disconnect and reconnect
        $conn = null;
        $conn = new PDO("pgsql:host=$host;port=$port", $user, $password);
        $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $conn->exec("CREATE DATABASE $dbname");
        echo "Database '$dbname' created successfully.\n";
    } else {
        echo "Database '$dbname' already exists.\n";
    }
} catch (PDOException $e) {
    echo "Database creation check/creation: " . $e->getMessage() . "\n";
}

// Connect to the specific database
try {
    $conn = new PDO("pgsql:host=$host;port=$port;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "Connected to database '$dbname' successfully.\n\n";
} catch (PDOException $e) {
    die("Connection to database failed: " . $e->getMessage() . "\n");
}

// Array of SQL statements to create tables
$sqlStatements = [
    // Drop tables if they exist (in reverse order of dependencies)
    "DROP TABLE IF EXISTS vouchers CASCADE;",
    "DROP TABLE IF EXISTS requests CASCADE;",
    "DROP TABLE IF EXISTS clients CASCADE;",
    "DROP TABLE IF EXISTS users CASCADE;",
    "DROP TABLE IF EXISTS branch CASCADE;",
    
    // Create Users table
    "CREATE TABLE users (
        username VARCHAR(50) PRIMARY KEY,
        first_name_user VARCHAR(100) NOT NULL,
        last_name_user VARCHAR(100) NOT NULL,
        email_user VARCHAR(255) NOT NULL UNIQUE,
        role VARCHAR(50) NOT NULL,
        password VARCHAR(255) NOT NULL,
        ddl VARCHAR(255),
        titre VARCHAR(100),
        status VARCHAR(50) DEFAULT 'Active',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );",
    
    // Create Branch table
    "CREATE TABLE branch (
        branch_id SERIAL PRIMARY KEY,
        location VARCHAR(255) NOT NULL,
        responsible_user VARCHAR(50),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (responsible_user) REFERENCES users(username) ON DELETE SET NULL
    );",
    
    // Create Clients table
    "CREATE TABLE clients (
        ref_client SERIAL PRIMARY KEY,
        nom_client VARCHAR(255) NOT NULL,
        email_client VARCHAR(255) NOT NULL UNIQUE,
        address_client TEXT,
        phone_client VARCHAR(20),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );",
    
    // Create Requests table
    "CREATE TABLE requests (
        ref_request SERIAL PRIMARY KEY,
        ref_client INTEGER NOT NULL,
        creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        num_voucher INTEGER NOT NULL DEFAULT 0,
        val_voucher DECIMAL(10, 2),
        status VARCHAR(50) DEFAULT 'Created',
        payment VARCHAR(50),
        status_payment VARCHAR(50),
        date_payment TIMESTAMP,
        ref_payment INTEGER,
        date_approval TIMESTAMP,
        duration_voucher INTEGER,
        expiry_voucher TIMESTAMP,
        init_payment TIMESTAMP,
        proof_of_request TEXT,
        proof_file VARCHAR(255),
        processed_by VARCHAR(50),
        approved_by VARCHAR(50),
        validated_by VARCHAR(50),
        FOREIGN KEY (ref_client) REFERENCES clients(ref_client) ON DELETE CASCADE,
        FOREIGN KEY (processed_by) REFERENCES users(username) ON DELETE SET NULL,
        FOREIGN KEY (approved_by) REFERENCES users(username) ON DELETE SET NULL,
        FOREIGN KEY (validated_by) REFERENCES users(username) ON DELETE SET NULL
    );",
    
    // Create Vouchers table
    "CREATE TABLE vouchers (
        ref_voucher SERIAL PRIMARY KEY,
        ref_client INTEGER NOT NULL,
        ref_request INTEGER,
        code_voucher VARCHAR(100) NOT NULL UNIQUE,
        price DECIMAL(10, 2) NOT NULL,
        redeemed BOOLEAN DEFAULT FALSE,
        init_date TIMESTAMP,
        expiry_date TIMESTAMP,
        date_redeemed TIMESTAMP,
        bearer VARCHAR(255),
        status_voucher VARCHAR(50) DEFAULT 'Active',
        redeemed_by VARCHAR(50),
        redeemed_branch INTEGER,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (ref_client) REFERENCES clients(ref_client) ON DELETE CASCADE,
        FOREIGN KEY (ref_request) REFERENCES requests(ref_request) ON DELETE SET NULL,
        FOREIGN KEY (redeemed_branch) REFERENCES branch(branch_id) ON DELETE SET NULL
    );",
    
    // Create indexes for better performance
    "CREATE INDEX idx_requests_ref_client ON requests(ref_client);",
    "CREATE INDEX idx_requests_status ON requests(status);",
    "CREATE INDEX idx_vouchers_ref_client ON vouchers(ref_client);",
    "CREATE INDEX idx_vouchers_code ON vouchers(code_voucher);",
    "CREATE INDEX idx_vouchers_status ON vouchers(status_voucher);",
    "CREATE INDEX idx_clients_email ON clients(email_client);",
    "CREATE INDEX idx_users_email ON users(email_user);"
];

// Execute all SQL statements
echo "Creating tables...\n";
echo str_repeat("=", 50) . "\n";

foreach ($sqlStatements as $index => $sql) {
    try {
        $conn->exec($sql);
        if (strpos($sql, 'DROP TABLE') !== false) {
            // Extract table name from DROP statement
            if (preg_match('/DROP TABLE IF EXISTS (\w+)/', $sql, $matches)) {
                echo "Dropped table: {$matches[1]}\n";
            }
        } elseif (strpos($sql, 'CREATE TABLE') !== false) {
            // Extract table name from CREATE statement
            if (preg_match('/CREATE TABLE (\w+)/', $sql, $matches)) {
                echo "Created table: {$matches[1]}\n";
            }
        } elseif (strpos($sql, 'CREATE INDEX') !== false) {
            // Extract index name from CREATE INDEX statement
            if (preg_match('/CREATE INDEX (\w+)/', $sql, $matches)) {
                echo "Created index: {$matches[1]}\n";
            }
        }
    } catch (PDOException $e) {
        echo "Error executing statement " . ($index + 1) . ": " . $e->getMessage() . "\n";
        echo "SQL: " . substr($sql, 0, 100) . "...\n";
    }
}

echo str_repeat("=", 50) . "\n";
echo "\nDatabase setup completed successfully!\n\n";

// Display table information
echo "Database Tables Summary:\n";
echo str_repeat("-", 50) . "\n";

$tables = ['users', 'branch', 'clients', 'requests', 'vouchers'];
foreach ($tables as $table) {
    try {
        $stmt = $conn->query("SELECT COUNT(*) as count FROM $table");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        echo sprintf("%-15s: %d rows\n", ucfirst($table), $result['count']);
    } catch (PDOException $e) {
        echo sprintf("%-15s: Error - %s\n", ucfirst($table), $e->getMessage());
    }
}

echo "\n";
$conn = null;
echo "Connection closed.\n";
?>

