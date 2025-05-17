OAuthPlugin README
==================

Overview
--------
OAuthPlugin is a Bukkit/Spigot plugin that lets you link Minecraft accounts to Discord and Patreon via OAuth2.  It stores per‐user data in PostgreSQL and exposes in‐memory APIs so other plugins or server‐side code can act on linked user profiles (Discord ID, Patreon tier, pledge amount, etc).  

Currently supported flows:
- Discord (identify scope)  
- Patreon (identity + memberships)  

More OAuth providers (Twitch, LinkedIn, OpenAI…) coming soon.  

Installation
------------
1. Build or download `OAuthPlugin.jar` and copy it into your server’s `plugins/` folder.  
2. Ensure you have a running PostgreSQL server and that its daemon/​service is started.  
3. Start your Minecraft server once.  The plugin will:
   - Create `plugins/OAuthPlugin/config.yml` from the bundled template.  
   - Connect (via JDBC/HikariCP) to Postgres, creating the database (if missing) and `users` table.  

4. Stop the server, edit `plugins/OAuthPlugin/config.yml`, fill in your credentials and endpoints (see Configuration below), then restart.

Configuration
-------------
Open and edit `plugins/OAuthPlugin/config.yml`.  Key values:

  • PostgreSQL  
    • POSTGRES_HOST, POSTGRES_PORT  
    • POSTGRES_DATABASE, POSTGRES_USER, POSTGRES_PASSWORD  

  • Discord OAuth  
    • ENABLE_DISCORD: true/false  
    • DISCORD_CLIENT_ID, DISCORD_CLIENT_SECRET  
    • DISCORD_REDIRECT_URI  (must match your app settings)  

  • Patreon OAuth  
    • ENABLE_PATREON: true/false  
    • PATREON_CLIENT_ID, PATREON_CLIENT_SECRET  
    • PATREON_REDIRECT_URI  (must match your app settings)  

  • Spark Java HTTP server (embedded)  
    • SPARK_PORT          e.g. 8000  
    • SPARK_DISCORD_ENDPOINT: “/oauth/discord_callback”  
    • SPARK_PATREON_ENDPOINT: “/oauth/patreon_callback”  

You can also override any of these via environment variables (upper‐case matching the above keys) or a global `~/.config/oauthplugin/config.yml`.

OAuth Flow
----------
1. In‐game a player runs:
   - `/discord`  → plugin stores a short‐lived session and sends them a Discord OAuth URL  
   - OR `/patreon`  → sends them a Patreon OAuth URL  

2. Player visits the URL in a browser; after granting permission, the OAuth provider will redirect to:
   ```
   http://<your-host>:<SPARK_PORT><SPARK_<PROVIDER>_ENDPOINT>?code=AUTH_CODE&state=PLAYER_UUID
   ```
   The embedded Spark server captures `code` and associates it with the in‐memory session.

3. Back in‐game, player runs:
   ```
   /code AUTH_CODE
   ```
   Plugin exchanges `AUTH_CODE` for an access token, fetches profile info, upserts a record in `users`, and confirms linking.

Running Spark behind ngrok or a remote server
---------------------------------------------
If your Minecraft server isn’t publicly reachable on port 8000, you can:

• Use ngrok:  
  ```
  ngrok http 8000
  ```  
  Copy the forwarding URL (e.g. `https://abcd1234.ngrok.io`), set your OAuth app redirect URIs to:
  ```
  https://abcd1234.ngrok.io/oauth/discord_callback
  https://abcd1234.ngrok.io/oauth/patreon_callback
  ```
  And in `config.yml`, set:
  ```
  SPARK_PORT: 8000
  SPARK_DISCORD_ENDPOINT: /oauth/discord_callback
  SPARK_PATREON_ENDPOINT: /oauth/patreon_callback
  ```

• Remote server / VPS:  
  – Deploy your JAR and Spark server on a public IP or Docker host.  
  – Configure DNS or port‐forwarding so that `<your-domain>:8000` reaches your Spark HTTP endpoints.

