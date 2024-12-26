package org.lwjglb.engine;

import org.lwjglb.engine.graph.Render;
import org.lwjglb.engine.scene.Scene;

public class Engine {

    public static final int TARGET_UPS = 30;
    private final IAppLogic appLogic;
    private final Window window;
    private Render render;
    private boolean running;
    private Scene scene;
    private int targetFps;
    private int targetUps;

    public Engine(String windowTitle, Window.WindowOptions opts, IAppLogic appLogic) {
        window = new Window(windowTitle, opts, () -> {
            resize();
            return null;
        });
        targetFps = opts.fps;
        targetUps = opts.ups;
        this.appLogic = appLogic;
        render = new Render();
        scene = new Scene();
        appLogic.init(window, scene, render);
        running = true;
    }

    private void cleanup() {
        appLogic.cleanup();
        render.cleanup();
        scene.cleanup();
        window.cleanup();
    }

    private void resize() {
        // Nothing to be done yet
    }

    private void run() {
        long initialTime = System.currentTimeMillis();
        float timeU = 1000.0f / targetUps;
        float timeR = targetFps > 0 ? 1000.0f / targetFps : 0;
        float deltaUpdate = 0;
        float deltaFps = 0;

        long updateTime = initialTime;
        while (running && !window.windowShouldClose()) {
            window.pollEvents();

            long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) // 代表这次 while 循环消耗的时间
                    / timeU; // 将 deltaUpdate 加上此次消耗的时间除以一次 Update 应当消耗的时间
            deltaFps += (now - initialTime) / timeR;

            // 判断 targetFps <= 0 是为了处理一种特殊情况，即当 targetFps 被设置为 0 或负数时，游戏引擎将不限制帧率。
            // 这意味着渲染和输入处理将尽可能频繁地进行，而不受帧率的限制。
            // 这样可以确保在想要的时候能够以最高的帧率运行。
            if (targetFps <= 0 || deltaFps >= 1) {
                appLogic.input(window, scene, now - initialTime);
            }

            if (deltaUpdate >= 1) { // 如果 deltaUpdate 大于等于 1，说明应该进行一次 Update
                long diffTimeMillis = now - updateTime;
                appLogic.update(window, scene, diffTimeMillis);
                updateTime = now; // 更新 updateTime
                deltaUpdate -= 1; // 将 deltaUpdate 减去 1，表示已经进行了一次 Update
                // 减 1 而不是直接置 0 是因为 deltaUpdate 可能大于 1，这样可以尽可能地减少误差
            }

            if (targetFps <= 0 || deltaFps >= 1) {
                render.render(window, scene);
                deltaFps--;
                window.update();
            }
            initialTime = now;
        }

        cleanup();
    }

    public void start() {
        running = true;
        run();
    }

    public void stop() {
        running = false;
    }

}
