package com.brandongcobb.oauthplugin.utils.handlers;

import com.brandongcobb.oauthplugin.utils.handlers.*;
import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

/**
 * In-memory representation of a user record.
 */
public class UserImpl implements User {

    private final long discordId;
    private final String minecraftId;
    private final String patreonAbout;
    private final int patreonAmountCents;
    private final String patreonEmail;
    private final long patreonId;
    private final String patreonName;
    private final String patreonStatus;
    private final String patreonTier;
    private final String patreonVanity;
    private final Timestamp timestamp;

    public UserImpl(Timestamp timestamp,
                    long discordId,
                    String minecraftId,
                    String patreonAbout,
                    int patreonAmountCents,
                    String patreonEmail,
                    long patreonId,
                    String patreonName,
                    String patreonStatus,
                    String patreonTier,
                    String patreonVanity) {
        this.timestamp = timestamp;
        this.discordId = discordId;
        this.minecraftId = minecraftId;
        this.patreonAbout = patreonAbout;
        this.patreonAmountCents = patreonAmountCents;
        this.patreonEmail = patreonEmail;
        this.patreonId = patreonId;
        this.patreonName = patreonName;
        this.patreonStatus = patreonStatus;
        this.patreonTier = patreonTier;
        this.patreonVanity = patreonVanity;
    }

    public long getDiscordId() { return discordId; }

    public String getMinecraftId() { return minecraftId; }

    public long getPatreonId() { return patreonId; }

    public String getPatreonTier() { return patreonTier; }

    public Timestamp getTimestamp() { return timestamp; }

    public String getPatreonAbout() { return patreonAbout; }

    public int getPatreonAmountCents() { return patreonAmountCents; }

    public String getPatreonEmail() { return patreonEmail; }

    public String getPatreonName() { return patreonName; }

    public String getPatreonStatus() { return patreonStatus; }

    public String getPatreonVanity() { return patreonVanity; }
    /**
     * Persist this user record using the UserManager.
     */
    @Override
    public CompletableFuture<Void> createUser(
            Timestamp timestamp,
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
    ) {
        UserManager um = new UserManager(Database.completeGetInstance());
        return um.createUser(
            timestamp,
            discordId,
            minecraftId,
            patreonAbout,
            patreonAmountCents,
            patreonEmail,
            patreonId,
            patreonName,
            patreonStatus,
            patreonTier,
            patreonVanity
        );
    }
}