Maven/Gradle Dependency
-----------------------
If you’re writing another plugin or app that wants to depend on OAuthPlugin’s API:

Maven:
```xml
<dependency>
  <groupId>com.brandongcobb.oauthplugin</groupId>
  <artifactId>OAuthPlugin</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle:
```groovy
repositories {
  mavenCentral()
  // or your internal repo
}
dependencies {
  implementation 'com.brandongcobb.oauthplugin:OAuthPlugin:1.0.0'
}
```

Usage (In-Game Commands)
------------------------
• `/discord`  
  Starts a Discord OAuth session, sends the player an authorization URL.  

• `/patreon`  
  Starts a Patreon OAuth session, sends player an authorization URL.  

• `/code <AUTH_CODE>`  
  Completes whichever pending session (Discord or Patreon), exchanges the code, stores the link, and loads the user data.

Programmatic API (utils/handlers)
---------------------------------
Once users are linked, you can retrieve and act on their data via the in‐memory handlers and User objects.

1. Database instance (singleton):
   ```
   Database db = Database.completeGetInstance();
   ```

2. UserManager  
   ```
   UserManager um = new UserManager(db);
   ```
   • createUser(...)       — upsert a full user record  
   • consolidateUsers()    — merge duplicate rows in the database  
   • cacheUser(User)       — cache a User instance in memory  
   • getUserByDiscordId(long) → User  
   • getUserByPatreonId(long) → User  
   • getUserByMinecraftId(String) → User  
   • getAllUsers()         → Map<Long, User>  

3. User implementations and interfaces:

   interface User  
     • CompletableFuture<Void> createUser(Timestamp ts, long discordId, String mcUuid, String about,…)

   class UserImpl implements User  
     • getDiscordId(), getMinecraftId(), getPatreonId(), getPatreonAbout(),  
       getPatreonAmountCents(), getPatreonEmail(), getPatreonName(), getPatreonStatus(), getPatreonTier(), getPatreonVanity(), getTimestamp()

   class MinecraftUser implements User  
     • getMinecraftId()  
     • userExists(String mcUuid, Consumer<Boolean> callback)

   class DiscordUser  
     • DiscordUser(String accessToken)  
     • long getDiscordId()  
     • userExists(long discordId, Consumer<Boolean> callback)

   class OAuthService  (Patreon profile fetcher)  
     • int    getPatreonAmountCents(String accessToken)  
     • String getPatreonAbout(String accessToken)  
     • String getPatreonEmail(String accessToken)  
     • long   getPatreonId(String accessToken)  
     • String getPatreonName(String accessToken)  
     • String getPatreonStatus(String accessToken)  
     • String getPatreonTier(String accessToken)  
     • String getPatreonVanity(String accessToken)  

4. Sessions  
   class OAuthUserSession  
     • getPlayerUuid(), getCommand() (“discord”/“patreon”), getAccessToken(), setAccessToken(...)

Example: fetch a User by Discord ID and print their Patreon tier
```
Database db = Database.completeGetInstance();
UserManager um = new UserManager(db);

