package it.unibo.artificial_vision_tracking.aruco_markers;

public enum ResolutionEnum {
    RESOLUTION_3840_2160(3840, 2160),
    RESOLUTION_1920_1080(1920, 1080),
    RESOLUTION_1280_720(1280, 720),
    RESOLUTION_1024_768(1024, 768),
    RESOLUTION_800_600(800, 600),
    RESOLUTION_640_480(640, 480);

    private final int width;
    private final int height;

    ResolutionEnum(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}


