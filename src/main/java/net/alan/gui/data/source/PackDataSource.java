package net.alan.gui.data.source;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public class PackDataSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackDataSource.class);
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png");
    private static final Map<String, PackDataSource> INSTANCES = new HashMap<>();

    private final PackRepository repository;
    private PackSelectionModel model;
    private final Map<String, ResourceLocation> iconCache = new HashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final String id;
    private Watcher watcher;
    private long ticksToReload;

    public static PackDataSource getOrCreate(String sourceId) {
        PackDataSource existing = INSTANCES.get(sourceId);
        if (existing != null) return existing;

        Minecraft minecraft = Minecraft.getInstance();
        PackRepository repository = minecraft.getResourcePackRepository();
        repository.reload();
        PackDataSource source = new PackDataSource(sourceId, repository,
            repo -> minecraft.options.updateResourcePacks(repo));
        source.watcher = Watcher.create(minecraft.getResourcePackDirectory());
        INSTANCES.put(sourceId, source);
        return source;
    }

    public static void commitAll() {
        for (PackDataSource source : INSTANCES.values()) {
            try {
                source.commit();
            } catch (Exception e) {
                LOGGER.error("Failed to commit pack source '{}'", source.getId(), e);
            }
        }
    }

    public static void clearAll() {
        for (PackDataSource source : INSTANCES.values()) {
            source.closeWatcher();
        }
        INSTANCES.clear();
    }

    private PackDataSource(String id, PackRepository repository, Consumer<PackRepository> output) {
        this.id = id;
        this.repository = repository;
        this.model = new PackSelectionModel(this::notifyListeners, this::getPackIcon, repository, output);
    }

    public String getId() {
        return id;
    }

    public PackSelectionModel getModel() {
        return model;
    }

    public PackRepository getRepository() {
        return repository;
    }

    public void reload() {
        model.findNewPacks();
        iconCache.clear();
        notifyListeners();
    }

    public void commit() {
        model.commit();
    }

    public void tick() {
        if (watcher != null) {
            try {
                if (watcher.pollForChanges()) {
                    ticksToReload = 20L;
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to poll for directory changes, stopping watcher", e);
                closeWatcher();
            }
        }
        if (ticksToReload > 0L && --ticksToReload == 0L) {
            reload();
        }
    }

    private void closeWatcher() {
        if (watcher != null) {
            try {
                watcher.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close watcher", e);
            }
            watcher = null;
        }
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public List<PackEntryData> getActivePacks() {
        List<PackEntryData> result = new ArrayList<>();
        model.getSelected().forEach(entry -> result.add(new PackEntryData(entry)));
        return result;
    }

    public List<PackEntryData> getUnactivePacks() {
        List<PackEntryData> result = new ArrayList<>();
        model.getUnselected().forEach(entry -> result.add(new PackEntryData(entry)));
        return result;
    }

    public List<PackEntryData> getAllPacks() {
        List<PackEntryData> result = new ArrayList<>();
        model.getSelected().forEach(entry -> result.add(new PackEntryData(entry)));
        model.getUnselected().forEach(entry -> result.add(new PackEntryData(entry)));
        return result;
    }

    public PackSelectionModel.Entry getEntry(String packId) {
        for (PackSelectionModel.Entry entry : model.getSelected().toList()) {
            if (entry.getId().equals(packId)) return entry;
        }
        for (PackSelectionModel.Entry entry : model.getUnselected().toList()) {
            if (entry.getId().equals(packId)) return entry;
        }
        return null;
    }

    private ResourceLocation getPackIcon(Pack pack) {
        return iconCache.computeIfAbsent(pack.getId(), p -> loadPackIcon(Minecraft.getInstance().getTextureManager(), pack));
    }

    private ResourceLocation loadPackIcon(TextureManager textureManager, Pack pack) {
        try {
            try (PackResources packResources = pack.open()) {
                IoSupplier<InputStream> supplier = packResources.getRootResource("pack.png");
                if (supplier == null) {
                    return DEFAULT_ICON;
                }
                String s = pack.getId();
                ResourceLocation loc = ResourceLocation.withDefaultNamespace(
                    "pack/" + Util.sanitizeName(s, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(s) + "/icon"
                );
                try (InputStream inputStream = supplier.get()) {
                    NativeImage nativeImage = NativeImage.read(inputStream);
                    textureManager.register(loc, new DynamicTexture(nativeImage));
                    return loc;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load icon from pack {}", pack.getId(), e);
            return DEFAULT_ICON;
        }
    }

    public static class PackEntryData {
        private final String id;
        private final String title;
        private final String description;
        private final String extendedDescription;
        private final ResourceLocation iconTexture;
        private final boolean isActive;
        private final boolean isCompatible;
        private final String compatibilityDescription;
        private final String compatibilityConfirmation;
        private final boolean isRequired;
        private final boolean isFixedPosition;
        private final boolean canActivate;
        private final boolean canDeactivate;
        private final boolean canMoveUp;
        private final boolean canMoveDown;
        private final String sourceLabel;

        public PackEntryData(PackSelectionModel.Entry entry) {
            this.id = entry.getId();
            this.title = entry.getTitle().getString();
            this.description = entry.getDescription().getString();
            this.extendedDescription = entry.getExtendedDescription().getString();
            this.iconTexture = entry.getIconTexture();
            this.isActive = entry.isSelected();
            this.isCompatible = entry.getCompatibility().isCompatible();
            this.compatibilityDescription = entry.getCompatibility().getDescription().getString();
            this.compatibilityConfirmation = entry.getCompatibility().getConfirmation().getString();
            this.isRequired = entry.isRequired();
            this.isFixedPosition = entry.isFixedPosition();
            this.canActivate = entry.canSelect();
            this.canDeactivate = entry.canUnselect();
            this.canMoveUp = entry.canMoveUp();
            this.canMoveDown = entry.canMoveDown();
            this.sourceLabel = getSourceLabel(entry.getPackSource());
        }

        private static String getSourceLabel(PackSource source) {
            if (source == PackSource.BUILT_IN) return "Built-in";
            if (source == PackSource.WORLD) return "World";
            if (source == PackSource.SERVER) return "Server";
            if (source == PackSource.FEATURE) return "Feature";
            return "";
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getExtendedDescription() { return extendedDescription; }
        public ResourceLocation getIconTexture() { return iconTexture; }
        public boolean isActive() { return isActive; }
        public boolean isCompatible() { return isCompatible; }
        public String getCompatibilityDescription() { return compatibilityDescription; }
        public String getCompatibilityConfirmation() { return compatibilityConfirmation; }
        public boolean isRequired() { return isRequired; }
        public boolean isFixedPosition() { return isFixedPosition; }
        public boolean canActivate() { return canActivate; }
        public boolean canDeactivate() { return canDeactivate; }
        public boolean canMoveUp() { return canMoveUp; }
        public boolean canMoveDown() { return canMoveDown; }
        public String getSourceLabel() { return sourceLabel; }
    }

    static class Watcher implements AutoCloseable {
        private final WatchService watchService;
        private final Path packPath;

        Watcher(Path packPath) throws IOException {
            this.packPath = packPath;
            this.watchService = packPath.getFileSystem().newWatchService();
            try {
                watchDir(packPath);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(packPath)) {
                    for (Path path : stream) {
                        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                            watchDir(path);
                        }
                    }
                }
            } catch (Exception e) {
                this.watchService.close();
                throw e;
            }
        }

        @javax.annotation.Nullable
        static Watcher create(Path packPath) {
            try {
                return new Watcher(packPath);
            } catch (IOException e) {
                LOGGER.warn("Failed to initialize pack directory monitoring", e);
                return null;
            }
        }

        private void watchDir(Path path) throws IOException {
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        }

        boolean pollForChanges() throws IOException {
            boolean changed = false;
            WatchKey key;
            while ((key = watchService.poll()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    changed = true;
                    if (key.watchable() == this.packPath
                        && event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path path = this.packPath.resolve((Path) event.context());
                        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                            watchDir(path);
                        }
                    }
                }
                key.reset();
            }
            return changed;
        }

        @Override
        public void close() throws IOException {
            watchService.close();
        }
    }
}