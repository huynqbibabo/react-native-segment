package analytics;

import android.app.Activity;
import android.os.Bundle;

import analytics.integrations.AliasPayload;
import analytics.integrations.BasePayload;
import analytics.integrations.GroupPayload;
import analytics.integrations.IdentifyPayload;
import analytics.integrations.Integration;
import analytics.integrations.ScreenPayload;
import analytics.integrations.TrackPayload;
import analytics.internal.Private;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static analytics.Options.ALL_INTEGRATIONS_KEY;
import static analytics.internal.Utils.isNullOrEmpty;

/**
 * Abstraction for a task that a {@link Integration <?>} can execute.
 */
abstract class IntegrationOperation {

  @Private
  static boolean isIntegrationEnabled(ValueMap integrations, String key) {
    if (isNullOrEmpty(integrations)) {
      return true;
    }
    if (SegmentIntegration.SEGMENT_KEY.equals(key)) {
      return true; // Leave Segment integration enabled.
    }
    boolean enabled = true;
    if (integrations.containsKey(key)) {
      enabled = integrations.getBoolean(key, true);
    } else if (integrations.containsKey(ALL_INTEGRATIONS_KEY)) {
      enabled = integrations.getBoolean(ALL_INTEGRATIONS_KEY, true);
    }
    return enabled;
  }

  static IntegrationOperation onActivityCreated(final Activity activity, final Bundle bundle) {
    return new IntegrationOperation() {
      @Override
      public void run(
        String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.onActivityCreated(activity, bundle);
      }

      @Override
      public String toString() {
        return "Activity Created";
      }
    };
  }

  static IntegrationOperation onActivityStarted(final Activity activity) {
    return new IntegrationOperation() {
      @Override
      public void run(
        String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.onActivityStarted(activity);
      }

      @Override
      public String toString() {
        return "Activity Started";
      }
    };
  }

  static IntegrationOperation onActivityResumed(final Activity activity) {
    return new IntegrationOperation() {
      @Override
      public void run(
        String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.onActivityResumed(activity);
      }

      @Override
      public String toString() {
        return "Activity Resumed";
      }
    };
  }

  static IntegrationOperation onActivityPaused(final Activity activity) {
    return new IntegrationOperation() {
      @Override
      public void run(
        String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.onActivityPaused(activity);
      }

      @Override
      public String toString() {
        return "Activity Paused";
      }
    };
  }

  static IntegrationOperation onActivityStopped(final Activity activity) {
    return new IntegrationOperation() {
      @Override
      public void run(
        String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.onActivityStopped(activity);
      }

      @Override
      public String toString() {
        return "Activity Stopped";
      }
    };
  }

  static IntegrationOperation onActivitySaveInstanceState(
    final Activity activity, final Bundle bundle) {
    return new IntegrationOperation() {
      @Override
      public void run(
        String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.onActivitySaveInstanceState(activity, bundle);
      }

      @Override
      public String toString() {
        return "Activity Save Instance";
      }
    };
  }

  static IntegrationOperation onActivityDestroyed(final Activity activity) {
    return new IntegrationOperation() {
      @Override
      public void run(
        String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.onActivityDestroyed(activity);
      }

      @Override
      public String toString() {
        return "Activity Destroyed";
      }
    };
  }

  @Private
  static List<Middleware> getMiddlewareList(
    Map<String, List<Middleware>> destinationMiddleware, String key) {
    List<Middleware> middleware = destinationMiddleware.get(key);
    if (middleware == null) {
      // No middleware registered, return empty list
      return Collections.emptyList();
    }
    return middleware;
  }

  @Private
  static void runMiddlewareChain(
    BasePayload payload, List<Middleware> middleware, Middleware.Callback callback) {
    Middleware.Chain chain = new MiddlewareChainRunner(0, payload, middleware, callback);
    chain.proceed(payload);
  }

