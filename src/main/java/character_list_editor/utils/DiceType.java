package character_list_editor.utils;

public enum DiceType {
    D4(4),
    D6(6),
    D8(8),
    D10(10),
    D12(12),
    D20(20),
    D100(100);

    private final int maxValue;

    DiceType(int maxValue) {
        this.maxValue = maxValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}