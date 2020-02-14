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

package dev.espi.protectionstones.utils;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.espi.protectionstones.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class LimitUtil {

    public static boolean check(Player p, PSProtectBlock b) {
        if (!p.hasPermission("protectionstones.admin")) {
            // check if player has limit on protection stones
            String msg = LimitUtil.hasPlayerPassedRegionLimit(p, b);
            if (!msg.isEmpty()) {
                PSL.msg(p, msg);
                return false;
            }
        }

        return true;
    }

    public static boolean hasPassedOrEqualsRentLimit(Player p) {
        int lim = MiscUtil.getPermissionNumber(p, "protectionstones.rent.limit.", -1);
        if (lim != -1) {
            int total = 0;

            // find total number of rented regions
            HashMap<World, RegionManager> m = WGUtils.getAllRegionManagers();
            for (World w : m.keySet()) {
                RegionManager rgm = m.get(w);
                for (ProtectedRegion r : rgm.getRegions().values()) {
                    if (ProtectionStones.isPSRegion(r) && r.getOwners().contains(WorldGuardPlugin.inst().wrapPlayer(p))) {
                        PSRegion psr = PSRegion.fromWGRegion(p.getWorld(), r);

                        if (psr != null && psr.getTenant() != null && psr.getTenant().equals(p.getUniqueId())) total++;
                    }
                }
            }

            return total >= lim;
        }
        return false;
    }

    private static String hasPlayerPassedRegionLimit(Player p, PSProtectBlock b) {
        PSPlayer psp = PSPlayer.fromPlayer(p);
        HashMap<PSProtectBlock, Integer> regionLimits = psp.getRegionLimits();
        int maxPS = psp.getGlobalRegionLimits();

        if (maxPS != -1 || !regionLimits.isEmpty()) { // only check if limit was found

            // count player's protection stones
            int total = 0, bFound = 0;
            HashMap<World, RegionManager> m = WGUtils.getAllRegionManagers();
            for (World w : m.keySet()) {
                RegionManager rgm = m.get(w);
                for (ProtectedRegion r : rgm.getRegions().values()) {
                    if (ProtectionStones.isPSRegion(r) && r.getOwners().contains(WorldGuardPlugin.inst().wrapPlayer(p))) {
                        PSRegion psr = PSRegion.fromWGRegion(p.getWorld(), r);

                        if (psr instanceof PSGroupRegion) {
                            for (PSMergedRegion psmr : ((PSGroupRegion) psr).getMergedRegions()) {
                                total++;
                                if (psmr.getType().equals(b.type)) bFound++; // if the specific block was found
                            }
                        } else {
                            total++;
                            if (psr.getType().equals(b.type)) bFound++; // if the specific block was found
                        }
                    }
                }
            }

            //Bukkit.getLogger().info("TOTAL: " + total + ", FOUND: " + bFound); // TODO

            // check if player has passed region limit
            if (total >= maxPS && maxPS != -1) {
                return PSL.REACHED_REGION_LIMIT.msg().replace("%limit%", ""+maxPS);
            }

            // check if player has passed per block limit
            if (regionLimits.get(b) != null && bFound >= regionLimits.get(b)) {
                return PSL.REACHED_PER_BLOCK_REGION_LIMIT.msg().replace("%limit%", ""+regionLimits.get(b));
            }
        }
        return "";
    }

}
