# Briefly — Billing & Usage Tracking Setup

## Overview

Briefly uses a freemium model:
- **Free tier**: 40 summaries/month (configurable via Firebase Remote Config)
- **Pro tier**: Unlimited summaries via a $1.49/mo Google Play subscription

Usage is tracked in Firestore with anonymous authentication so it persists across reinstalls.

---

## Services You Maintain

### 1. Firebase Anonymous Auth
- **What it does**: Gives each device a stable user ID without requiring sign-in
- **Where**: Firebase Console → Authentication → Sign-in method → Anonymous
- **Maintenance**: None — it's fully managed by Firebase
- **Cost**: Free, no limits

### 2. Firestore Database
- **What it does**: Stores each user's summary count and billing period start date
- **Where**: Firebase Console → Firestore Database
- **Maintenance**: Deploy security rules (see below), monitor usage if needed
- **Cost**: Free tier covers 50K reads + 20K writes/day

### 3. Firebase Remote Config
- **What it does**: Lets you change the free summary limit without publishing an app update
- **Where**: Firebase Console → Remote Config
- **Parameter**: `free_summary_limit` (integer, default: 40)
- **Maintenance**: Change the value anytime and publish — the app picks it up within 1 hour
- **Cost**: Free

### 4. Google Play Billing
- **What it does**: Handles the $1.49/mo subscription purchase and renewal
- **Where**: Play Console → Monetize → Subscriptions
- **Product ID**: `briefly_pro_monthly` (must match exactly — hardcoded in `BillingManager.kt`)
- **Maintenance**: You can change the price in Play Console anytime; existing subscribers keep their price unless you configure otherwise

---

## Firestore Structure

```
Firestore Database
└── usage (collection)
    └── {userId} (document — one per anonymous user)
        ├── summaryCount: 12        ← number of summaries used this period
        └── periodStart: 1715241600000  ← epoch ms when the 30-day period started
```

### Example

After a user creates their 5th summary:
```
usage/abc123def456
  summaryCount: 5
  periodStart: 1715241600000   (May 9, 2025)
```

When the app checks if a user can summarize:
1. Fetch `usage/{uid}`
2. If `periodStart` is older than 30 days → reset `summaryCount` to 0 and update `periodStart`
3. If `summaryCount < free_summary_limit` (from Remote Config) → allow
4. If user is Pro (has active subscription) → always allow

### What happens on reinstall?
Anonymous auth reuses the same UID on the same device, so the usage document persists. The user cannot reset their count by reinstalling.

---

## Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /usage/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Each user can only read/write their own usage document. No user can access another user's data.

---

## Quick Reference: Things You Might Want to Change

| What | Where | Requires app update? |
|---|---|---|
| Free summary limit | Firebase Remote Config | No |
| Subscription price | Play Console | No |
| Billing period (30 days) | `UsageRepository.kt` → `PERIOD_MS` | Yes |
| Product ID | `BillingManager.kt` → `PRODUCT_ID` | Yes |
| Firestore collection name | `UsageRepository.kt` → `COLLECTION_USAGE` | Yes |

---

## Testing Billing

1. Add your email as a license tester: Play Console → Setup → License testing
2. Upload the app to the internal testing track
3. License testers can make purchases without being charged
4. Subscription renewals happen on an accelerated schedule during testing (e.g. monthly = every 5 minutes)
