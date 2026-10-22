package com.dragonroll;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.regex.Pattern;

/** Decides which ridden entities count as "dragons". Generic so it works with any dragon mod. */
public final class Mounts {
    private Mounts() {}

    public static boolean isEligible(Entity vehicle, Player rider) {
        if (!(vehicle instanceof LivingEntity) || vehicle instanceof Player) return false;
        if (vehicle.getPassengers().isEmpty() || vehicle.getPassengers().get(0) != rider) return false;
        ResourceLocation id = ForgeRegistries.ENTITIES.getKey(vehicle.getType());
        if (id == null) return false;
        String s = id.toString();
        if (matchesAny(Config.BLACKLIST.get(), s)) return false;
        List<? extends String> white = Config.WHITELIST.get();
        if (!white.isEmpty()) return matchesAny(white, s);
        return !Config.REQUIRE_NON_VANILLA.get() || !"minecraft".equals(id.getNamespace());
    }

    private static boolean matchesAny(List<? extends String> patterns, String id) {
        for (String p : patterns) {
            String regex = ("\\Q" + p.trim().toLowerCase() + "\\E").replace("*", "\\E.*\\Q");
            if (Pattern.matches(regex, id)) return true;
        }
        return false;
    }
}
