# Posko24 Firebase Functions

## upgradeToProvider

`upgradeToProvider` is a callable Cloud Function that upgrades the current user to a service provider.

### Behavior
- Requires the caller to be authenticated.
- Adds the string `"provider"` to the user's `roles` array in `users/{uid}`.
- If a profile for the user does not yet exist in `provider_profiles/{uid}`, a default profile document is created with basic fields such as `fullName`, `bio`, and availability.
- Returns `{ success: true }` when the upgrade is complete.

The function can be invoked from the client using the Firebase Functions SDK:

```javascript
const upgradeToProvider = httpsCallable(functions, 'upgradeToProvider');
await upgradeToProvider();
```

## Konfigurasi Lingkungan

Beberapa fungsi callable membutuhkan konfigurasi tambahan melalui variabel lingkungan. Untuk mengaktifkan pengiriman OTP email menggunakan layanan bawaan Firebase Authentication, setel nilai `firebase.web_api_key` sebelum melakukan deploy fungsi:
```bash
firebase functions:config:set firebase.web_api_key="<web-api-key-project-anda>"
```

Web API key dapat ditemukan di konsol Firebase pada pengaturan proyek bagian *General*. Selama pengembangan lokal Anda juga dapat menggunakan variabel lingkungan `FIREBASE_WEB_API_KEY` apabila lebih nyaman.

Untuk parameter `EMAIL_OTP_CONTINUE_URL`, sertakan nilai yang sesuai ketika melakukan deploy fungsi:

```bash
firebase deploy --only functions --set-env-vars EMAIL_OTP_CONTINUE_URL="https://posko24-80fa4.firebaseapp.com"
```

Perintah yang sama dapat digunakan bersama emulator agar parameter tersedia selama pengembangan lokal:

```bash
firebase emulators:start --only functions --set-env-vars EMAIL_OTP_CONTINUE_URL="https://posko24-80fa4.firebaseapp.com"
```