import type { SegmentClientContext } from '../client';
// import type { SegmentAPISettings } from 'react-native-segment';

export default async function getSettings(this: SegmentClientContext) {
  this.store.dispatch(
    this.actions.system.updateSettings({
      settings: this.config.defaultSettings!,
    })
  );
}
