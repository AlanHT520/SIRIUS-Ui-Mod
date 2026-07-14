package net.alan.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * PackSelectionScreen 的包装器，修复 onClose 不返回父屏幕的问题
 * 
 * 问题：原版 PackSelectionScreen.onClose() 只提交了资源包更改并关闭了文件监视器，
 * 但没有调用 minecraft.setScreen() 返回父屏幕，导致点击"完成"按钮无效。
 * 
 * 解决方案：继承 PackSelectionScreen，重写 onClose() 方法，
 * 在调用 super.onClose() 后返回父屏幕。
 */
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
        // 执行原版的提交和清理逻辑
        super.onClose();
        
        // 修复：返回父屏幕
        if (this.parentScreen != null) {
            this.minecraft.setScreen(this.parentScreen);
        } else {
            // 如果没有父屏幕，返回游戏
            this.minecraft.setScreen(null);
        }
    }
}