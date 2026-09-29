# Briefly

An Android app that summarizes articles and on-screen text using Gemini —
share a link or capture your screen, get a short summary back.

[Get it on Google Play](https://play.google.com/store/apps/details?id=com.raibbl.AiAnalyze&hl=en_US)

## What it does

- **Share-to-summarize**: share any link from another app (browser, Twitter,
  etc.) and Briefly fetches the article, extracts the main text, and
  summarizes it.
- **Screen capture summarize**: a Quick Settings tile that captures the
  current screen, runs on-device OCR, and summarizes the recognized text —
  useful for content that isn't a shareable link.
- Summaries are saved locally (Room) with search, so past summaries stay
  searchable and swipe-to-delete works offline.
- Free tier (40 summaries/month) with an optional Pro subscription for
  unlimited use, tracked via Firebase so limits persist across reinstalls.

## Stack

- **UI**: Jetpack Compose
- **AI**: Firebase AI Logic SDK (`Firebase.ai`, Gemini `2.5-flash-lite`)
- **OCR**: ML Kit Text Recognition
- **Persistence**: Room (local summaries), Firestore (usage/billing state)
- **Billing**: Google Play Billing (monthly subscription)
- **Remote config**: Firebase Remote Config (adjust free-tier limits without
  an app update)

See [`WARP.md`](WARP.md) for a deeper architecture walkthrough and
[`BILLING_SETUP.md`](BILLING_SETUP.md) for how the billing/usage-tracking
system works.

## Building locally

```bash
./gradlew assembleDebug
```

Requires a `google-services.json` from your own Firebase project (not
included — see `.gitignore`) since the app uses Firebase AI Logic, Firestore,
and Remote Config.
