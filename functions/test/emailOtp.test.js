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

    delete require.cache[indexPath];
    delete process.env.EMAIL_OTP_CONTINUE_URL;

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
  }
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});