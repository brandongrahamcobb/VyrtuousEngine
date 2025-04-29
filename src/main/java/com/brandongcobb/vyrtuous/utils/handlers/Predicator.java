/*  Predicator.java The purpose of this program is to run lambda functions before executing code on the main thread.
 *  Copyright (C) 2024  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.bots.DiscordBot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
//import org.javacord.api.DiscordApi;
//import org.javacord.api.entity.channel.PrivateChannel;
//import org.javacord.api.entity.permission.Role;
//import org.javacord.api.entity.server.Server;
//import org.javacord.api.entity.channel.ServerTextChannel;
//import org.javacord.api.entity.user.User;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;

public class Predicator {

    private Vyrtuous app;
    private DiscordBot bot;

    public Predicator(Vyrtuous application) {
        this.app = application;
        this.bot = app.discordBot;
    }

//    public boolean atHome(Server server) {
    public boolean atHome(Guild guild) {
//        return server != null && server.getId() == (long) ConfigManager.getConfigValue("discord_testing_guild_id");
        return guild != null && Long.parseLong(guild.getId()) == (long) ConfigManager.getConfigValue("discord_testing_guild_id");
    }

//    public boolean releaseMode(User user, ServerTextChannel channel) {
    public boolean releaseMode(User user, TextChannel channel) {
//        return (Long) user.getId() == ConfigManager.getConfigValue("discord_owner_id") || // Your developer ID
        return String.valueOf(user.getIdLong()).equals(ConfigManager.getConfigValue("discord_owner_id")) || // Your developer ID
               (boolean) ConfigManager.getConfigValue("discord_release_mode") ||
               (channel instanceof PrivateChannel);
    }

    public static boolean isDeveloper(User user) {
        return user != null && String.valueOf(user.getIdLong()).equals(ConfigManager.getLongValue("discord_owner_id"));
    }

    public boolean isVeganUser(User user) {
//        List<Long> serverIds = (List<Long>) ConfigManager.getConfigValue("discord_testing_guild_ids");
//        for (Long serverId : serverIds) {
//            Server server = getServerById(serverId);
//            if (server != null) {
//                Optional<Role> veganRoleOpt = server.getRoles().stream()
        List<Long> guildIds = (List<Long>) ConfigManager.getConfigValue("discord_testing_guild_ids");
        for (Long guildId : guildIds) {
            Guild guild = getGuildById(guildId);
            Member member = guild.getMember(user);
            if (guild != null) {
                Optional<Role> veganRoleOpt = guild.getRoles().stream()
                    .filter(role -> role.getName().equals("vegan"))
                    .findFirst();
                if (veganRoleOpt.isPresent()) {
                    Role veganRole = veganRoleOpt.get();
//                    if (veganRole.getUsers().contains(user)) {
                    if (guild.getMembersWithRoles(veganRole).contains(member)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

//    public Server getServerById(long serverId) {
//        Set<Server> servers = bot.getApi().getServers(); // Get all servers
//        for (Server server : servers) {
//            if (server.getId() == serverId) {
//                return server; // Return the server wrapped in an Optional
    public Guild getGuildById(long guildId) {
        List<Guild> guilds = bot.getApi().getGuilds(); // Get all guilds
        for (Guild guild : guilds) {
            if (Long.parseLong(guild.getId()) == guildId) {
                return guild; // Return the guild wrapped in an Optional
            }
        }
        return null;
    }

//    public boolean isReleaseMode(ServerTextChannel channel, User user) {
    public boolean isReleaseMode(TextChannel channel, User user) {
        return String.valueOf(user.getId()).equals(ConfigManager.getConfigValue("discord_owner_id")) || // Your developer ID
               (boolean) ConfigManager.getConfigValue("discord_release_mode") ||
               (channel instanceof PrivateChannel);
    }
}