// after server startup & caching
User u = um.getUserByDiscordId(123456789012345678L);
if (u != null) {
  System.out.println("Discord user linked to Patreon tier: " + u.getPatreonTier());
}
```

Support & Contribution
----------------------
Contributions welcome via GitHub.  Please file issues for bugs or feature requests.  

License
-------
GNU General Public License v3.0
# OAuthPlugin

## Overview

**OAuthPlugin** is a Bukkit/Spigot plugin that links Minecraft accounts with external OAuth providers (currently Discord and Patreon), stores user profiles in PostgreSQL, and exposes an in-memory API for other plugins or server-side code to retrieve and act on user data.

## Purpose

Designed for **server owners and developers**, OAuthPlugin:

- Maps Minecraft UUIDs to platform identities (Discord, Patreon).
- Fetches and persists user details (e.g. pledge amount, account status).
- Prevents duplicate records via consolidation routines.
- Provides asynchronous, thread-safe APIs for integration with custom plugins.

## Core Features

- **OAuth Flows**  
  - `/discord` → Discord OAuth2 (identify scope)  
  - `/patreon` → Patreon OAuth2 (identity + memberships)  
  - `/code <AUTH_CODE>` → Finalize and store link  

- **User Management API**  
  - Asynchronous methods to create, fetch, cache, and consolidate users  
  - In-memory caching for low-latency lookups  

- **Data Storage**  
  - PostgreSQL via HikariCP connection pool  
  - Automatic database and `users` table creation  

- **Extensible Architecture**  
  - Pluggable to add more OAuth providers (Twitch, LinkedIn, etc.)  
  - Future support for custom user attributes, analytics, event listeners  

## Available Data

| Column                  | Description                                      |
|-------------------------|--------------------------------------------------|
| `id`                    | Internal primary key (serial)                    |
| `create_date`           | Timestamp of record creation                     |
| `discord_id`            | Linked Discord account ID (unique)               |
| `minecraft_id`          | Minecraft player UUID                            |
| `patreon_about`         | Patreon “about” text                             |
| `patreon_amount_cents`  | Total pledge amount in cents                     |
| `patreon_email`         | Patreon account email                            |
| `patreon_id`            | Patreon account ID                               |
| `patreon_name`          | Patreon username                                 |
| `patreon_status`        | Patreon status (e.g. “active”)                   |
| `patreon_tier`          | Patreon tier identifier                          |
| `patreon_vanity`        | Vanity URL or custom label                       |

## Getting Started

### 1. Clone & Build

```bash
git clone https://github.com/brandongrahamcobb/oauthplugin.git
cd oauthplugin
mvn clean package
```

The built JAR will be in `target/OAuthPlugin-<version>.jar`.

### 2. Install

1. Copy the JAR into your server’s `plugins/` folder.
2. Ensure PostgreSQL is installed and its service/daemon is running.
3. Start the Minecraft server once to generate the default `config.yml` and initialize the database.
4. Stop the server, edit `plugins/OAuthPlugin/config.yml` as described below, then restart.

## Configuration

### `plugins/OAuthPlugin/config.yml`

Fill in your credentials and endpoints:

```yaml
# PostgreSQL settings
POSTGRES_HOST: "localhost"
POSTGRES_PORT: "5432"
POSTGRES_DATABASE: "oauthplugin_db"
POSTGRES_USER: "oauth_user"
POSTGRES_PASSWORD: "secret"

# Enable providers
ENABLE_DISCORD: true
ENABLE_PATREON: true

# Discord OAuth
DISCORD_CLIENT_ID: "<your-client-id>"
DISCORD_CLIENT_SECRET: "<your-client-secret>"
DISCORD_REDIRECT_URI: "https://your-domain.com/oauth/discord_callback"

# Patreon OAuth
PATREON_CLIENT_ID: "<your-client-id>"
PATREON_CLIENT_SECRET: "<your-client-secret>"
PATREON_REDIRECT_URI: "https://your-domain.com/oauth/patreon_callback"

# Spark (embedded HTTP server)
SPARK_PORT: 8000
SPARK_DISCORD_ENDPOINT: "/oauth/discord_callback"
SPARK_PATREON_ENDPOINT: "/oauth/patreon_callback"
```

### Environment Variables

All config keys above can be overridden by environment variables of the same name.

## Setting Up PostgreSQL

1. Install PostgreSQL (your OS package manager or installer).  
2. Create a database and user:

   ```sql
   -- Connect as superuser:
   psql -U postgres

   -- Create DB and user:
   CREATE DATABASE oauthplugin_db;
   CREATE USER oauth_user WITH PASSWORD 'secret';
   GRANT ALL PRIVILEGES ON DATABASE oauthplugin_db TO oauth_user;
   \q
   ```

3. Alternatively, run a SQL script:

   ```bash
   psql -U oauth_user -d oauthplugin_db -f schema.sql
   ```

## OAuth Flow

1. **In-game** `/discord` or `/patreon`  
   → Player receives an OAuth2 authorization URL with state=`<player-uuid>`.

2. **Browser** → Player grants access → Redirect to  
   `http(s)://<host>:<SPARK_PORT><SPARK_<PROVIDER>_ENDPOINT>?code=AUTH_CODE&state=PLAYER_UUID`

