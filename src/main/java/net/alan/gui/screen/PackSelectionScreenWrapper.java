

package net.alan.gui.screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.function.Consumer;
public class PackSelectionScreenWrapper extends PackSelectionScreen {
    @Nullable
    private final Screen parentScreen;
    private final Minecraft minecraft;
    public PackSelectionScreenWrapper(
            @Nullable Screen parentScreen,
            PackRepository repository,
            Consumer<PackRepository> output,
            Path packDir,
            Component title
    ) {
        super(repository, output, packDir, title);
        this.parentScreen = parentScreen;
        this.minecraft = Minecraft.getInstance();
    }
    @Override
    public void onClose() {
        super.onClose();
        if (this.parentScreen != null) {
            this.minecraft.setScreen(this.parentScreen);
        } else {
            this.minecraft.setScreen(null);
        }
    }
}
