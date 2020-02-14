/*
 * Copyright 2019 ProtectionStones team and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package dev.espi.protectionstones.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.espi.protectionstones.*;
import dev.espi.protectionstones.utils.WGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;

class ArgAdminChangeblock {

    // /ps admin changeblock [world] [fromblockalias] [toblockalias]
    static boolean argumentAdminChangeblock(CommandSender p, String[] args) {
        if (args.length < 5) {
            PSL.msg(p, ArgAdmin.CHANGEBLOCK_HELP);
            return true;
        }

        String world = args[2], fromBlockAlias = args[3], toBlockAlias = args[4];
        if (ProtectionStones.getProtectBlockFromAlias(fromBlockAlias) == null) {
            PSL.msg(p, ChatColor.GRAY + "The type to change from is not a registered protection block!");
            return true;
        }
        if (ProtectionStones.getProtectBlockFromAlias(toBlockAlias) == null) {
            PSL.msg(p, ChatColor.GRAY + "The type to change to is not a registered protection block!");
            return true;
        }

        String fromBlock = ProtectionStones.getProtectBlockFromAlias(fromBlockAlias).type,
                toBlock = ProtectionStones.getProtectBlockFromAlias(toBlockAlias).type;

        Consumer<PSRegion> convertFunction = (region) -> {
            if (region.getType().equals(fromBlock)) {
                p.sendMessage(ChatColor.GRAY + "Changing " + region.getID() + "...");

                region.setType(ProtectionStones.getBlockOptions(toBlock));
            }
        };

        World w = Bukkit.getWorld(world);
        if (w == null) {
            PSL.msg(p, ChatColor.GRAY + "The world is not valid!");
            return true;
        }
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null) {
            return PSL.msg(p, ChatColor.GRAY + "The world does not have WorldGuard configured!");
        }
        for (ProtectedRegion r : rgm.getRegions().values()) {
            if (ProtectionStones.isPSRegion(r)) {
                PSRegion pr = PSRegion.fromWGRegion(w, r);

                convertFunction.accept(pr);

                if (pr instanceof PSGroupRegion) {
                    for (PSMergedRegion psmr : ((PSGroupRegion) pr).getMergedRegions()) {
                        convertFunction.accept(psmr);
                    }
                }
            }
        }

        p.sendMessage(ChatColor.GRAY + "Done!");

        return true;
    }
}