  /**
   * Integration Operation for a Segment Event (track | identify | alias | group | screen)
   * Operation runs destination middleware for given integration before sending to the desired
   * integration
   */
  static IntegrationOperation segmentEvent(
    final BasePayload payload, Map<String, List<Middleware>> destinationMiddleware) {
    return new IntegrationOperation() {
      @Override
      void run(String key, Integration<?> integration, ProjectSettings projectSettings) {
        List<Middleware> applicableMiddleware =
          getMiddlewareList(destinationMiddleware, key);
        runMiddlewareChain(
          payload,
          applicableMiddleware,
          payload1 -> {
            switch (payload1.type()) {
              case identify:
                identify((IdentifyPayload) payload1, key, integration);
                break;
//              case alias:
//                alias((AliasPayload) payload1, key, integration);
//                break;
//              case group:
//                group((GroupPayload) payload1, key, integration);
//                break;
              case track:
                track(
                  (TrackPayload) payload1,
                  key,
                  integration,
                  projectSettings);
                break;
//              case screen:
//                screen((ScreenPayload) payload1, key, integration);
//                break;
              default:
                throw new AssertionError("unknown type " + payload1.type());
            }
          });
      }

      @Override
      public String toString() {
        return payload.toString();
      }
    };
  }

  static void identify(IdentifyPayload identifyPayload, String key, Integration<?> integration) {
//    if (isIntegrationEnabled(identifyPayload.integrations(), key)) {
      integration.identify(identifyPayload);
//    }
  }

  static void group(GroupPayload groupPayload, String key, Integration<?> integration) {
//    if (isIntegrationEnabled(groupPayload.integrations(), key)) {
      integration.group(groupPayload);
//    }
  }

  static void track(
    TrackPayload trackPayload,
    String key,
    Integration<?> integration,
    ProjectSettings projectSettings) {
//    ValueMap integrationOptions = trackPayload.integrations();

    ValueMap trackingPlan = projectSettings.trackingPlan();
    if (isNullOrEmpty(trackingPlan)) {
      // No tracking plan, use options provided.
//      if (isIntegrationEnabled(integrationOptions, key)) {
        integration.track(trackPayload);
//      }
      return;
    }

    String event = trackPayload.event();

    ValueMap eventPlan = trackingPlan.getValueMap(event);
    if (isNullOrEmpty(eventPlan)) {
//      if (!isNullOrEmpty(integrationOptions)) {
//        // No event plan, use options provided.
//        if (isIntegrationEnabled(integrationOptions, key)) {
//          integration.track(trackPayload);
//        }
//        return;
//      }

      // Use schema defaults if no options are provided.
      ValueMap defaultPlan = trackingPlan.getValueMap("__default");

      // No defaults, send the event.
      if (isNullOrEmpty(defaultPlan)) {
        integration.track(trackPayload);
        return;
      }

      // Send the event if new events are enabled or if this is the Segment integration.
      boolean defaultEventsEnabled = defaultPlan.getBoolean("enabled", true);
      if (defaultEventsEnabled || SegmentIntegration.SEGMENT_KEY.equals(key)) {
        integration.track(trackPayload);
      }

      return;
    }

    // We have a tracking plan for the event.
    boolean isEnabled = eventPlan.getBoolean("enabled", true);
    if (!isEnabled) {
      // If event is disabled in the tracking plan, send it only Segment.
      if (SegmentIntegration.SEGMENT_KEY.equals(key)) {
        integration.track(trackPayload);
      }
      return;
    }

    ValueMap integrations = new ValueMap();
    ValueMap eventIntegrations = eventPlan.getValueMap("integrations");
    if (!isNullOrEmpty(eventIntegrations)) {
      integrations.putAll(eventIntegrations);
    }
//    integrations.putAll(integrationOptions);
    if (isIntegrationEnabled(integrations, key)) {
      integration.track(trackPayload);
    }
  }

//  static void screen(ScreenPayload screenPayload, String key, Integration<?> integration) {
//    if (isIntegrationEnabled(screenPayload.integrations(), key)) {
//      integration.screen(screenPayload);
//    }
//  }
//
//  static void alias(AliasPayload aliasPayload, String key, Integration<?> integration) {
//    if (isIntegrationEnabled(aliasPayload.integrations(), key)) {
//      integration.alias(aliasPayload);
//    }
//  }

  static final IntegrationOperation FLUSH =
    new IntegrationOperation() {
      @Override
      void run(String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.flush();
      }

      @Override
      public String toString() {
        return "Flush";
      }
    };

  static final IntegrationOperation RESET =
    new IntegrationOperation() {
      @Override
      void run(String key, Integration<?> integration, ProjectSettings projectSettings) {
        integration.reset();
      }

      @Override
      public String toString() {
        return "Reset";
      }
    };

  private IntegrationOperation() {
  }

  /**
   * Run this operation on the given integration.
   */
  abstract void run(String key, Integration<?> integration, ProjectSettings projectSettings);
}
