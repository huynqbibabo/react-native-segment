package analytics;

import android.content.Context;

import analytics.internal.Private;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;

class ProjectSettings extends ValueMap {

    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String PLAN_KEY = "plan";
    private static final String INTEGRATIONS_KEY = "integrations";
    private static final String TRACKING_PLAN_KEY = "track";
    private static final String EDGE_FUNCTIONS_KEY = "edgeFunction";

    static ProjectSettings create(Map<String, Object> map) {
        map.put(TIMESTAMP_KEY, System.currentTimeMillis());
        return new ProjectSettings(map);
    }

    @Private
    ProjectSettings(Map<String, Object> map) {
        super(unmodifiableMap(map));
    }

    long timestamp() {
        return getLong(TIMESTAMP_KEY, 0L);
    }

    ValueMap plan() {
        return getValueMap(PLAN_KEY);
    }

    ValueMap trackingPlan() {
        ValueMap plan = plan();
        if (plan == null) {
            return null;
        }
        return plan.getValueMap(TRACKING_PLAN_KEY);
    }

    ValueMap integrations() {
        return getValueMap(INTEGRATIONS_KEY);
    }

    ValueMap edgeFunctions() {
        return getValueMap(EDGE_FUNCTIONS_KEY);
    }

    static class Cache extends ValueMap.Cache<ProjectSettings> {

        // todo: remove. This is legacy behaviour from before we started namespacing the entire
        // shared
        // preferences object and were namespacing keys instead.
        private static final String PROJECT_SETTINGS_CACHE_KEY_PREFIX = "project-settings-plan-";

        Cache(Context context, Cartographer cartographer, String tag) {
            super(
                    context,
                    cartographer,
                    PROJECT_SETTINGS_CACHE_KEY_PREFIX + tag,
                    tag,
                    ProjectSettings.class);
        }

        @Override
        public ProjectSettings create(Map<String, Object> map) {
            return new ProjectSettings(map);
        }
    }
}
