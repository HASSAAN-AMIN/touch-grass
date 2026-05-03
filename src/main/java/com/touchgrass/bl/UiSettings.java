package com.touchgrass.bl;

public final class UiSettings {
    public enum ThemeMode {
        LIGHT,
        DUSK
    }

    public enum AccentStyle {
        SAGE,
        LAVENDER,
        CORAL
    }

    private ThemeMode themeMode = ThemeMode.LIGHT;
    private AccentStyle accentStyle = AccentStyle.SAGE;
    private boolean showFps = true;
    private boolean ambientMotion = true;

    public synchronized ThemeMode getThemeMode() {
        return themeMode;
    }

    public synchronized void setThemeMode(ThemeMode themeMode) {
        if (themeMode != null) {
            this.themeMode = themeMode;
        }
    }

    public synchronized AccentStyle getAccentStyle() {
        return accentStyle;
    }

    public synchronized void setAccentStyle(AccentStyle accentStyle) {
        if (accentStyle != null) {
            this.accentStyle = accentStyle;
        }
    }

    public synchronized boolean isShowFps() {
        return showFps;
    }

    public synchronized void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public synchronized boolean isAmbientMotion() {
        return ambientMotion;
    }

    public synchronized void setAmbientMotion(boolean ambientMotion) {
        this.ambientMotion = ambientMotion;
    }
}
