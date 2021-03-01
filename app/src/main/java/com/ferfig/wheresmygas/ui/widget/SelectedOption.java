package com.ferfig.wheresmygas.ui.widget;

public enum SelectedOption {
    NONE(0),
    BOTH(1),
    NEAR(2),
    FAVORITE(3);

    private final int selectedValue;

    SelectedOption(int selectedValue) {
        this.selectedValue = selectedValue;
    }

    public int getValue(){
        return this.selectedValue;
    }
}
