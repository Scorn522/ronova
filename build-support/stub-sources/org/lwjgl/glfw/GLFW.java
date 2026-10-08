package org.lwjgl.glfw;

public final class GLFW {
    public static final int GLFW_PRESS = 1;
    public static final int GLFW_MOUSE_BUTTON_LEFT = 0;
    public static final int GLFW_MOUSE_BUTTON_RIGHT = 1;
    public static final int GLFW_KEY_LEFT_SHIFT = 340;
    public static final int GLFW_KEY_RIGHT_SHIFT = 344;
    public static final int GLFW_MOD_SHIFT = 1;
    public static final int GLFW_KEY_O = 79;

    private GLFW() {}

    public static int glfwGetMouseButton(long window, int button) {
        return 0;
    }
}
