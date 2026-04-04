# Notification System Improvements

## Changes Made

### 1. Reduced Polling Interval (15 min → 2 min)
- Created `NotificationAlarmScheduler` to use AlarmManager
- Created `NotificationAlarmReceiver` to trigger worker on alarm
- Updated `UnreadPollingScheduler` to use AlarmManager
- Added `NotificationConfig` for easy interval adjustment

### 2. Optional Real-Time Socket Service
- Created `BackgroundSocketService` for persistent Socket.IO connection
- Provides instant notifications when app is backgrounded
- Uses foreground service to maintain connection

### 3. Configuration
- Default polling interval: **2 minutes** (configurable in `NotificationConfig`)
- Can be changed to 5 minutes by updating `POLLING_INTERVAL_MINUTES`

## How It Works

### Polling Method (Active by Default)
1. AlarmManager triggers every 2 minutes
2. AlarmReceiver enqueues UnreadMessagesWorker
3. Worker fetches unread messages and shows notifications
4. Bypasses WorkManager's 15-minute limitation

### Real-Time Method (Optional)
1. Start `BackgroundSocketService.start(context)` on login
2. Maintains Socket.IO connection in background
3. Receives instant notifications via socket events
4. Stop with `BackgroundSocketService.stop(context)` on logout

## Usage

### Current Setup (Polling Only)
Already active - no changes needed. Polls every 2 minutes.

### Enable Real-Time (Optional)
Add to LoginScreenViewModel after successful login:
```kotlin
BackgroundSocketService.start(getApplication<DinoOrderApplication>())
```

Add to ProfileScreenViewModel on logout:
```kotlin
BackgroundSocketService.stop(getApplication<DinoOrderApplication>())
```

## Permissions Added
- `SCHEDULE_EXACT_ALARM` - For precise alarm scheduling
- `USE_EXACT_ALARM` - For exact alarms on Android 14+
- `RECEIVE_BOOT_COMPLETED` - To restart alarms after reboot
- `FOREGROUND_SERVICE` - For background socket service
- `FOREGROUND_SERVICE_DATA_SYNC` - Service type declaration

## Files Created
1. `NotificationAlarmScheduler.kt` - Manages AlarmManager
2. `NotificationAlarmReceiver.kt` - Receives alarm broadcasts
3. `NotificationConfig.kt` - Configuration constants
4. `BackgroundSocketService.kt` - Optional real-time service

## Files Modified
1. `UnreadPollingScheduler.kt` - Uses AlarmManager
2. `AndroidManifest.xml` - Added permissions and receiver

## Recommendations

### For Best Performance
- Use **polling only** (current setup) - battery efficient, reliable
- 2-minute interval provides good balance

### For Real-Time Notifications
- Enable `BackgroundSocketService` - instant delivery
- Higher battery usage due to persistent connection
- Best for users who need immediate notifications

### Adjust Interval
Change `POLLING_INTERVAL_MINUTES` in `NotificationConfig.kt`:
- 2 minutes: Fast, moderate battery usage
- 5 minutes: Balanced, lower battery usage
- 10 minutes: Slow, minimal battery usage
