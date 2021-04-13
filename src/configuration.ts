import type { Configuration } from './bridge';
import type { Segment } from './index';

const defaults = {
  android: ({
    collectDeviceId = true,
    flushInterval,
  }: Partial<Configuration['android']>) => ({
    collectDeviceId,
    flushInterval,
  }),
  ios: ({
    trackAdvertising = false,
    trackDeepLinks = false,
  }: Partial<Configuration['ios']>) => ({
    trackAdvertising,
    trackDeepLinks,
  }),
};

export const configure = async (
  writeKey: string,
  {
    flushAt = 20,
    debug = false,
    recordScreenViews = false,
    trackAppLifecycleEvents = false,
    using = [],
    defaultProjectSettings = {},
    proxy,

    ios = {},
    android = {},
  }: Segment.Configuration
): Promise<Configuration> => {
  await Promise.all(
    using.map(async (integration: any) =>
      typeof integration === 'function' ? await integration() : null
    )
  );

  const config = {
    debug,
    defaultProjectSettings,
    flushAt,
    proxy,
    recordScreenViews,
    trackAppLifecycleEvents,
    writeKey,

    android: defaults.android(android),
    ios: defaults.ios(ios),
  };
  const json = JSON.stringify(config);

  return {
    ...config,
    json,
  };
};
