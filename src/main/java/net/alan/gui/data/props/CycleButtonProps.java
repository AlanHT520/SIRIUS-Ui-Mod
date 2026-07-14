package net.alan.gui.data.props;

import net.alan.gui.data.CycleValue;
import java.util.List;

public record CycleButtonProps(
        String optionKey,
        List<CycleValue> values,
        boolean displayOnlyValue
) {
    public CycleButtonProps() { this(null, List.of(), false); }
}