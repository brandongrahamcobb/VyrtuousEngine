---

# PatreonPlugin Plugin

## Overview

This plugin provides **server owners and developers** with a robust interface to manage player-related data stored in an external database. It acts as a **centralized data layer** in your Minecraft server, enabling seamless retrieval and synchronization of user information across platforms such as Discord, Patreon, and future OAuth providers.

## Purpose

Designed to support **multi-platform verification** and **activity tracking**, this plugin allows server administrators to:

- Map Minecraft accounts to external platform identities (Discord, Patreon, etc.).
- Fetch detailed user profiles, including balances, roles, and activity statuses.
- Prevent account duplication by consolidating user data.
- Expand to additional OAuth platforms in future releases.

## Core Features

- **User Management API:** Accessible through the plugin’s Java classes, offering methods to create, update, and fetch user records asynchronously.
- **Cross-Platform Identity:** Maintains a synchronized database that links Minecraft UUIDs with platform-specific IDs (e.g., Discord ID, Patreon ID).
- **Duplication Prevention:** Supports deduplication routines to ensure each user has a single, consolidated entry.
- **Extensible Data Model:** Designed to be expanded to include more OAuth providers and user data in the future.

## Available Data

The database currently stores the following user information:

| Field                       | Description                                                                               |
|------------------------------|------------------------------------------------------------------------------------------|
| `create_date`                | Timestamp of user creation in the database                                               |
| `discord_id`                 | Unique Discord account ID used for authentication                                        |
| `exp`                        | User experience points accumulated in the system                                         |
| `faction_name`               | User’s faction or group within the server                                                |
| `level`                      | User level or rank within the server                                                     |
| `minecraft_id`               | Unique UUID of the Minecraft player                                                      |
| `patreon_about`              | User's Patreon about/bio                                                                 |
| `patreon_amount_cents`       | Total amount pledged via Patreon (in cents)                                              |
| `patreon_email`              | Patreon account email                                                                    |
| `patreon_id`                 | Unique Patreon account ID                                                                |
| `patreon_name`               | Patreon account username                                                                 |
| `patreon_status`             | Patreon account status (e.g., 'active')                                                  |
| `patreon_tier`               | Patron tier level                                                                        |
| `patreon_vanity`             | Vanity URL or custom label for Patreon                                                   |

## Getting Started

### Cloning the Repository###
```bash
git clone https://github.com/brandongrahamcobb/jVyrtuous/PatreonPlugin.git
```

### Building the Plugin

Navigate into the cloned directory and run Maven to package the plugin:
```bash
mvn clean package
```
This will compile the code and create a Jar file named `PatreonPlugin-0.1.jar` in the `target/` directory.

### Installing in Minecraft Server

- Locate your server's `plugins/` folder.
- Copy the `target/PatreonPlugin-0.1.jar` file into `plugins/`.

Your server will automatically load the plugin when started or restarted.

---

## Configuration

The plugin's configuration can be set in one of two ways:

### 1. Using the Provided `config.yml` in the Plugin Folder

- Navigate to your server's folder.
- Inside `plugins/PatreonPlugin/`, replace or create the `config.yml` file with your custom settings.
- Restart the server to load the new configuration.

### 2. Using Environment Variables (Recommended)

- Set environment variables in your shell or via a `.bashrc` or `.zshrc` file:
```bash
export DISCORD_API_KEY=""
export POSTGRES_HOST="localhost"
export POSTGRES_DATABASE=""
export POSTGRES_USER=""
export POSTGRES_PASSWORD=""
export POSTGRES_PORT="5432"
export DISCORD_CLIENT_ID=""
export DISCORD_CLIENT_SECRET=""
export DISCORD_REDIRECT_URI=""
export DISCORD_ROLE_PASS=""
export DISCORD_TESTING_GUILD_ID=""
export PATREON_CLIENT_ID=""
export PATREON_CLIENT_SECRET=""
export PATREON_REDIRECT_URI=""
export SPARK_DISCORD_ENDPOINT=""
export SPARK_PATREON_ENDPOINT=""
export SPARK_PORT="8000"
export DISCORD_OWNER_ID=""
```
- To load the variables immediately without restarting the terminal:
```bash
source ~/.bashrc  # or ~/.zshrc
```
- The plugin will read these environment variables at startup, overriding values in `config.yml`.

---

## Notes

- Make sure your environment variables are correctly set before starting the server to ensure the plugin picks them up.
- Changes to `config.yml` require a server restart to take effect.
- Alternatively, editing the `config.yml` file directly in `plugins/PatreonPlugin/` allows quick configuration updates without messing with environment variables.

Certainly! Here's an example section you could add to your README, explaining how to run SQL scripts in PostgreSQL and how to create a database across different operating systems:

---

## Setting Up PostgreSQL Database

### Creating a Database

Before deploying the plugin, you need a PostgreSQL database instance. The process varies slightly depending on your operating system but follows similar principles.

