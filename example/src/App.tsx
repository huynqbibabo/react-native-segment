import React, { Component } from 'react';
import { StyleSheet, Text, View, Image, TouchableOpacity } from 'react-native';
import segment from 'react-native-segment';
import type { Segment } from 'react-native-segment';

const Button = ({ title, onPress }: { title: string; onPress: () => void }) => (
  <TouchableOpacity style={styles.button} onPress={onPress}>
    <Text style={styles.text}>{title}</Text>
  </TouchableOpacity>
);

const flush = () => segment.flush();

const pizzaEaten = () => {
  segment.track('post_view', {
    postId: 9999,
    groupId: 9696,
    topicIds: '96,69,96,69',
  });
};

const trackOrder = () => {
  segment.track('product_view', {
    productId: 123456,
    categoryId: 3333,
  });
};

const logAnonymousId = async () => {
  segment.track('add_to_cart', {
    productId: 123456,
    categoryId: 3333,
  });
};

export default class App extends Component {
  componentDidMount() {
    segment.getFacebookCampaignId().then((id) => {
      console.log(id);
    });
    segment.identify('9999');
  }

  render() {
    return (
      <View style={styles.container}>
        <Image
          source={{ uri: 'https://i.imgur.com/GrCdId0.png' }}
          resizeMode="contain"
          style={{
            margin: 50,
            width: 240,
            height: 160,
          }}
        />
        <Button title="Track: Order Complete" onPress={trackOrder} />
        <Button title="Flush" onPress={flush} />
        <Button title="Track: Pizza Eaten" onPress={pizzaEaten} />
        <Button title="Log anonymousId" onPress={logAnonymousId} />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  button: {
    margin: 20,
  },
  text: {
    color: '#FBFAF9',
  },
  container: {
    flex: 1,
    justifyContent: 'flex-start',
    alignItems: 'center',
    backgroundColor: '#32A75D',
  },
});
const integrations: Segment.Integration[] = [];

segment
  .setup(
    'eyJDVCI6MCwiQ0kiOjEsIlVJIjo4Mzg1NzksIlNFIjoiMTYxODIwOTY0MzE4NTQ3MjEifQ',
    {
      debug: true,
      using: integrations,
      flushAt: 3,
      proxy: {
        host: 'segment.bbbnet.xyz',
        path: 'api/v1/log-event',
        scheme: 'http',
        port: 80,
      },
      android: {
        collectDeviceId: false,
      },
      ios: {
        trackAdvertising: false,
      },
    }
  )
  .then(() => console.log('Analytics ready'))
  .catch((err: any) => console.error(err));
