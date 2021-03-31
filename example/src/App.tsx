import React, { Component } from 'react';
import { StyleSheet, Text, View, Image, TouchableOpacity } from 'react-native';
import analytics from 'react-native-segment';

const Button = ({ title, onPress }: { title: string; onPress: () => void }) => (
  <TouchableOpacity style={styles.button} onPress={onPress}>
    <Text style={styles.text}>{title}</Text>
  </TouchableOpacity>
);

const flush = () => analytics.flush();

const pizzaEaten = () => {
  analytics.track('post_view', { postId: 123456 });
};

const trackOrder = () => {
  // analytics.track('Order Completed');
  // analytics.track('Order Cancelled', {
  //   order_id: 323,
  // });
  // analytics.identify('userIdOnly');
  // analytics.identify(123456789, {
  //   age: 32,
  // });
  // analytics.alias('newlyAliasedId');
  // analytics.screen('User Login Screen', {
  //   method: 'google',
  // });
};

const logAnonymousId = async () => {
  console.log('anonymousId: %s', await analytics.getAnonymousId());
};

export default class App extends Component {
  componentDidMount() {
    analytics.identify('9999');
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

analytics
  .setup(
    'eyJDVCI6MCwiQ0kiOjEsIlVJIjozMjc4LCJTRSI6IjE1Nzk0OTA4MjgxNjE5MDU3In0',
    {
      debug: true,
      using: [],
      flushAt: 3,
      proxy: {
        host: 'segment.bbbnet.xyz',
        path: 'api/v1/log-event',
        scheme: 'http',
        port: 80,
      },
    }
  )
  .then(() => console.log('Analytics ready'))
  .catch((err: any) => console.error(err));
