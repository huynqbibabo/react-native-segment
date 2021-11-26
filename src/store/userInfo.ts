import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { UserTraits } from '../types';

type UserInfoState = {
  anonymousId: string;
  userId?: string;
  traits?: UserTraits;
};

export const initialState: UserInfoState = {
  anonymousId: '',
  userId: undefined,
  traits: undefined,
};

export default createSlice({
  name: 'userInfo',
  initialState,
  reducers: {
    reset: () => ({
      anonymousId: '',
      userId: undefined,
      traits: undefined,
    }),
    setUserId: (state, action: PayloadAction<{ userId: string }>) => {
      state.userId = action.payload.userId;
    },
    setTraits: (state, action: PayloadAction<{ traits?: UserTraits }>) => {
      state.traits = {
        ...state.traits,
        ...action.payload.traits,
      };
    },
    setUserIdAndTraits: (
      state,
      action: PayloadAction<{ userId: string; traits?: UserTraits }>
    ) => {
      state.userId = action.payload.userId;
      state.traits = action.payload.traits;
    },
    setAnonymousId: (state, action: PayloadAction<{ anonymousId: string }>) => {
      state.anonymousId = action.payload.anonymousId;
    },
  },
});
