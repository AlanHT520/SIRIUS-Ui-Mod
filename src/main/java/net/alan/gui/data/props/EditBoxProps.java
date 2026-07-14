package net.alan.gui.data.props;

public record EditBoxProps(
        int maxLength,
        boolean bordered,
        String hint,
        String initialValue,
        String textColor
) {
    public EditBoxProps {
        if (maxLength <= 0) maxLength = 32;
    }

    public static EditBoxProps defaults() {
        return new EditBoxProps(32, true, null, "", "0xE0E0E0");
    }
}