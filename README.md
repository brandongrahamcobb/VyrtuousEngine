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
