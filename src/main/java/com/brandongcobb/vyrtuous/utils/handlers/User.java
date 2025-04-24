package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import java.sql.Timestamp;

public interface User {
   void createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity, Runnable callback);
}
