package com.ferfig.wheresmygas.ui.settings;

public enum SettingOption {
    SHOW_INFO_WINDOW(0),
    HIDE_INFO_WINDOW(1),
    UNITS_METRIC(2),
    UNITS_IMPERIAL(3);

    private final int selectedValue;

    SettingOption(int selectedValue) {
        this.selectedValue = selectedValue;
    }

    public int getValue(){
        return this.selectedValue;
    }

}