#### Common Steps (All OSs)
1. **Install PostgreSQL** if not already installed.
2. **Create a user** with the necessary permissions.
3. **Create the database** for your plugin.

---

### For Linux / macOS / Windows (Command Line)

Once PostgreSQL is installed, you can create a new database with the `psql` command-line tool.

**Step-by-step:**

1. Open your terminal or command prompt.
2. Connect to PostgreSQL as the superuser (often `postgres`):

```bash
psql -U postgres
```
(You will be prompted for your password.)

3. Create a new database:
```sql
CREATE DATABASE <your-db>;
```
4. Create a user (if needed):
```sql
CREATE USER <your-user> WITH PASSWORD '<your-password>';
GRANT ALL PRIVILEGES ON DATABASE <your-db> TO <your-user>;
```
5. Exit psql:
```sql
\q
```
6. Update your plugin's configuration to connect to `lucy` with `lucy` and the password.

---

### Running a SQL Script File

If you have an SQL script containing your schema or initial data, you can run it like this:
```bash
psql -U <your-user> -d <your-db> -f file.sql
```

---

### Specific OS Notes:

- **Linux:**
  Use your distribution's package manager (e.g., `apt`, `yum`) to install PostgreSQL, then use `psql` as shown.
- **macOS:**
  Install via Homebrew (`brew install postgresql`), then start the service (`brew services start postgresql`).
- **Windows:**
  Use the [PostgreSQL installer](https://www.postgresql.org/download/windows/). After installation, use `psql` from the Command Prompt or PowerShell. You may need to add PostgreSQL's `bin` directory to your PATH.

---

### Final Tips:
- Make sure the PostgreSQL service is running before attempting to connect.
- Use a GUI client like pgAdmin for easier management if you prefer a graphical interface.
- Store your credentials securely and do not include passwords in scripts or config files in version-controlled repositories.

---

This guide helps users across all operating systems set up the necessary database environment for your plugin.
---

# Usage

---

## Developer API & Data Interaction

The plugin exposes a robust and flexible API designed for other developers and plugins to query and respond to user data stored within the system. The API provides asynchronous methods to access individual user properties, enabling non-blocking, thread-safe integrations with custom plugins or external systems.

### Accessing User Data

- **Retrieve a User Object:**
  Use the provided service to asynchronously fetch a user instance associated with a specific UUID. This user object serves as the gateway to all related data.

- **Get User Properties:**
  From the user object, individual properties such as experience points, faction name, level, and platform-specific IDs (e.g., Discord, Patreon) can be accessed through dedicated getter methods. These methods return `CompletableFuture` objects to allow asynchronous, non-blocking operations.

- **Modify User Data:**
  If supported, setter methods are available for updating user properties dynamically, with changes persisted seamlessly.

### Detecting Data Changes

- **Event Listeners:**
  The system provides a registration mechanism where external plugins can listen for specific user data updates, such as experience or faction changes. When a monitored property is modified, registered listeners are triggered with the new data, enabling real-time reactions.

- **Callback Registration:**
  For specific properties, developers can register callbacks that execute when data alterations occur, allowing dynamic updates of roles, ranks, or external markers in response to user activity.

### Usage Patterns

Developers can asynchronously fetch user information and set up change detection as follows:
```java
// Fetch user data for a specific UUID
CompletableFuture<User> userFuture = UserService.getInstance().getUserByUUID(playerUUID);
userFuture.thenAccept(user -> {
    // Retrieve individual properties asynchronously
    user.getExp().thenAccept(exp -> { /* process experience */ });
    user.getFactionName().thenAccept(faction -> { /* process faction */ });

    // Register for real-time change events
    user.registerChangeListener(new UserChangeListener() {
        @Override
        public void onExpChanged(UUID uuid, int newExp) { /* react to EXP change */ }
        @Override
        public void onFactionNameChanged(UUID uuid, String newFaction) { /* react to faction change */ }
    });
});
```
### Extensibility and Future Plans

The API is designed to support expansion, including integration with additional OAuth providers and custom user attributes. This modular approach allows plugins to stay in sync with evolving user data needs, reacting dynamically to user activity across multiple platforms.

---

**With this API, plugin developers can build rich, interactive features that respond to user data in real time, enhancing gameplay experience and community engagement.**
### For Server Owners

- Load the plugin and ensure it has access to the database.
- Use the API methods within plugin code to get user info, verify identities, or track activity.

---

## Future Expansion

The system is built with scalability in mind and is planning to include support for:

- Other OAuth providers (Twitch, Twitter, etc.)
- External APIs for activity feeds
- Advanced user analytics and roles management

---

## Licensing

This plugin is open-source under the GNU General Public License v3. For full license details, refer to the [LICENSE](https://www.gnu.org/licenses/gpl-3.0.html).

---

## Contact

- **Author:** Brandon G. Cobb  
- **GitHub:** [https://github.com/brandongrahamcobb](https://github.com/brandongrahamcobb)

---
