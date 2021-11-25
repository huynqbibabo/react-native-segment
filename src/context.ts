import { NativeModules } from 'react-native';
import packageJson from '../package.json';

import type {
  Context,
  ContextDevice,
  NativeContextInfo,
  UserTraits,
} from './types';

export const getContext = async (
  userTraits: UserTraits = {}
): Promise<Context> => {
  const { RNSegment } = NativeModules;

  const {
    appName,
    appVersion,
    buildNumber,
    bundleId,
    locale,
    networkType,
    osName,
    osVersion,
    screenHeight,
    screenWidth,
    timezone,
    manufacturer,
    model,
    deviceName,
    deviceId,
    deviceType,
    screenDensity,
  }: NativeContextInfo = await RNSegment.getContextInfo({});

  const device: ContextDevice = {
    id: deviceId,
    manufacturer: manufacturer,
    model: model,
    name: deviceName,
    type: deviceType,
  };

  return {
    app: {
      build: buildNumber,
      name: appName,
      namespace: bundleId,
      version: appVersion,
    },
    device,
    library: {
      name: packageJson.name,
      version: packageJson.version,
    },
    locale,
    network: {
      cellular: networkType === 'cellular',
      wifi: networkType === 'wifi',
    },
    os: {
      name: osName,
      version: osVersion,
    },
    screen: {
      width: screenWidth,
      height: screenHeight,
      density: screenDensity,
    },
    timezone,
    traits: userTraits,
  };
};
