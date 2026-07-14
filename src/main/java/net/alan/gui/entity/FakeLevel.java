package net.alan.gui.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class FakeLevel extends ClientLevel {

    private static FakeLevel INSTANCE;

    private FakeLevel(RegistryAccess registryAccess) {
        super(
            createPacketListener(registryAccess),
            new ClientLevelData(Difficulty.NORMAL, false, false),
            Level.OVERWORLD,
            OVERWORLD_DIMENSION_TYPE,
            2, 2,
            () -> Minecraft.getInstance().getProfiler(),
            Minecraft.getInstance().levelRenderer,
            false, 0
        );
    }

    private static final Holder<DimensionType> OVERWORLD_DIMENSION_TYPE = Holder.direct(new DimensionType(
        OptionalLong.empty(),
        true,
        false,
        false,
        true,
        1.0,
        true,
        false,
        -64,
        384,
        384,
        TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("infiniburn_overworld")),
        ResourceLocation.withDefaultNamespace("overworld"),
        0.0f,
        new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
    ));

    private static ClientPacketListener createPacketListener(RegistryAccess registryAccess) {
        Minecraft mc = Minecraft.getInstance();
        RegistryAccess.Frozen frozen = registryAccess instanceof RegistryAccess.Frozen
            ? (RegistryAccess.Frozen) registryAccess
            : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

        return new ClientPacketListener(
            mc,
            new Connection(PacketFlow.CLIENTBOUND),
            new CommonListenerCookie(
                mc.getGameProfile(),
                null,
                frozen,
                FeatureFlagSet.of(),
                null, null, null,
                Map.of(),
                null,
                false,
                Map.of(),
                ServerLinks.EMPTY,
                ConnectionType.OTHER
            )
        );
    }

    public static FakeLevel getInstance() {
        if (INSTANCE == null) {
            RegistryAccess ra = getRegistryAccess();
            if (ra == null) return null;
            INSTANCE = new FakeLevel(ra);
        }
        return INSTANCE;
    }

    @Override
    public long getGameTime() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.getGameTime();
        }
        return (long) mc.getTimer().getGameTimeDeltaTicks();
    }

    @Override
    public long getDayTime() {
        return getGameTime();
    }

    private static RegistryAccess getRegistryAccess() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return mc.level.registryAccess();
        if (mc.getConnection() != null) return mc.getConnection().registryAccess();
        return createFakeRegistryAccess();
    }

    private static RegistryAccess.Frozen createFakeRegistryAccess() {
        MappedRegistry<DamageType> damageTypeRegistry = new MappedRegistry<>(
            Registries.DAMAGE_TYPE, Lifecycle.stable()
        );

        DamageType dummyType = new DamageType(
            "generic", DamageScaling.NEVER, 0.0F, DamageEffects.HURT, DeathMessageType.DEFAULT
        );

        registerDamageType(damageTypeRegistry, DamageTypes.IN_FIRE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.CAMPFIRE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.LIGHTNING_BOLT, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.ON_FIRE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.LAVA, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.HOT_FLOOR, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.IN_WALL, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.CRAMMING, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.DROWN, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.STARVE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.CACTUS, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FALL, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FLY_INTO_WALL, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FELL_OUT_OF_WORLD, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.GENERIC, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.MAGIC, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.WITHER, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.DRAGON_BREATH, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.DRY_OUT, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.SWEET_BERRY_BUSH, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FREEZE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.STALAGMITE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FALLING_BLOCK, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FALLING_ANVIL, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FALLING_STALACTITE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.STING, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.MOB_ATTACK, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.MOB_ATTACK_NO_AGGRO, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.PLAYER_ATTACK, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.ARROW, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.TRIDENT, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.MOB_PROJECTILE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.SPIT, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.WIND_CHARGE, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FIREWORKS, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.FIREBALL, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.UNATTRIBUTED_FIREBALL, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.WITHER_SKULL, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.THROWN, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.INDIRECT_MAGIC, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.THORNS, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.EXPLOSION, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.PLAYER_EXPLOSION, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.SONIC_BOOM, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.BAD_RESPAWN_POINT, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.OUTSIDE_BORDER, dummyType);
        registerDamageType(damageTypeRegistry, DamageTypes.GENERIC_KILL, dummyType);

        damageTypeRegistry.freeze();

        MappedRegistry<Biome> biomeRegistry = new MappedRegistry<>(
            Registries.BIOME, Lifecycle.stable()
        );

        Biome plainBiome = new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.8F)
            .downfall(0.4F)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .fogColor(0xC0D8FF)
                .waterColor(0x3F76E4)
                .waterFogColor(0x050533)
                .skyColor(0x78A7FF)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .temperatureAdjustment(Biome.TemperatureModifier.NONE)
            .build();

        Registry.register(biomeRegistry, Biomes.PLAINS.location(), plainBiome);
        biomeRegistry.freeze();

        MappedRegistry<DimensionType> dimensionTypeRegistry = new MappedRegistry<>(
            Registries.DIMENSION_TYPE, Lifecycle.stable()
        );
        Registry.register(dimensionTypeRegistry, Level.OVERWORLD.location(), OVERWORLD_DIMENSION_TYPE.value());
        dimensionTypeRegistry.freeze();

        Map<ResourceKey<? extends Registry<?>>, Registry<?>> registryMap = new HashMap<>();
        for (var entry : BuiltInRegistries.REGISTRY.entrySet()) {
            @SuppressWarnings("unchecked")
            ResourceKey<? extends Registry<?>> key = (ResourceKey<? extends Registry<?>>) entry.getKey();
            registryMap.put(key, entry.getValue());
        }
        registryMap.put(Registries.DAMAGE_TYPE, damageTypeRegistry);
        registryMap.put(Registries.BIOME, biomeRegistry);
        registryMap.put(Registries.DIMENSION_TYPE, dimensionTypeRegistry);

        return new RegistryAccess.ImmutableRegistryAccess(registryMap).freeze();
    }

    private static void registerDamageType(
        MappedRegistry<DamageType> registry, ResourceKey<DamageType> key, DamageType value
    ) {
        Registry.register(registry, key.location(), value);
    }

    public FakePlayer createPlayer() {
        GameProfile profile = Minecraft.getInstance().getGameProfile();
        if (profile == null) return null;
        return new FakePlayer(this, profile);
    }
}