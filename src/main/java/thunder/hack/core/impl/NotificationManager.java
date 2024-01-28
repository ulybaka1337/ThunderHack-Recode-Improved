package thunder.hack.core.impl;

import net.minecraft.client.gui.DrawContext;
import thunder.hack.ThunderHack;
import thunder.hack.core.IManager;
import thunder.hack.core.impl.ModuleManager;
import thunder.hack.gui.notification.Notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager implements IManager {
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    {
        ThunderHack.EVENT_BUS.subscribe(this);
    }

    public void publicity(String title, String content, int second, Notification.Type type) {
        notifications.add(new Notification(title, content, type, second * 1000));
    }

    public void onRender2D(DrawContext event) {
        if (!ModuleManager.notifications.isEnabled()) return;
        if (notifications.size() > 8)
            notifications.remove(0);
        float startY = mc.getWindow().getScaledHeight() - 36f;

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            notifications.removeIf(Notification::shouldDelete);
            notification.render(event.getMatrices(), startY);
            startY -= (float) (notification.getHeight() + 3);
        }
    }

    public void onRenderShader(DrawContext context) {
        if (!ModuleManager.notifications.isEnabled()) return;
        float startY = mc.getWindow().getScaledHeight() - 36f;

        for (Notification notification : notifications) {
            notification.renderShaders(context.getMatrices(), startY);
            startY -= (float) (notification.getHeight() + 3);
        }
    }
}