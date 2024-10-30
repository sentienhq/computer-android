package fr.neamar.kiss.pojo;

import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.utils.UserHandle;

public final class AppPojo extends PojoWithTags {

    public final String packageName;
    public final String activityName;
    public final UserHandle userHandle;
    private final boolean disabled;
    private List<String> foundShortcuts;
    private boolean excluded;
    private boolean excludedFromHistory;
    /**
     * Whether shortcuts are excluded for this app
     */
    private boolean excludedShortcuts;
    private long customIconId = 0;

    public AppPojo(String id, String packageName, String activityName, UserHandle userHandle,
                   boolean isExcluded, boolean isExcludedFromHistory, boolean isExcludedShortcuts, boolean disabled) {
        super(id);

        this.packageName = packageName;
        this.activityName = activityName;
        this.userHandle = userHandle;
        this.foundShortcuts = new ArrayList<>();
        this.excluded = isExcluded;
        this.excludedFromHistory = isExcludedFromHistory;
        this.excludedShortcuts = isExcludedShortcuts;
        this.disabled = disabled;
    }

    public static String getComponentName(String packageName, String activityName,
                                          UserHandle userHandle) {
        return userHandle.addUserSuffixToString(packageName + "/" + activityName, '#');
    }

    public String getComponentName() {
        return getComponentName(packageName, activityName, userHandle);
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    public boolean isExcludedFromHistory() {
        return excludedFromHistory;
    }

    public void setExcludedFromHistory(boolean excludedFromHistory) {
        this.excludedFromHistory = excludedFromHistory;
    }

    public boolean isExcludedShortcuts() {
        return excludedShortcuts;
    }

    public void setExcludedShortcuts(boolean excludedShortcuts) {
        this.excludedShortcuts = excludedShortcuts;
    }

    public long getCustomIconId() {
        return customIconId;
    }

    public void setCustomIconId(long iconId) {
        customIconId = iconId;
    }

    public String getPackageKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return userHandle.getRealHandle().hashCode() + "|" + packageName;
        } else {
            return packageName;
        }
    }

    public List<String> getShortcuts() {
        return foundShortcuts;
    }

    public void setShortcuts(List<String> shortcuts) {
        this.foundShortcuts = shortcuts;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }
}