3. **In-game** `/code AUTH_CODE`  
   → Plugin exchanges code for access token, fetches profile, upserts `users` table.

## Running Spark Behind ngrok or Remote Host

- **ngrok**:  
  ```bash
  ngrok http 8000
  ```
  Copy the HTTPS forwarding URL into your OAuth app redirect settings and `config.yml`.

- **Remote Server**:  
  Expose port 8000 via firewall or Docker, set your domain and update redirect URIs accordingly.

## In-Game Commands

- `/discord`  
- `/patreon`  
- `/code <AUTH_CODE>`

## Maven/Gradle Dependency

To use OAuthPlugin’s API in your own plugin:

**Maven**
```xml
<dependency>
  <groupId>com.brandongcobb.oauthplugin</groupId>
  <artifactId>OAuthPlugin</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Gradle**
```groovy
repositories { mavenCentral() }
dependencies {
  implementation 'com.brandongcobb.oauthplugin:OAuthPlugin:1.0.0'
}
```

## Developer API & Data Interaction

### Database & UserManager

```java
Database db = Database.completeGetInstance();
UserManager um = new UserManager(db);
```

- `createUser(...)` → Asynchronously upsert a user record.  
- `cacheUser(User)` → Cache a User instance for quick lookups.  
- `getUserByDiscordId(long)` / `getUserByPatreonId(long)` / `getUserByMinecraftId(String)` → Retrieve cached or return `null`.  
- `consolidateUsers()` → Merge duplicate database rows.

### User Interfaces

#### interface User
```java
CompletableFuture<Void> createUser(
    Timestamp ts,
    long discordId,
    String minecraftId,
    String patreonAbout,
    int patreonAmountCents,
    String patreonEmail,
    long patreonId,
    String patreonName,
    String patreonStatus,
    String patreonTier,
    String patreonVanity
);
```

#### class UserImpl implements User
Getters:
- `getDiscordId()`, `getMinecraftId()`, `getPatreonId()`, `getPatreonAbout()`, `getPatreonAmountCents()`, etc.

#### class DiscordUser
```java
DiscordUser d = new DiscordUser(accessToken);
long discordId = d.getDiscordId();
d.userExists(discordId, exists -> { … });
```

#### class MinecraftUser implements User
```java
MinecraftUser m = new MinecraftUser(uuid);
m.userExists(uuid, exists -> { … });
```

#### class OAuthService (Patreon)
- `getPatreonAbout(accessToken)`
- `getPatreonAmountCents(accessToken)`
- `getPatreonEmail(accessToken)`
- `getPatreonId(accessToken)`
- `getPatreonName(accessToken)`
- `getPatreonStatus(accessToken)`
- `getPatreonTier(accessToken)`
- `getPatreonVanity(accessToken)`

#### class OAuthUserSession
Tracks pending sessions in `OAuthPlugin.getInstance().getSessions()`:
- `getCommand()` returns `"discord"` or `"patreon"`.
- `getAccessToken()` stores the OAuth code until `/code` is executed.

### Example

```java
// After users have linked accounts and sessions are cached:
User u = um.getUserByDiscordId(123456789012345678L);
if (u != null) {
  System.out.println("Patreon Tier: " + u.getPatreonTier());
}
```

## Extensibility & Future Plans

- Add Twitch, LinkedIn, OpenAI OAuth flows  
- Support event listeners for real-time data change notifications  
- Advanced analytics and role management

## License

GNU General Public License v3.0 — see LICENSE or https://www.gnu.org/licenses/gpl-3.0.html

## Contact

**Author:** Brandon G. Cobb
**GitHub:** https://github.com/brandongrahamcobb/oauthplugin
