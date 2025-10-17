'use strict';

const assert = require('assert');
const { HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

function getCallable(handler) {
  if (!handler || typeof handler.run !== 'function') {
    throw new Error('Unsupported callable handler type');
  }
  return handler;
}

async function run() {
  const indexPath = require.resolve('../index.js');
  const originalFetch = global.fetch;
  const originalEnv = {
    FIREBASE_WEB_API_KEY: process.env.FIREBASE_WEB_API_KEY,
    EMAIL_OTP_CONTINUE_URL: process.env.EMAIL_OTP_CONTINUE_URL,
        EMAIL_OTP_HANDLE_IN_APP: process.env.EMAIL_OTP_HANDLE_IN_APP,
        EMAIL_OTP_DYNAMIC_LINK_DOMAIN: process.env.EMAIL_OTP_DYNAMIC_LINK_DOMAIN,
        EMAIL_OTP_ANDROID_PACKAGE_NAME: process.env.EMAIL_OTP_ANDROID_PACKAGE_NAME,
        EMAIL_OTP_ANDROID_MIN_VERSION: process.env.EMAIL_OTP_ANDROID_MIN_VERSION,
        EMAIL_OTP_ANDROID_INSTALL_APP: process.env.EMAIL_OTP_ANDROID_INSTALL_APP,
        EMAIL_OTP_IOS_BUNDLE_ID: process.env.EMAIL_OTP_IOS_BUNDLE_ID,
  };
  const originalInitializeApp = admin.initializeApp;
  const originalFirestoreDescriptor = Object.getOwnPropertyDescriptor(admin, 'firestore');

  try {
    admin.initializeApp = () => {};
    Object.defineProperty(admin, 'firestore', {
      configurable: true,
      enumerable: true,
      writable: true,
      value: () => ({}),
    });

    delete require.cache[indexPath];
    process.env.FIREBASE_WEB_API_KEY = 'test-key';
    process.env.EMAIL_OTP_CONTINUE_URL = 'https://example.com/continue';
    process.env.EMAIL_OTP_HANDLE_IN_APP = 'true';
    process.env.EMAIL_OTP_DYNAMIC_LINK_DOMAIN = 'example.page.link';
    process.env.EMAIL_OTP_ANDROID_PACKAGE_NAME = 'com.example.app';
    process.env.EMAIL_OTP_ANDROID_MIN_VERSION = '21';
    process.env.EMAIL_OTP_ANDROID_INSTALL_APP = 'true';
    process.env.EMAIL_OTP_IOS_BUNDLE_ID = 'com.example.ios';
    const requests = [];
    global.fetch = async (url, options) => {
      requests.push({ url, options });
      return {
        ok: true,
        async json() {
          return {};
        },
      };
    };

    const { sendEmailOtp } = require(indexPath);
    const callable = getCallable(sendEmailOtp);
    const response = await callable.run({ data: { email: 'user@example.com' } });

    assert.strictEqual(response.expiresInSeconds, 5 * 60, 'TTL should be returned when configuration is valid');
    assert.strictEqual(requests.length, 1, 'Firebase Auth should be invoked exactly once');

    const payload = JSON.parse(requests[0].options?.body || '{}');
    assert.strictEqual(payload.email, 'user@example.com');
    assert.strictEqual(payload.continueUrl, 'https://example.com/continue');
    assert.strictEqual(payload.handleCodeInApp, true);
    assert.strictEqual(payload.canHandleCodeInApp, true);
    assert.strictEqual(payload.requestType, 'EMAIL_SIGNIN');
    assert.strictEqual(payload.dynamicLinkDomain, 'example.page.link');
    assert.strictEqual(payload.iOSBundleId, 'com.example.ios');
    assert.strictEqual(payload.androidPackageName, 'com.example.app');
    assert.strictEqual(payload.androidMinimumVersion, '21');
    assert.strictEqual(payload.androidInstallApp, true);

    delete require.cache[indexPath];
    delete process.env.EMAIL_OTP_CONTINUE_URL;
    delete process.env.EMAIL_OTP_HANDLE_IN_APP;
    delete process.env.EMAIL_OTP_DYNAMIC_LINK_DOMAIN;
    delete process.env.EMAIL_OTP_ANDROID_PACKAGE_NAME;
    delete process.env.EMAIL_OTP_ANDROID_MIN_VERSION;
    delete process.env.EMAIL_OTP_ANDROID_INSTALL_APP;
    delete process.env.EMAIL_OTP_IOS_BUNDLE_ID;

    const { sendEmailOtp: sendEmailOtpMissingConfig } = require(indexPath);
    const callableMissing = getCallable(sendEmailOtpMissingConfig);

    try {
      await callableMissing.run({ data: { email: 'user@example.com' } });
      assert.fail('sendEmailOtp should fail when continue URL is missing');
    } catch (error) {
      assert.ok(error instanceof HttpsError, 'Error should be an instance of HttpsError');
      assert.strictEqual(error.code, 'failed-precondition');
      assert.match(error.message, /EMAIL_OTP_CONTINUE_URL/i);
    }

    assert.strictEqual(requests.length, 1, 'sendEmailOtp should not call fetch when configuration is missing');
    console.log('All emailOtp tests passed.');
  } finally {
    delete require.cache[indexPath];
    admin.initializeApp = originalInitializeApp;
    if (originalFirestoreDescriptor) {
      Object.defineProperty(admin, 'firestore', originalFirestoreDescriptor);
    } else {
      delete admin.firestore;
    }
    if (originalFetch) {
      global.fetch = originalFetch;
    } else {
      delete global.fetch;
    }
    if (originalEnv.FIREBASE_WEB_API_KEY === undefined) {
      delete process.env.FIREBASE_WEB_API_KEY;
    } else {
      process.env.FIREBASE_WEB_API_KEY = originalEnv.FIREBASE_WEB_API_KEY;
    }
    if (originalEnv.EMAIL_OTP_CONTINUE_URL === undefined) {
      delete process.env.EMAIL_OTP_CONTINUE_URL;
    } else {
      process.env.EMAIL_OTP_CONTINUE_URL = originalEnv.EMAIL_OTP_CONTINUE_URL;
    }
     if (originalEnv.EMAIL_OTP_HANDLE_IN_APP === undefined) {
          delete process.env.EMAIL_OTP_HANDLE_IN_APP;
        } else {
          process.env.EMAIL_OTP_HANDLE_IN_APP = originalEnv.EMAIL_OTP_HANDLE_IN_APP;
        }
        if (originalEnv.EMAIL_OTP_DYNAMIC_LINK_DOMAIN === undefined) {
          delete process.env.EMAIL_OTP_DYNAMIC_LINK_DOMAIN;
        } else {
          process.env.EMAIL_OTP_DYNAMIC_LINK_DOMAIN = originalEnv.EMAIL_OTP_DYNAMIC_LINK_DOMAIN;
        }
        if (originalEnv.EMAIL_OTP_ANDROID_PACKAGE_NAME === undefined) {
          delete process.env.EMAIL_OTP_ANDROID_PACKAGE_NAME;
        } else {
          process.env.EMAIL_OTP_ANDROID_PACKAGE_NAME = originalEnv.EMAIL_OTP_ANDROID_PACKAGE_NAME;
        }
        if (originalEnv.EMAIL_OTP_ANDROID_MIN_VERSION === undefined) {
          delete process.env.EMAIL_OTP_ANDROID_MIN_VERSION;
        } else {
          process.env.EMAIL_OTP_ANDROID_MIN_VERSION = originalEnv.EMAIL_OTP_ANDROID_MIN_VERSION;
        }
        if (originalEnv.EMAIL_OTP_ANDROID_INSTALL_APP === undefined) {
          delete process.env.EMAIL_OTP_ANDROID_INSTALL_APP;
        } else {
          process.env.EMAIL_OTP_ANDROID_INSTALL_APP = originalEnv.EMAIL_OTP_ANDROID_INSTALL_APP;
        }
        if (originalEnv.EMAIL_OTP_IOS_BUNDLE_ID === undefined) {
          delete process.env.EMAIL_OTP_IOS_BUNDLE_ID;
        } else {
          process.env.EMAIL_OTP_IOS_BUNDLE_ID = originalEnv.EMAIL_OTP_IOS_BUNDLE_ID;
        }
  }
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});