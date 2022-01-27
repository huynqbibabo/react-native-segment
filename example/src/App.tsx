import React, { useEffect, useMemo } from 'react';
import {
  View,
  ScrollView,
  Text,
  TouchableOpacity,
  StyleSheet,
  Dimensions,
  SafeAreaView,
} from 'react-native';
import {
  SegmentProvider,
  createClient,
  useSegment,
} from 'react-native-segment';
import { Navigation } from 'react-native-navigation';

const screenWidth = Dimensions.get('screen').width;

const Home = () => {
  const {
    screen,
    track,
    identify,
    group,
    alias,
    reset,
    flush,
    refreshToken,
    getInstallCampaignId,
  } = useSegment();

  useEffect(() => {
    refreshToken('');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const getInstallId = async () => {
      const id = await getInstallCampaignId();
      console.log(id);
    };
    getInstallId();
  }, [getInstallCampaignId]);

  const analyticsEvents = useMemo(() => {
    return [
      {
        color: colors.green,
        name: 'Track',
        testID: 'BUTTON_TRACK',
        onPress: () => {
          track('post_view', {
            postId: 123456,
            categoryId: 3333,
            groupId: 1111,
            topicId: 22222,
          });
        },
      },
      {
        color: colors.darkGreen,
        name: 'Screen',
        testID: 'BUTTON_SCREEN',
        onPress: () => {
          screen('Home Screen', { foo: 'bar' });
        },
      },
      {
        color: colors.purple,
        name: 'Identify',
        testID: 'BUTTON_IDENTIFY',
        onPress: () => {
          identify('user_2', { username: 'simplyTheBest' });
        },
      },
      {
        color: colors.lightPurple,
        name: 'Group',
        testID: 'BUTTON_GROUP',
        onPress: () => group('best-group', { companyId: 'Lala' }),
      },
      {
        color: colors.indigo,
        name: 'Alias',
        testID: 'BUTTON_ALIAS',
        onPress: () => alias('new-id'),
      },
    ];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const clientEvents = [
    {
      color: colors.pink,
      name: 'Flush',
      testID: 'BUTTON_FLUSH',
      onPress: () => flush(),
    },
    {
      color: colors.orange,
      name: 'Reset',
      testID: 'BUTTON_RESET',
      onPress: () => reset(),
    },
  ];

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView style={styles.page}>
        <Text style={styles.title}>Analytics Events</Text>
        <View style={styles.section}>
          {analyticsEvents.map((item) => (
            <TouchableOpacity
              key={item.name}
              style={[styles.button, { backgroundColor: item.color }]}
              onPress={item.onPress}
              testID={item.testID}
            >
              <Text style={styles.text}>{item.name}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.title}>Client Events</Text>
        <View style={styles.section}>
          {clientEvents.map((item) => (
            <TouchableOpacity
              key={item.name}
              style={[styles.trackingButton, { backgroundColor: item.color }]}
              onPress={item.onPress}
              testID={item.testID}
            >
              <Text style={styles.text}>{item.name}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.title}>Navigation</Text>
      </ScrollView>
    </SafeAreaView>
  );
};

const colors = {
  green: '#52bd95',
  darkGreen: '#28a745',
  acai: '#5c4d6b',
  purple: '#6152bd',
  lightPurple: '#6f42c1',
  indigo: '#6610f2',
  pink: '#e83e8c',
  red: '#dc3545',
  orange: '#fd7e14',
  yellow: '#ffc107',
  darkBlue: '#262e4f',
};

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: colors.darkBlue },
  page: {
    flex: 1,
    paddingTop: 30,
  },
  trackingButton: {
    marginVertical: 5,
    marginHorizontal: 5,
    paddingHorizontal: 0,
    paddingVertical: 16,
    backgroundColor: colors.green,
    borderRadius: 8,
    width: screenWidth / 3 - 20,
  },
  button: {
    marginVertical: 8,
    marginHorizontal: 5,
    paddingHorizontal: 12,
    paddingVertical: 16,
    backgroundColor: colors.green,
    borderRadius: 8,
    width: screenWidth / 1.5,
  },
  text: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: 24,
    textAlign: 'center',
  },
  title: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: 20,
    textAlign: 'center',
    marginBottom: 10,
  },
  section: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    marginBottom: 40,
  },
  mainHeading: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: 24,
    textAlign: 'center',
    marginBottom: 20,
  },
});

export default function start() {
  const segmentClient = createClient({
    debug: true,
    writeKey: 'segment.client',
    trackAppLifecycleEvents: false,
    flushAt: 5,
    proxy: {
      path: '',
      host: '',
      scheme: 'https',
      token: '',
      port: 80,
    },
  });

  Navigation.registerComponent('example.app', () => {
    return (props) => (
      <SegmentProvider client={segmentClient}>
        <Home {...props} />
      </SegmentProvider>
    );
  });

  Navigation.events().registerAppLaunchedListener(() => {
    Navigation.setRoot({
      root: {
        stack: {
          children: [
            {
              component: {
                name: 'example.app',
              },
            },
          ],
          options: {
            topBar: { visible: false },
          },
        },
      },
    });
  });
}
