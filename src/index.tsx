import { NativeModules } from 'react-native';

const { Segment } = NativeModules;

export async function getFacebookAdCampaignId(): Promise<string | null> {
  return await Segment.getFacebookAdCampaignId();
}
