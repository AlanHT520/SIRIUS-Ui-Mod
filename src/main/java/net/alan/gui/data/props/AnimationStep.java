package net.alan.gui.data.props;

public record AnimationStep(
        String type,
        int duration,
        int delay,
        float speed,
        boolean loop,
        int start,
        int end
) {
    public AnimationStep() {
        this(null, 0, 0, 0.0f, false, -1, -1);
    }

    public long totalDuration() {
        return (long) delay + duration;
    }
}