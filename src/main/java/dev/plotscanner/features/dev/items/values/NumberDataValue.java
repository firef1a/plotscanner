package dev.plotscanner.features.dev.items.values;

public class NumberDataValue extends DataValue {
    public NumberDataValue(double value) {
        super(value);
    }

    public Double getValue() {
        return (Double) super.getValue();
    }
}