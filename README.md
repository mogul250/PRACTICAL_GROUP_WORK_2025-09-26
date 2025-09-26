# Court Case Management System (CCMS)

A comprehensive Java-based application for managing court cases, hearings, judges, lawyers, and court proceedings. Built with Swing GUI and MySQL database.

## Group Members

1. **Heshima Herbert** - Reg No: 223008594
2. **Munezero Grace** - Reg No: 223009957
3. **Uwababyeyi Fabiola** - Reg No: 223006477

## Features

- **User Authentication & Role-Based Access Control**
  - Admin, Clerk, Judge, Lawyer, and Read-only user roles
  - Secure password hashing

- **Case Management**
  - Create and manage court cases
  - Track case status (Filed, Active, Stayed, Closed, Appealed)
  - Assign judges to cases

- **Court Entities Management**
  - Manage courthouses and court types
  - Judge profiles with specializations
  - Lawyer information with firm and license details
  - Person/organization records

- **Hearing Management**
  - Schedule and track court hearings
  - Record hearing outcomes and purposes
  - Assign presiding judges

- **Case Representation**
  - Link parties to cases (Plaintiff, Defendant, Witness, etc.)
  - Manage legal representation
  - Track primary and secondary representatives

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- MySQL Server 8.0 or higher
- MySQL Connector/J (included in `lib/` directory)

## Database Setup

1. Install MySQL Server on your system
2. Create a database named `court_db`
3. Import the database schema from `Database Dump/court_db.sql`

   ```sql
   mysql -u root -p court_db < "Database Dump/court_db.sql"
   ```

4. Update database credentials in `com/ccms/Database.java` if needed:
   - Default URL: `jdbc:mysql://localhost:3306/court_db`
   - Default User: `root`
   - Default Password: ``

## Installation & Setup

1. **Clone or download the project**
   ```bash
   cd /path/to/your/projects
   # Place the project files in the desired directory
   ```

2. **Compile the Java files**
   ```bash
   javac -cp "lib/mysql-connector-j-9.4.0.jar" com/ccms/*.java
   ```

3. **Run the application**
   ```bash
   java -cp ".:lib/mysql-connector-j-9.4.0.jar" com.ccms.Main
   ```

## Default Login Credentials

- **Email:** admin@ccms.com
- **Password:** 123456
- **Role:** Administrator

*Note: Change the default password after first login for security.*

## Project Structure

```
PRACTICAL_GROUP_WORK_2025-09-26/
├── com/ccms/
│   ├── Main.java                 # Application entry point
│   ├── Database.java             # Database connection and initialization
│   ├── LoginFrame.java           # Login interface
│   ├── AdminDashboardFrame.java  # Admin dashboard
│   ├── CasePanel.java            # Case management panel
│   ├── HearingPanel.java         # Hearing management panel
│   ├── JudgePanel.java           # Judge management panel
│   ├── LawyerPanel.java          # Lawyer management panel
│   ├── PersonPanel.java          # Person management panel
│   ├── CourthousePanel.java      # Courthouse management panel
│   ├── UserDAO.java              # User data access object
│   ├── PasswordUtil.java         # Password hashing utilities
│   └── *.java                    # Other supporting classes
├── lib/
│   └── mysql-connector-j-9.4.0.jar  # MySQL JDBC driver
├── Database Dump/
│   └── court_db.sql              # Database schema and sample data
└── README.md                     # This file
```

## Technologies Used

- **Java** - Core programming language
- **Swing** - GUI framework
- **MySQL** - Database management system
- **JDBC** - Database connectivity

## Contributing

This is a group project for academic purposes. For contributions or modifications:

1. Ensure database is properly set up
2. Test changes thoroughly
3. Update documentation as needed

## License

This project is developed for educational purposes as part of Year 3 Java Assignments.
