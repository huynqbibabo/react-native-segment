import { combineReducers, configureStore } from '@reduxjs/toolkit';
import { persistStore, persistReducer, PERSIST } from 'redux-persist';
import AsyncStorage from '@react-native-community/async-storage';
import mainSlice from './main';
import systemSlice from './system';
import userInfo from './userInfo';
import type { Config } from '../types';
import logger from 'redux-logger';

export const actions = {
  main: mainSlice.actions,
  system: systemSlice.actions,
  userInfo: userInfo.actions,
};

const rootReducer = combineReducers({
  main: mainSlice.reducer,
  system: systemSlice.reducer,
  userInfo: userInfo.reducer,
});

export const initializeStore = (config: Config) => {
  const persistConfig = {
    key: `${config.writeKey}-analyticsData`,
    storage: AsyncStorage,
  };

  const persistedReducer = persistReducer(persistConfig, rootReducer);

  const middlewares: any[] = [];

  if (config.debug) {
    middlewares.push(logger);
  }

  const store = configureStore({
    reducer: persistedReducer,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        serializableCheck: {
          ignoredActions: [PERSIST],
        },
      }).concat(middlewares),
  });

  const persistor = persistStore(store);

  return { store, persistor };
};
