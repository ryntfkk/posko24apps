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

Beberapa fungsi callable membutuhkan konfigurasi tambahan melalui variabel lingkungan. Untuk mengaktifkan pengiriman OTP email menggunakan layanan bawaan Firebase Authentication, setel nilai `FIREBASE_WEB_API_KEY` sebelum melakukan deploy fungsi:

```bash
firebase functions:config:set FIREBASE_WEB_API_KEY="<web-api-key-project-anda>"
```

Web API key dapat ditemukan di konsol Firebase pada pengaturan proyek bagian *General*.
