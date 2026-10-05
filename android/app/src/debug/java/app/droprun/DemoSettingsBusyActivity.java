package app.droprun;

/** Only models a pending settings request in memory; cached preferences stay unchanged. */
public final class DemoSettingsBusyActivity extends SettingsActivity {
    int modeRequests;boolean requestedDirect;
    @Override void refresh(){}
    @Override void changeMode(boolean direct){saveMode(direct);}
    @Override void saveMode(boolean direct){modeRequests++;requestedDirect=direct;busy=true;notice=L.t("Saving…","正在保存…");render();}
    void settle(boolean success){busy=false;notice=success?L.t("Execution preference saved.","执行偏好已保存。"):L.t("Could not save the execution preference.","执行偏好保存失败。");render();}
    @Override void setPermission(String id,boolean enabled){throw new AssertionError("Busy probe must not change project access");}
    @Override void disconnect(){throw new AssertionError("Busy probe must not disconnect");}
}
