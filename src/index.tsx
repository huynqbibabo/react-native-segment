import { NativeModules } from 'react-native';

type SegmentType = {
  multiply(a: number, b: number): Promise<number>;
};

const { Segment } = NativeModules;

export default Segment as SegmentType;
