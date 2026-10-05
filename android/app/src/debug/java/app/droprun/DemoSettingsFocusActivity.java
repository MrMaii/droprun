package app.droprun;

/** Reads existing settings only: no fixture seed, network refresh or remote changes. */
public final class DemoSettingsFocusActivity extends SettingsActivity {
    @Override void refresh(){}
    @Override void setPermission(String id,boolean enabled){}
    @Override void saveMode(boolean direct){}
    @Override void disconnect(){}
}
