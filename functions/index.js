'use strict';

const functions = require('firebase-functions/v2');
const { setGlobalOptions } = require("firebase-functions/v2");
// Impor defineString di sini
const { defineString } = require('firebase-functions/params');

// Tambahkan baris ini
const appWebApiKey = defineString('APP_WEB_API_KEY');
const emailOtpContinueUrl = defineString('EMAIL_OTP_CONTINUE_URL');

// Tetapkan region untuk semua fungsi di file ini ke Jakarta
setGlobalOptions({ region: "asia-southeast2" });
const { onDocumentUpdated, onDocumentWritten } = require('firebase-functions/v2/firestore');
const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');
const midtransClient = require('midtrans-client');
const { ADMIN_FEE } = require('./config');
const { calculateRefundBreakdown } = require('./refund');

const fetchWithFallback = (() => {
  if (typeof globalThis.fetch === 'function') {
    const boundFetch = globalThis.fetch.bind(globalThis);
    return async (input, init) => boundFetch(input, init);
  }

  let fetchPromise;
  return async (input, init) => {
    if (!fetchPromise) {
      fetchPromise = import('node-fetch').then((mod) => mod.default);
    }
    const fetch = await fetchPromise;
    return fetch(input, init);
  };
})();

function getFunctionsConfig() {
  try {
    return functions.config() || {};
  } catch (error) {
    functions.logger.debug('[FUNCTIONS_CONFIG_UNAVAILABLE]', {
      message: error?.message,
    });
    return {};
  }
}

function readOptionalString(value) {
  if (typeof value !== 'string') {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function readBoolean(value) {
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase();
    if (['true', '1', 'yes', 'y', 'on'].includes(normalized)) {
      return true;
    }
    if (['false', '0', 'no', 'n', 'off'].includes(normalized)) {
      return false;
    }
  }
  return null;
}

function resolveConfigString(...paths) {
  const config = getFunctionsConfig();

  for (const segments of paths) {
      const envKey = segments.join('_').toUpperCase();
      const envValue = readOptionalString(process.env[envKey]);
      if (envValue) {
        return envValue;
      }

    let current = config;
    let found = true;
    for (const segment of segments) {
      if (current && typeof current === 'object' && segment in current) {
        current = current[segment];
      } else {
        found = false;
        break;
      }
    }
    if (found) {
      const value = readOptionalString(current);
      if (value) {
        return value;
      }
    }
  }

  return null;
}

function resolveConfigBoolean(...paths) {
  const config = getFunctionsConfig();

  for (const segments of paths) {
      const envKey = segments.join('_').toUpperCase();
      const envValue = readBoolean(process.env[envKey]);
      if (envValue !== null) {
        return envValue;
      }

    let current = config;
    let found = true;
    for (const segment of segments) {
      if (current && typeof current === 'object' && segment in current) {
        current = current[segment];
      } else {
        found = false;
        break;
      }
    }
    if (found) {
      const parsed = readBoolean(current);
      if (parsed !== null) {
        return parsed;
      }
    }
  }

  return null;
}

admin.initializeApp();
const db = admin.firestore();

const ACTIVE_PROVIDER_ORDER_STATUSES = [
  'awaiting_payment',
  'pending',
  'accepted',
  'ongoing',
  'awaiting_confirmation',
  'awaiting_provider_confirmation',
];

const EMAIL_OTP_TTL_SECONDS = 5 * 60;

function sanitizeEmailInput(rawEmail) {
  if (typeof rawEmail !== 'string') {
    return '';
  }
  return rawEmail.trim().toLowerCase();
}


function normalizeScheduledDate(rawDate) {
  if (typeof rawDate !== 'string') {
    return null;
  }
  const trimmed = rawDate.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function sanitizeDateList(rawList) {
  if (!Array.isArray(rawList)) {
    return [];
  }

  const normalized = rawList
    .map((value) => normalizeScheduledDate(value))
    .filter((value) => typeof value === 'string' && value.length > 0);

  const deduped = Array.from(new Set(normalized));
  deduped.sort();
  return deduped;
}

function extractDistrictValue(raw) {
  if (!raw) {
    return null;
  }

  if (typeof raw === 'string') {
    const trimmed = raw.trim();
    return trimmed.length > 0 ? trimmed : null;
  }

  if (typeof raw === 'object') {
    if (typeof raw.name === 'string') {
      const trimmed = raw.name.trim();
      if (trimmed.length > 0) {
        return trimmed;
      }
    }

    if (typeof raw.label === 'string') {
      const trimmed = raw.label.trim();
      if (trimmed.length > 0) {
        return trimmed;
      }
    }

    if (typeof raw.value === 'string') {
      const trimmed = raw.value.trim();
      if (trimmed.length > 0) {
        return trimmed;
      }
    }

    if (raw.district) {
      const nested = extractDistrictValue(raw.district);
      if (nested) {
        return nested;
      }
    }

    for (const value of Object.values(raw)) {
      if (typeof value === 'string') {
        const trimmed = value.trim();
        if (trimmed.length > 0) {
          return trimmed;
        }
      } else if (value && typeof value === 'object') {
        const nested = extractDistrictValue(value);
        if (nested) {
          return nested;
        }
      }
    }
  }

  return null;
}

function normalizeAddressString(raw) {
  if (typeof raw !== 'string') {
    return null;
  }

  const trimmed = raw.trim();
  return trimmed.length > 0 ? trimmed : null;
}

const DIRECT_DISTRICT_KEYS = [
  'addressLabel',
  'locationLabel',
  'formattedAddress',
  'formatted_address',
  'defaultAddressLabel',
];

const ADMINISTRATIVE_PART_KEYS = [
  'district',
  'kecamatan',
  'subDistrict',
  'sub_district',
    'subdistrict',
    'kelurahan',
    'desa',
    'village',
  'city',
  'kota',
  'regency',
  'kabupaten',
  'province',
  'provinsi',
  'state',
  'region',
    'wilayah',

];

function resolveDistrictFromData(data) {
  if (!data || typeof data !== 'object') {
    return null;
  }

  const direct = extractDistrictValue(data.district);
  if (direct) {
    return direct;
  }
  const directSegments = deriveBasicAddressSegments(data);
  if (directSegments?.district) {
    return directSegments.district;
  }

  for (const key of DIRECT_DISTRICT_KEYS) {
    if (key in data) {
      const candidate = extractDistrictValue(data[key]);
      if (candidate) {
        return candidate;
      }
    }
  }

  const combined = buildAdministrativeLabelFromMap(data);
  if (combined) {
    return combined;
  }
  const fallbackKeys = ['address', 'defaultAddress', 'serviceArea', 'location'];
  for (const key of fallbackKeys) {
    if (key in data) {
      const candidate = extractDistrictValue(data[key]);
      if (candidate) {
        return candidate;
      }
      if (data[key] && typeof data[key] === 'object') {
        const nested = resolveDistrictFromData(data[key]);
        if (nested) {
          return nested;
        }
                const nestedSegments = deriveBasicAddressSegments(data[key]);
                if (nestedSegments?.district) {
                  return nestedSegments.district;
                }
      }
    }
  }

  return null;
}
function buildAdministrativeLabelFromMap(data) {
  if (!data || typeof data !== 'object') {
    return null;
  }

  const parts = [];
  for (const key of ADMINISTRATIVE_PART_KEYS) {
    if (key in data) {
      const candidate = extractDistrictValue(data[key]);
      if (candidate) {
        const exists = parts.some((item) => item.toLowerCase() === candidate.toLowerCase());
        if (!exists) {
          parts.push(candidate.trim());
        }
      }
    }
  }

  return parts.length > 0 ? parts.join(', ') : null;
}

function resolveAddressSegments(rawData) {
  if (!rawData || typeof rawData !== 'object') {
    return null;
  }

  const data = Array.isArray(rawData)
    ? rawData.find((item) => item && typeof item === 'object') || null
    : rawData;

  if (!data || typeof data !== 'object') {
    return null;
  }
  const directSegments = deriveBasicAddressSegments(data);
  if (directSegments) {
    return directSegments;
  }
  const district =
    normalizeAddressString(data.district) ||
    normalizeAddressString(data.kecamatan) ||
    normalizeAddressString(data.subDistrict) ||
    normalizeAddressString(data.subdistrict) ||
    normalizeAddressString(data.sub_district);

  const city =
    normalizeAddressString(data.city) ||
    normalizeAddressString(data.kota) ||
    normalizeAddressString(data.regency) ||
    normalizeAddressString(data.kabupaten) ||
    normalizeAddressString(data.town);

  const province =
    normalizeAddressString(data.province) ||
    normalizeAddressString(data.provinsi) ||
    normalizeAddressString(data.state) ||
    normalizeAddressString(data.region);

  const detail =
    normalizeAddressString(data.detail) ||
    normalizeAddressString(data.addressLine) ||
    normalizeAddressString(data.address_line) ||
    normalizeAddressString(data.address) ||
    normalizeAddressString(data.street) ||
    normalizeAddressString(data.line1);

  const segments = { detail, district, city, province };

  if (Object.values(segments).some((value) => normalizeAddressString(value))) {
    return segments;
  }

  const nestedKeys = ['address', 'defaultAddress', 'serviceArea', 'location'];
  for (const key of nestedKeys) {
    if (key in data) {
      const nested = resolveAddressSegments(data[key]);
      if (nested) {
        return nested;
      }
    }
  }

  return null;
}
function deriveBasicAddressSegments(data) {
  if (!data || typeof data !== 'object') {
    return null;
  }

  const pickString = (value, { allowDistrictExtraction = false } = {}) => {
    if (typeof value === 'string') {
      return normalizeAddressString(value);
    }
    if (allowDistrictExtraction) {
      const candidate = extractDistrictValue(value);
      if (candidate) {
        return candidate;
      }
    }
    return null;
  };

  const pickFromKeys = (keys, options) => {
    for (const key of keys) {
      if (!Object.prototype.hasOwnProperty.call(data, key)) {
        continue;
      }
      const raw = data[key];
      const candidate = pickString(raw, options);
      if (candidate) {
        return candidate;
      }
    }
    return null;
  };

  const district =
    pickFromKeys(
      ['district', 'kecamatan', 'subDistrict', 'sub_district', 'subdistrict', 'kelurahan', 'desa', 'village'],
      { allowDistrictExtraction: true }
    ) || null;
  const city =
    pickFromKeys(['city', 'kota', 'regency', 'kabupaten', 'town', 'kota_kabupaten']) || null;
  const province =
    pickFromKeys(['province', 'provinsi', 'state', 'region', 'wilayah']) || null;
  const detail =
    pickFromKeys(
      ['detail', 'addressLine', 'address_line', 'address', 'street', 'line1', 'alamat', 'alamatLengkap', 'alamat_lengkap'],
      { allowDistrictExtraction: false }
    ) || null;

  const segments = { detail, district, city, province };
  const hasValue = Object.values(segments).some((value) => typeof value === 'string' && value.length > 0);
  return hasValue ? segments : null;
}

function buildAddressLabel(segments) {
  if (!segments || typeof segments !== 'object') {
    return null;
  }

  const parts = [segments.detail, segments.district, segments.city, segments.province]
    .map((value) => normalizeAddressString(value))
    .filter((value) => typeof value === 'string');

  const unique = [];
  for (const part of parts) {
    const exists = unique.some((item) => item.toLowerCase() === part.toLowerCase());
    if (!exists) {
      unique.push(part);
    }
  }

  return unique.length > 0 ? unique.join(', ') : null;
}

function arraysEqual(a, b) {
  if (a === b) {
    return true;
  }

  if (!Array.isArray(a) || !Array.isArray(b)) {
    return false;
  }

  if (a.length !== b.length) {
    return false;
  }

  return a.every((value, index) => value === b[index]);
}

async function recomputeProviderBusyDates(providerId) {
  if (!providerId) {
    return 0;
  }

  const profileRef = db.collection('provider_profiles').doc(providerId);

  if (!Array.isArray(ACTIVE_PROVIDER_ORDER_STATUSES) || ACTIVE_PROVIDER_ORDER_STATUSES.length === 0) {
    await profileRef.set({ busyDates: [] }, { merge: true });
    return 0;
  }

  const activeOrdersQuery = db
    .collection('orders')
    .where('providerId', '==', providerId)
    .where('status', 'in', ACTIVE_PROVIDER_ORDER_STATUSES);

  const snapshot = await activeOrdersQuery.get();

  const busyDates = sanitizeDateList(
    snapshot.docs.map((doc) => {
      const data = doc.data() || {};
      return normalizeScheduledDate(data.scheduledDate);
    })
  );

  await profileRef.set({ busyDates }, { merge: true });

  return busyDates.length;
}

async function applyProviderAvailabilityChange({
  providerId,
  scheduledDate,
  action,
  reason = 'UNSPECIFIED',
  forceAvailability,
} = {}) {
  const normalizedDate = normalizeScheduledDate(scheduledDate);

  if (!providerId || !action) {
    return null;
  }

  const profileRef = db.collection('provider_profiles').doc(providerId);

  try {
    await db.runTransaction(async (tx) => {
      const snapshot = await tx.get(profileRef);
      if (!snapshot.exists) {
        functions.logger.warn('[PROVIDER_AVAILABILITY_MISSING_PROFILE]', {
          providerId,
          action,
          reason,
        });
        return;
      }

      const data = snapshot.data() || {};
      const currentDates = sanitizeDateList(data.availableDates);
      let nextDates = currentDates;

      if (action === 'consume') {
        if (normalizedDate) {
          nextDates = currentDates.filter((date) => date !== normalizedDate);
        }
      } else if (action === 'release') {
        if (normalizedDate && !currentDates.includes(normalizedDate)) {
          nextDates = currentDates.concat(normalizedDate);
          nextDates.sort();
        }
      } else {
        functions.logger.warn('[PROVIDER_AVAILABILITY_UNKNOWN_ACTION]', {
          providerId,
          action,
          reason,
        });
        return;
      }

      const updates = {};
      let changed = false;

      if (!arraysEqual(nextDates, currentDates)) {
        updates.availableDates = nextDates;
        changed = true;
      }

      const resolvedAvailability =
        typeof forceAvailability === 'boolean'
          ? forceAvailability
          : nextDates.length > 0;

      const currentAvailability =
        typeof data.available === 'boolean'
          ? data.available
          : typeof data.isAvailable === 'boolean'
            ? data.isAvailable
            : null;

      if (currentAvailability !== resolvedAvailability) {
        updates.available = resolvedAvailability;
        changed = true;
      }

      if (changed) {
        tx.update(profileRef, updates);
        functions.logger.info('[PROVIDER_AVAILABILITY_UPDATED]', {
          providerId,
          action,
          reason,
          scheduledDate: normalizedDate,
          availableDates: updates.availableDates || currentDates,
          available:
            'available' in updates ? updates.available : currentAvailability,
        });
      }
    });
  } catch (error) {
    functions.logger.error('[PROVIDER_AVAILABILITY_UPDATE_FAILED]', {
      providerId,
      action,
      reason,
      scheduledDate: normalizedDate,
      message: error?.message || 'Unknown error',
    });
    throw error;
  }

  return null;
}
/* =========================
   Helpers: Env & Validation
   ========================= */

// Midtrans dulu memakai prefix SB- untuk sandbox. Kunci generasi baru tidak memakai prefix itu.
// Karena itu, JANGAN paksa infer ke production hanya karena prefix 'Mid-'.
function inferEnvFromKeyPrefix(key) {
  if (!key) return null;
  if (key.startsWith('SB-')) return false; // sandbox (legacy key style)
  // newer keys have no reliable prefix → unknown
  return null;
}

function resolveMidtransEnv() {
  const serverKey = process.env.MIDTRANS_SERVER_KEY || '';
  const clientKey = process.env.MIDTRANS_CLIENT_KEY || '';

  // Optional manual override
  if (typeof process.env.MIDTRANS_IS_PRODUCTION === 'string') {
    const forced = process.env.MIDTRANS_IS_PRODUCTION.trim().toLowerCase();
    if (forced === 'true' || forced === 'false') {
      return {
        isProduction: forced === 'true',
        serverKey,
        clientKey,
        reason: 'ENV_OVERRIDE',
      };
    }
  }

  const envFromServer = inferEnvFromKeyPrefix(serverKey);
  const envFromClient = inferEnvFromKeyPrefix(clientKey);

  if (envFromServer !== null && envFromClient !== null && envFromClient !== envFromServer) {
    throw new Error(
      `ENV mismatch: SERVER_KEY (${serverKey.slice(0, 8)}) dan CLIENT_KEY (${clientKey.slice(0, 8)}) beda environment.`
    );
  }

  let isProduction;
  let reason;
  if (envFromServer !== null) {
    isProduction = envFromServer;
    reason = 'INFER_FROM_SERVER_PREFIX';
  } else if (envFromClient !== null) {
    isProduction = envFromClient;
    reason = 'INFER_FROM_CLIENT_PREFIX';
  } else {
    // Default aman → sandbox
    isProduction = false;
    reason = 'DEFAULT_SANDBOX';
  }

  return { isProduction, serverKey, clientKey, reason };
}

function logEnv(where, resolved) {
  functions.logger.info(`[MIDTRANS:${where}]`, {
    isProduction: resolved.isProduction,
    serverKeyPrefix: (resolved.serverKey || '').slice(0, 8),
    clientKeyPrefix: (resolved.clientKey || '').slice(0, 8),
    decideBy: resolved.reason,
    runtime: process.env.FUNCTIONS_EMULATOR ? 'emulator' : 'cloud',
  });
}

/**
 * ============================================================
 * 1) CREATE MIDTRANS TRANSACTION (Callable v2)
 * ============================================================
 */
exports.createMidtransTransaction = functions.https.onCall(
  { secrets: ['MIDTRANS_SERVER_KEY', 'MIDTRANS_CLIENT_KEY'] },
  async (request) => {
    if (!request.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'Anda harus login.');
    }

    const resolved = resolveMidtransEnv();
    logEnv('CREATE', resolved);

    const { orderId } = request.data || {};
    if (!orderId) {
      throw new functions.https.HttpsError('invalid-argument', 'OrderId wajib diisi.');
    }

    // User
    const userId = request.auth.uid;
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'User tidak ditemukan.');
    }

    const user = userDoc.data() || {};
    const customerName = String(user.fullName || 'Customer').trim();
    const customerEmail = String(user.email || 'user@example.com').trim();
    const customerPhone = String(user.phoneNumber || '08123456789').trim();
    const sanitizedPhone = customerPhone.replace(/[^0-9+]/g, '');

    // Order
    const orderRef = db.collection('orders').doc(orderId);
    const orderDoc = await orderRef.get();
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Pesanan tidak ditemukan.');
    }

    const order = orderDoc.data() || {};
        const adminFee =
          typeof order.adminFee === 'number' ? Number(order.adminFee) : ADMIN_FEE;
        const discountAmount = Number(order.discountAmount || 0);

        // Determine grossAmount. Prefer detailed items, then explicit total, then fallback to basePrice * quantity.
        const items = Array.isArray(order?.serviceSnapshot?.items)
          ? order.serviceSnapshot.items
          : [];

        let grossAmount = 0;
        if (items.length > 0) {
          const itemsTotal = items.reduce(
            (sum, item) => sum + Number(item?.lineTotal || 0),
            0
          );
          grossAmount = Math.max(0, itemsTotal + adminFee - discountAmount);
        } else if (typeof order.totalAmount === 'number' && order.totalAmount > 0) {
          grossAmount = Math.max(0, Number(order.totalAmount));
        } else {
          const basePrice = Number(order?.serviceSnapshot?.basePrice);
          const quantity = Number(order?.quantity || 1);
          const lineTotal = basePrice * quantity;
          grossAmount = Math.max(0, lineTotal + adminFee - discountAmount);
        }

        if (!grossAmount || grossAmount <= 0) {
          throw new functions.https.HttpsError(
            'invalid-argument',
            'Harga layanan tidak valid.'
          );
    }

    const [firstName, ...rest] = customerName.split(' ').filter(Boolean);
    const lastName = rest.join(' ') || 'User';

    const payload = {
            transaction_details: { order_id: orderId, gross_amount: grossAmount },
      customer_details: {
        first_name: firstName || 'Customer',
        last_name: lastName,
        email: customerEmail,
        phone: sanitizedPhone.length >= 9 ? sanitizedPhone : '08123456789',
        billing_address: {
          first_name: firstName || 'Customer',
          last_name: lastName,
          address: String(order.addressText || '-'),
          city: String(order.city || '-'),
          postal_code: '11440',
          phone: sanitizedPhone,
          country_code: 'IDN',
        },
        shipping_address: {
          first_name: firstName || 'Customer',
          last_name: lastName,
          address: String(order.addressText || '-'),
          city: String(order.city || '-'),
          postal_code: '11440',
          phone: sanitizedPhone,
          country_code: 'IDN',
        },
      },
      // enabled_payments: ['gopay', 'bank_transfer', 'credit_card', 'echannel', 'cstore'],
    };
    await orderRef.set({ adminFee, totalAmount: grossAmount }, { merge: true });

    functions.logger.info('[CREATE_TX] Requesting Snap token', {
      orderId,
            grossAmount,
            adminFee,
            discountAmount,
      isProduction: resolved.isProduction,
    });

    // 1) Coba via SDK Snap
    const snap = new midtransClient.Snap({
      isProduction: resolved.isProduction,
      serverKey: resolved.serverKey,
      clientKey: resolved.clientKey,
    });

    try {
      const tx = await snap.createTransaction(payload);
      await orderRef.set(
        { paymentGatewayInfo: { token: tx.token, redirect_url: tx.redirect_url, env: resolved.isProduction ? 'production' : 'sandbox' } },
        { merge: true }
      );
      functions.logger.info('[CREATE_TX] Token created (SDK)', {
        orderId,
        tokenPrefix: String(tx.token || '').slice(0, 8),
      });
      return { token: tx.token, redirectUrl: tx.redirect_url }; // <— penting!

    } catch (sdkErr) {
      // 2) Fallback REST jika SDK error (mis. ReferenceError 'resolved is not defined' dari lib pihak ketiga)
      functions.logger.warn('SNAP_SDK_FAILED_FALLBACK', { message: sdkErr && sdkErr.message });
      try {
        const base = resolved.isProduction ? 'https://app.midtrans.com' : 'https://app.sandbox.midtrans.com';
        const auth = Buffer.from(`${resolved.serverKey}:`).toString('base64');
        const resp = await fetchWithFallback(`${base}/snap/v1/transactions`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
            Authorization: `Basic ${auth}`,
          },
          body: JSON.stringify(payload),
        });
        const data = await resp.json();
        if (!resp.ok) {
          const msg = data?.status_message || (data?.error_messages && data.error_messages.join(', ')) || 'Gagal membuat transaksi (REST).';
          throw new Error(msg);
        }
        await orderRef.set(
          { paymentGatewayInfo: { token: data.token, redirect_url: data.redirect_url, env: resolved.isProduction ? 'production' : 'sandbox' } },
          { merge: true }
        );
        functions.logger.info('[CREATE_TX] Token created (REST)', {
          orderId,
          tokenPrefix: String(data.token || '').slice(0, 8),
        });
        return { token: data.token, redirectUrl: data.redirect_url };
      } catch (restErr) {
        const msg = restErr?.message || sdkErr?.message || 'Gagal membuat transaksi di Midtrans.';
        functions.logger.error('[CREATE_TX_FAILED]', { orderId, message: msg });
        throw new functions.https.HttpsError('internal', msg);
      }
    }
  }
);

/**
 * ============================================================
* 2) CLAIM ORDER (Callable v2)
 * ============================================================
 */
exports.claimOrder = functions.https.onCall(async (request) => {
  if (!request.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Anda harus login.');
  }

  const { orderId, scheduledDate } = request.data || {};
  if (!orderId) {
    throw new functions.https.HttpsError('invalid-argument', 'OrderId wajib diisi.');
  }

 const requestedScheduledDate = normalizeScheduledDate(scheduledDate);
  if (!requestedScheduledDate) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Tanggal penjadwalan wajib diisi dan harus berupa string.'
    );
  }

  const isoDatePattern = /^\d{4}-\d{2}-\d{2}$/;
  if (!isoDatePattern.test(requestedScheduledDate)) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Format tanggal tidak valid. Gunakan format yyyy-MM-dd.'
    );
  }

  const uid = request.auth.uid;
  const orderRef = db.collection('orders').doc(orderId);
   const profileRef = db.collection('provider_profiles').doc(uid);
  await db.runTransaction(async (tx) => {
    const [orderSnap, profileSnap] = await Promise.all([
      tx.get(orderRef),
      tx.get(profileRef),
    ]);
 if (!orderSnap.exists) {
      throw new functions.https.HttpsError('not-found', 'Pesanan tidak ditemukan.');
    }
     const orderData = orderSnap.data() || {};
        if (orderData.providerId) {
          throw new functions.https.HttpsError('failed-precondition', 'Pesanan sudah memiliki provider.');
        }
        if (orderData.status !== 'searching_provider') {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'Pesanan tidak dalam status pencarian provider.'
          );
        }

        if (!profileSnap.exists) {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'Profil provider tidak ditemukan.'
          );
        }

        const profileData = profileSnap.data() || {};
        const availableDates = sanitizeDateList(profileData.availableDates);
        if (!availableDates.includes(requestedScheduledDate)) {
          const message = 'Tanggal yang dipilih tidak ada dalam jadwal tersedia Anda.';
          const details = {
            reason: 'SCHEDULE_NOT_AVAILABLE',
            requestedScheduledDate,
            availableDates,
          };
          throw new functions.https.HttpsError('failed-precondition', message, details);
        }

        if (ACTIVE_PROVIDER_ORDER_STATUSES.length > 0) {
          const activeOrdersQuery = db
            .collection('orders')
            .where('providerId', '==', uid)
            .where('status', 'in', ACTIVE_PROVIDER_ORDER_STATUSES);

          const activeOrdersSnap = await tx.get(activeOrdersQuery);
          const conflictingDoc = activeOrdersSnap.docs.find((doc) => {
            const existingData = doc.data() || {};
            const existingScheduledDate = normalizeScheduledDate(existingData.scheduledDate);

            if (!existingScheduledDate) {
              return true;
            }

            return existingScheduledDate === requestedScheduledDate;
      });

 if (conflictingDoc) {
         const conflictingData = conflictingDoc.data() || {};
         const conflictingScheduledDate = normalizeScheduledDate(conflictingData.scheduledDate);
         const message = conflictingScheduledDate
           ? 'Anda sudah memiliki pesanan aktif pada tanggal tersebut. Selesaikan pesanan sebelumnya sebelum mengambil order baru.'
           : 'Anda masih memiliki pesanan aktif yang belum selesai. Selesaikan pesanan sebelumnya sebelum mengambil order baru.';

         const details = {
           reason: 'ACTIVE_ORDER_CONFLICT',
           conflictingOrderId: conflictingDoc.id,
           conflictingStatus: conflictingData.status || null,
           conflictingScheduledDate,
           requestedOrderId: orderId,
           requestedScheduledDate,
           message,
         };

         functions.logger.warn('[CLAIM_ORDER_CONFLICT]', {
           orderId,
           providerId: uid,
           requestedScheduledDate,
           conflictingOrderId: conflictingDoc.id,
           conflictingScheduledDate,
           conflictingStatus: conflictingData.status || null,
         });

         throw new functions.https.HttpsError('failed-precondition', message, details);
       }
     }

     tx.update(orderRef, {
       scheduledDate: requestedScheduledDate,
       providerId: uid,
       status: 'pending',
     });
   });

   await applyProviderAvailabilityChange({
     providerId: uid,
     scheduledDate: requestedScheduledDate,
     action: 'consume',
     reason: 'CLAIM_ORDER',
   });

   functions.logger.info('[CLAIM_ORDER]', {
     orderId,
     providerId: uid,
     scheduledDate: requestedScheduledDate,
   });
  return { success: true };
});

/**
 * ============================================================
 * 3) UPGRADE TO PROVIDER (Callable v2)
  * ============================================================
  */
exports.upgradeToProvider = functions.https.onCall(async (request) => {
  if (!request.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Anda harus login.');
  }


   const uid = request.auth.uid;
   const userRef = db.collection('users').doc(uid);
   const userSnap = await userRef.get();
   if (!userSnap.exists) {
     throw new functions.https.HttpsError('not-found', 'User tidak ditemukan.');
   }

   const userData = userSnap.data() || {};

   await db.runTransaction(async (tx) => {
     tx.update(userRef, {
       roles: admin.firestore.FieldValue.arrayUnion('provider'),
     });

     const profileRef = db.collection('provider_profiles').doc(uid);
     const profileSnap = await tx.get(profileRef);
     if (!profileSnap.exists) {
       tx.set(profileRef, {
         uid,
         fullName: userData.fullName || '',
         primaryCategoryId: '',
         bio: '',
         available: true,
         acceptsBasicOrders: true,
         averageRating: 0,
         totalReviews: 0,
         profilePictureUrl: userData.profilePictureUrl || null,
         location: null,
       });
     }
   });

    functions.logger.info('[UPGRADE_TO_PROVIDER]', { uid });
    return { success: true };
  });

 /**
  * ============================================================
  * 3B) GET PROVIDER PUBLIC STATS (Callable v2)
  * ============================================================
  */
 exports.getProviderPublicStats = functions.https.onCall(async (request) => {
   if (!request.auth) {
          functions.logger.info('[GET_PROVIDER_PUBLIC_STATS_UNAUTH]', {
            providerId: request.data?.providerId,
          });
   }

   const rawProviderId = request.data?.providerId;
   if (typeof rawProviderId !== 'string') {
     throw new functions.https.HttpsError('invalid-argument', 'providerId wajib berupa string.');
   }

   const providerId = rawProviderId.trim();
   if (!providerId) {
     throw new functions.https.HttpsError('invalid-argument', 'providerId tidak boleh kosong.');
   }

   try {
     const profileRef = db.collection('provider_profiles').doc(providerId);
     const userRef = db.collection('users').doc(providerId);

     const [profileSnap, userSnap, ordersSnap] = await Promise.all([
       profileRef.get(),
       userRef.get(),
       db
         .collection('orders')
         .where('providerId', '==', providerId)
         .where('status', '==', 'completed')
         .get(),
     ]);

     let district = '';
          let addressSegments = null;

     const profileData = profileSnap.exists ? profileSnap.data() || {} : {};
     if (profileSnap.exists) {
       district = resolveDistrictFromData(profileData) || '';
              addressSegments = resolveAddressSegments(profileData) || null;

     }

     const userData = userSnap.exists ? userSnap.data() || {} : {};
     if (!district && userSnap.exists) {
       district =
         resolveDistrictFromData(userData) ||
                 extractDistrictValue(userData.defaultAddress) ||
                 '';
             }
             if (!addressSegments && userSnap.exists) {
               addressSegments =
                 resolveAddressSegments(userData) || resolveAddressSegments(userData.defaultAddress) || null;
     }

          if (!district || !addressSegments) {

       const addressesSnap = await userRef
         .collection('addresses')
         .orderBy('isDefault', 'desc')
         .limit(5)
         .get();

       for (const doc of addressesSnap.docs) {
         const data = doc.data() || {};
          const resolvedDistrict =
                    resolveDistrictFromData(data) || extractDistrictValue(data.district) || '';
                  const resolvedSegments = resolveAddressSegments(data);

                  if (!district && resolvedDistrict) {
                    district = resolvedDistrict;
                  }

                  if (!addressSegments && resolvedSegments) {
                    addressSegments = resolvedSegments;
                  }

                  if (district && addressSegments) {
           break;
         }
       }
     }
 if ((!district || !district.trim()) && addressSegments) {
       district =
         normalizeAddressString(addressSegments.district) ||
         normalizeAddressString(addressSegments.city) ||
         normalizeAddressString(addressSegments.province) ||
         '';
     }

     const completedOrders = ordersSnap.size;
     const label = buildAddressLabel(addressSegments);

   functions.logger.info('[GET_PROVIDER_PUBLIC_STATS_RESOLUTION]', {
         providerId,
         profileHasDistrict: Boolean(profileData?.district || profileData?.defaultAddress),
         userHasDistrict: Boolean(userData?.district || userData?.defaultAddress),
         resolvedDistrict: district || null,
         addressSegments,
       });

       if (!district || !district.trim()) {
         functions.logger.warn('[GET_PROVIDER_PUBLIC_STATS_DISTRICT_MISSING]', {
           providerId,
           profileDistrict: profileData?.district || null,
           userDistrict: userData?.district || null,
           addressSegments,
         });
       }

       return {
         completedOrders,
         district: district || null,
                addressLabel: label || null,
                address: addressSegments,
       };
     } catch (error) {
     functions.logger.error('[GET_PROVIDER_PUBLIC_STATS_FAILED]', {
       providerId,
       message: error?.message || 'Unknown error',
     });
     throw new functions.https.HttpsError('internal', 'Gagal mengambil statistik penyedia.');
   }
 });

 /**
  * ============================================================
  * 4) MIDTRANS WEBHOOK (HTTP v2)
 * ============================================================
 */
exports.midtransWebhookHandler = functions.https.onRequest(
  { secrets: ['MIDTRANS_SERVER_KEY', 'MIDTRANS_CLIENT_KEY'] },
  async (req, res) => {
    try {
      const resolved = resolveMidtransEnv();
      logEnv('WEBHOOK', resolved);

      const coreApi = new midtransClient.CoreApi({
        isProduction: resolved.isProduction,
        serverKey: resolved.serverKey,
        clientKey: resolved.clientKey,
      });

      const notificationJson = req.body;
      const statusResponse = await coreApi.transaction.notification(notificationJson);

      const orderId = statusResponse.order_id;
      const transactionStatus = statusResponse.transaction_status;
      const fraudStatus = statusResponse.fraud_status;

            const normalizedTransactionStatus = String(transactionStatus || '').toLowerCase();
            const normalizedFraudStatus = String(fraudStatus || '').toLowerCase();

            functions.logger.info('[WEBHOOK]', {
              orderId,
              transactionStatus,
              fraudStatus,
              normalizedTransactionStatus,
              normalizedFraudStatus,
            });

      const orderRef = db.collection('orders').doc(orderId);

      let newPaymentStatus = 'pending';
      let newOrderStatus = null;

      if (['capture', 'settlement'].includes(normalizedTransactionStatus)) {
              if (normalizedFraudStatus === 'accept') {
                newPaymentStatus = 'paid';
          const snap = await orderRef.get();
          if (snap.exists) {
            const data = snap.data();
            if (data?.status === 'awaiting_payment') {
              if (data.orderType === 'basic') {
                newOrderStatus = 'searching_provider';
              } else if (data.orderType === 'direct') {
                newOrderStatus = 'awaiting_provider_confirmation';
                await createChatRoom(data, orderId);
              }
            }
          }
        }
      } else if (['cancel', 'deny', 'expire'].includes(normalizedTransactionStatus)) {
             newPaymentStatus = normalizedTransactionStatus;
        newOrderStatus = 'cancelled';
      }

      const update = { paymentStatus: newPaymentStatus };
      if (newOrderStatus) update.status = newOrderStatus;

      await orderRef.update(update);

      functions.logger.info('[WEBHOOK_UPDATED]', {
        orderId,
        paymentStatus: newPaymentStatus,
        orderStatus: newOrderStatus || '(unchanged)',
      });

      res.status(200).send('OK');
    } catch (error) {
      functions.logger.error('[WEBHOOK_FAILED]', { message: error?.message || 'Unknown error' });
      res.status(500).send('Internal Server Error');
    }
  }
);

/**
 * ============================================================
 * 5) FIND PROVIDER FOR BASIC ORDER (Firestore Trigger v2)
 * ============================================================
 */
exports.findProviderForBasicOrder = onDocumentUpdated('orders/{orderId}', async () => {
  // Trigger disabled. Provider assignment is handled explicitly by claimOrder.
  return null;
});

/**
 * ============================================================
 * 6) ON PROVIDER ASSIGNED - CREATE CHAT (Firestore Trigger v2)
 * ============================================================
 */
exports.onProviderAssigned = onDocumentUpdated('orders/{orderId}', async (event) => {
  const after = event.data.after.data();
  const before = event.data.before.data();

  if (!before.providerId && after.providerId && after.orderType === 'basic') {
    functions.logger.info('[CREATE_CHAT]', { orderId: event.params.orderId });
    await createChatRoom(after, event.params.orderId);
  }
  return null;
});
/**
 * ============================================================
 * 6B) SYNC PROVIDER AVAILABILITY (Firestore Trigger v2)
 * ============================================================
 */
exports.syncProviderAvailability = onDocumentWritten('orders/{orderId}', async (event) => {
  const orderId = event.params.orderId;
  const beforeSnap = event.data?.before;
  const afterSnap = event.data?.after;

  const beforeData = beforeSnap && beforeSnap.exists && typeof beforeSnap.data === 'function'
    ? beforeSnap.data()
    : null;
  const afterData = afterSnap && afterSnap.exists && typeof afterSnap.data === 'function'
    ? afterSnap.data()
    : null;

  if (!beforeData && !afterData) {
    return null;
  }

  const tasks = [];

  const beforeProvider = beforeData?.providerId || null;
  const afterProvider = afterData?.providerId || null;

  const beforeDate = beforeData ? normalizeScheduledDate(beforeData.scheduledDate) : null;
  const afterDate = afterData ? normalizeScheduledDate(afterData.scheduledDate) : null;

  const beforeStatus = beforeData?.status || null;
  const afterStatus = afterData?.status || null;

  if (afterProvider && !beforeProvider) {
    tasks.push(
      applyProviderAvailabilityChange({
        providerId: afterProvider,
        scheduledDate: afterDate,
        action: 'consume',
        reason: 'PROVIDER_ASSIGNED',
      })
    );
  } else if (afterProvider && beforeProvider && afterProvider !== beforeProvider) {
    tasks.push(
      applyProviderAvailabilityChange({
        providerId: beforeProvider,
        scheduledDate: beforeDate,
        action: 'release',
        reason: 'PROVIDER_CHANGED_RELEASE',
      })
    );
    tasks.push(
      applyProviderAvailabilityChange({
        providerId: afterProvider,
        scheduledDate: afterDate,
        action: 'consume',
        reason: 'PROVIDER_CHANGED_CONSUME',
      })
    );
  } else if (!afterProvider && beforeProvider) {
    tasks.push(
      applyProviderAvailabilityChange({
        providerId: beforeProvider,
        scheduledDate: beforeDate,
        action: 'release',
        reason: 'PROVIDER_REMOVED',
      })
    );
  }

  if (afterProvider && beforeProvider === afterProvider) {
    const providerId = afterProvider;
    if (beforeDate !== afterDate) {
      if (beforeDate) {
        tasks.push(
          applyProviderAvailabilityChange({
            providerId,
            scheduledDate: beforeDate,
            action: 'release',
            reason: 'SCHEDULE_CHANGED_RELEASE',
          })
        );
      }
      if (afterDate) {
        tasks.push(
          applyProviderAvailabilityChange({
            providerId,
            scheduledDate: afterDate,
            action: 'consume',
            reason: 'SCHEDULE_CHANGED_CONSUME',
          })
        );
      }
    }
  }

  if (afterProvider && beforeStatus !== afterStatus) {
    if (afterStatus === 'cancelled' || afterStatus === 'completed') {
      const relevantDate = afterDate || beforeDate;
      tasks.push(
        applyProviderAvailabilityChange({
          providerId: afterProvider,
          scheduledDate: relevantDate,
          action: 'release',
          reason: `STATUS_${String(afterStatus || '').toUpperCase()}`,
        })
      );
    }
  }

  if (!afterData && beforeProvider) {
    tasks.push(
      applyProviderAvailabilityChange({
        providerId: beforeProvider,
        scheduledDate: beforeDate,
        action: 'release',
        reason: 'ORDER_DELETED',
      })
    );
  }

  if (tasks.length > 0) {
    await Promise.all(tasks);
    functions.logger.info('[SYNC_PROVIDER_AVAILABILITY_TASKS]', {
      orderId,
      taskCount: tasks.length,
    });
  }

  return null;
});

/**
 * ============================================================
 * 6C) SYNC PROVIDER BUSY DATES (Firestore Trigger v2)
 * ============================================================
 */
exports.syncProviderBusyDates = onDocumentWritten('orders/{orderId}', async (event) => {
  const orderId = event.params.orderId;
  const beforeSnap = event.data?.before;
  const afterSnap = event.data?.after;

  const beforeData = beforeSnap && beforeSnap.exists && typeof beforeSnap.data === 'function'
    ? beforeSnap.data()
    : null;
  const afterData = afterSnap && afterSnap.exists && typeof afterSnap.data === 'function'
    ? afterSnap.data()
    : null;

  if (!beforeData && !afterData) {
    return null;
  }

  const beforeProvider = beforeData?.providerId || null;
  const afterProvider = afterData?.providerId || null;

  const beforeStatus = typeof beforeData?.status === 'string' ? beforeData.status : null;
  const afterStatus = typeof afterData?.status === 'string' ? afterData.status : null;

  const beforeDate = beforeData ? normalizeScheduledDate(beforeData.scheduledDate) : null;
  const afterDate = afterData ? normalizeScheduledDate(afterData.scheduledDate) : null;

  const beforeActive = Boolean(
    beforeProvider && beforeStatus && ACTIVE_PROVIDER_ORDER_STATUSES.includes(beforeStatus)
  );
  const afterActive = Boolean(
    afterProvider && afterStatus && ACTIVE_PROVIDER_ORDER_STATUSES.includes(afterStatus)
  );

  let shouldSync = false;
  if (beforeActive !== afterActive) {
    shouldSync = true;
  } else if (beforeActive && afterActive) {
    if (beforeProvider !== afterProvider || beforeDate !== afterDate) {
      shouldSync = true;
    }
  }

  if (!shouldSync) {
    return null;
  }

  const providerIds = new Set();
  if (beforeActive && beforeProvider) {
    providerIds.add(beforeProvider);
  }
  if (afterActive && afterProvider) {
    providerIds.add(afterProvider);
  }

  if (providerIds.size === 0) {
    return null;
  }

  await Promise.all(
    Array.from(providerIds).map(async (providerId) => {
      try {
        const busyDatesCount = await recomputeProviderBusyDates(providerId);
        functions.logger.info('[SYNC_PROVIDER_BUSY_DATES_UPDATED]', {
          orderId,
          providerId,
          busyDatesCount,
        });
      } catch (error) {
        functions.logger.error('[SYNC_PROVIDER_BUSY_DATES_FAILED]', {
          orderId,
          providerId,
          message: error?.message || 'Unknown error',
        });
        throw error;
      }
    })
  );

  return null;
});

/**
 * ============================================================
 * 7) REFUND ON CANCELLATION (Firestore Trigger v2)
 * ============================================================
 */
async function handleOrderCancelled(event) {
  const after = event?.data?.after?.data?.();

  if (!after) {
    return null;
  }

     if (after.status === 'cancelled' && after.paymentStatus === 'paid') {

     const orderId = event.params.orderId;
     const { amount, source, adjustments, policy } = calculateRefundBreakdown(after);
     const customerId = after.customerId;

     if (!customerId || !(amount > 0)) {
       functions.logger.warn('[REFUND_SKIPPED]', {
         orderId,
         customerId: customerId || '(missing)',
         source,
         adjustments,
         policy,
       });
       return null;
     }

     functions.logger.info('[REFUND_CALCULATED]', {
       orderId,
       customerId,
       amount,
       source,
       adjustments,
       policy,
     });

    const customerRef = db.collection('users').doc(customerId);
    await db.runTransaction(async (tx) => {
      const customerDoc = await tx.get(customerRef);
      if (!customerDoc.exists) throw new Error('User tidak ditemukan.');

      const currentBalanceRaw = Number(customerDoc.data().balance || 0);
            const currentBalance = Number.isFinite(currentBalanceRaw) ? currentBalanceRaw : 0;
            const newBalance = currentBalance + amount;

      tx.update(customerRef, { balance: newBalance });

      const refundRef = db.collection('transactions').doc();
      tx.set(refundRef, {
        userId: customerId,
        orderId,
        type: 'REFUND_IN',
        amount,
        description: 'Pengembalian dana dari pesanan yang dibatalkan',
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });
  }
  return null;
}

exports.onOrderCancelled = onDocumentUpdated('orders/{orderId}', handleOrderCancelled);
exports.__testHandleOrderCancelled = handleOrderCancelled;
/**
 * ============================================================
 * 8) CANCEL EXPIRED ORDERS (Scheduler v2)
 * ============================================================
 */
exports.cancelExpiredOrders = functions.scheduler.onSchedule('every 60 minutes', async () => {
  const cutoff = new Date(Date.now() - 60 * 60 * 1000);
  const snapshot = await db
    .collection('orders')
    .where('status', '==', 'awaiting_payment')
    .where('createdAt', '<', cutoff)
    .get();

  if (snapshot.empty) {
    return null;
  }

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.update(doc.ref, { status: 'cancelled', paymentStatus: 'expire' });
  });
  await batch.commit();
  return null;
});
/* =========================
   Helper: Create chat room
   ========================= */

async function createChatRoom(orderData, orderId) {
  try {
    const { customerId, providerId, serviceSnapshot } = orderData;
    if (!customerId || !providerId) return;

    const [customerDoc, providerUserDoc] = await Promise.all([
      db.collection('users').doc(customerId).get(),
      db.collection('users').doc(providerId).get(),
    ]);

    if (!customerDoc.exists || !providerUserDoc.exists) return;

    const customer = customerDoc.data() || {};
    const providerUser = providerUserDoc.data() || {};

    const chatData = {
      orderId,
      participantIds: [customerId, providerId],
      lastMessage: null,
      serviceName: (serviceSnapshot && serviceSnapshot.serviceName) || 'Layanan',
      participantNames: {
        [customerId]: customer.fullName,
        [providerId]: providerUser.fullName,
      },
      participantPictures: {
        [customerId]: customer.profilePictureUrl || null,
        [providerId]: providerUser.profilePictureUrl || null,
      },
    };

    await db.collection('chats').doc(orderId).set(chatData);
    functions.logger.info('[CHAT_CREATED]', { orderId });
  } catch (error) {
    functions.logger.error('[CHAT_CREATE_FAILED]', { orderId, message: error?.message || 'Unknown error' });
  }
}
function resolveWebApiKey() {
  try {
    // Coba baca dari parameter yang sudah didefinisikan
    const key = appWebApiKey.value();
    if (key) return key;
  } catch (error) {
    functions.logger.debug('Gagal membaca parameter FIREBASE_WEB_API_KEY.', error.message);
  }

  // Jika parameter tidak ada, fallback ke metode lama (sebagai pengaman)
  const fallbackKey = resolveConfigString(['app', 'firebase_web_api_key']);
  if (fallbackKey) return fallbackKey;

  return null; // Kembalikan null jika tidak ditemukan sama sekali
}

function resolveEmailOtpContinueUrl() {
     let parameterValue = null;
      try {
        parameterValue = readOptionalString(emailOtpContinueUrl.value());
      } catch (error) {
        functions.logger.debug('[PARAM_UNAVAILABLE]', {
          param: 'EMAIL_OTP_CONTINUE_URL',
          message: error?.message,
        });
      }

      if (parameterValue) {
        return parameterValue;
      }

      const envValue = readOptionalString(process.env.APP_EMAIL_OTP_CONTINUE_URL);
      if (envValue) {
        return envValue;
  }

  const continueUrl =
      resolveConfigString(['app', 'email_otp_continue_url']) ||
            resolveConfigString(['identity', 'email_otp_continue_url']) ||
            resolveConfigString(['identity', 'continue_url']) ||
            resolveConfigString(['email', 'otp_continue_url']) ||
    null;

  if (continueUrl) {
    return continueUrl;
  }

  throw new HttpsError(
    'failed-precondition',
    'Konfigurasi Firebase belum lengkap. Set nilai EMAIL_OTP_CONTINUE_URL atau app.email_otp_continue_url.'
  );
}
function resolveEmailOtpActionCodeSettings() {
  const continueUrl = resolveEmailOtpContinueUrl();

  const dynamicLinkDomain =
      readOptionalString(process.env.EMAIL_OTP_DYNAMIC_LINK_DOMAIN) ||
      resolveConfigString(['app', 'email_otp_dynamic_link_domain']) ||
      resolveConfigString(['identity', 'email_otp_dynamic_link_domain']) ||
      resolveConfigString(['email', 'otp_dynamic_link_domain']) ||
    null;

  const iosBundleId =
      readOptionalString(process.env.EMAIL_OTP_IOS_BUNDLE_ID) ||
      resolveConfigString(['app', 'email_otp_ios_bundle_id']) ||
      resolveConfigString(['identity', 'email_otp_ios_bundle_id']) ||
      resolveConfigString(['email', 'otp_ios_bundle_id']) ||
    null;

  const androidPackageName =
      readOptionalString(process.env.EMAIL_OTP_ANDROID_PACKAGE_NAME) ||
      resolveConfigString(['app', 'email_otp_android_package_name']) ||
      resolveConfigString(['identity', 'email_otp_android_package_name']) ||
      resolveConfigString(['email', 'otp_android_package_name']) ||
    null;

  const androidMinimumVersion =
      readOptionalString(process.env.EMAIL_OTP_ANDROID_MIN_VERSION) ||
      resolveConfigString(['app', 'email_otp_android_min_version']) ||
      resolveConfigString(['identity', 'email_otp_android_min_version']) ||
      resolveConfigString(['email', 'otp_android_min_version']) ||
    null;

  const androidInstallAppPref =
      readBoolean(process.env.EMAIL_OTP_ANDROID_INSTALL_APP) ??
      resolveConfigBoolean(['app', 'email_otp_android_install_app']) ??
      resolveConfigBoolean(['identity', 'email_otp_android_install_app']) ??
      resolveConfigBoolean(['email', 'otp_android_install_app']);

  const handleCodeInAppPref =
      readBoolean(process.env.EMAIL_OTP_HANDLE_IN_APP) ??
      resolveConfigBoolean(['app', 'email_otp_handle_in_app']) ??
      resolveConfigBoolean(['identity', 'email_otp_handle_in_app']) ??
      resolveConfigBoolean(['email', 'otp_handle_in_app']);

  const handleCodeInApp =
    handleCodeInAppPref !== null
      ? handleCodeInAppPref
      : Boolean(dynamicLinkDomain || iosBundleId || androidPackageName);

  const settings = {
    continueUrl,
    handleCodeInApp,
  };

  if (handleCodeInApp) {
    settings.canHandleCodeInApp = true;
  }

  if (dynamicLinkDomain) {
    settings.dynamicLinkDomain = dynamicLinkDomain;
  }

  if (iosBundleId) {
    settings.iOSBundleId = iosBundleId;
  }

  if (androidPackageName) {
    settings.androidPackageName = androidPackageName;
    if (androidMinimumVersion) {
      settings.androidMinimumVersion = androidMinimumVersion;
    }
    if (androidInstallAppPref !== null) {
      settings.androidInstallApp = androidInstallAppPref;
    }
  }

  return settings;
}

async function callIdentityToolkit(endpoint, payload) {
  const apiKey = resolveWebApiKey();
  if (!apiKey) {
    throw new HttpsError(
      'failed-precondition',
      'Konfigurasi Firebase belum lengkap. Set nilai FIREBASE_WEB_API_KEY atau firebase.web_api_key.'
    );
  }

  const response = await fetchWithFallback(`https://identitytoolkit.googleapis.com/v1/${endpoint}?key=${apiKey}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  let json;
  try {
    json = await response.json();
  } catch (error) {
    json = null;
  }

  if (!response.ok) {
    const message = json?.error?.message || 'UNKNOWN_ERROR';
        functions.logger.error('[IDENTITY_TOOLKIT_REQUEST_FAILED]', {
          endpoint,
          status: response.status,
          identityErrorCode: message,
        });
    const error = new HttpsError('failed-precondition', 'Permintaan ke Firebase Auth gagal.');
    error.details = { identityErrorCode: message };
    throw error;
  }

  return json || {};
}
exports.sendEmailOtp = onCall(async (request) => {
  const email = sanitizeEmailInput(request.data?.email);
  if (!email) {
    throw new HttpsError('invalid-argument', 'Email wajib diisi.');
  }

  const actionCodeSettings = resolveEmailOtpActionCodeSettings();
  const payload = {
    requestType: 'EMAIL_SIGNIN',
    email,
    continueUrl: actionCodeSettings.continueUrl,
    handleCodeInApp: Boolean(actionCodeSettings.handleCodeInApp),
  };

   if (actionCodeSettings.canHandleCodeInApp) {
       payload.canHandleCodeInApp = true;
     }
     if (actionCodeSettings.dynamicLinkDomain) {
       payload.dynamicLinkDomain = actionCodeSettings.dynamicLinkDomain;
     }
     if (actionCodeSettings.iOSBundleId) {
       payload.iOSBundleId = actionCodeSettings.iOSBundleId;
     }
     if (actionCodeSettings.androidPackageName) {
       payload.androidPackageName = actionCodeSettings.androidPackageName;
     }
     if (actionCodeSettings.androidMinimumVersion) {
       payload.androidMinimumVersion = actionCodeSettings.androidMinimumVersion;
     }
     if (typeof actionCodeSettings.androidInstallApp === 'boolean') {
       payload.androidInstallApp = actionCodeSettings.androidInstallApp;
     }

     try {
       await callIdentityToolkit('accounts:sendOobCode', payload);

  } catch (error) {
    const identityErrorCode = error?.details?.identityErrorCode;
    if (identityErrorCode === 'TOO_MANY_ATTEMPTS_TRY_LATER') {
      throw new HttpsError('resource-exhausted', 'Terlalu banyak permintaan. Silakan coba lagi nanti.');
    }
    functions.logger.error('[EMAIL_OTP_SEND_FAILED]', {
          email,
          identityErrorCode,
          message: error?.message,
        });
    throw new HttpsError('failed-precondition', 'Gagal mengirim OTP email.');
  }
  functions.logger.info('[EMAIL_OTP_SENT]', { email });
  return { expiresInSeconds: EMAIL_OTP_TTL_SECONDS };

});
function mapEmailOtpVerificationError(errorCode) {
  switch (errorCode) {
    case 'INVALID_OOB_CODE':
    case 'MISSING_OOB_CODE':
      return new HttpsError('permission-denied', 'Kode OTP salah.');
    case 'EXPIRED_OOB_CODE':
      return new HttpsError('deadline-exceeded', 'Kode OTP sudah kedaluwarsa.');
    case 'TOO_MANY_ATTEMPTS_TRY_LATER':
      return new HttpsError('resource-exhausted', 'Terlalu banyak percobaan. Silakan coba lagi nanti.');
    default:
      return new HttpsError('failed-precondition', 'Verifikasi OTP email gagal.');
  }
}
exports.verifyEmailOtp = onCall(async (request) => {
  const rawCode = String(request.data?.code || '').trim();
  const email = sanitizeEmailInput(request.data?.email);

  if (!rawCode || rawCode.length < 6 || !email) {
    throw new HttpsError('invalid-argument', 'Data verifikasi OTP tidak lengkap.');
  }

  let response;
  try {
      response = await callIdentityToolkit('accounts:signInWithEmailLink', {
        email,
        oobCode: rawCode,
        returnSecureToken: false,
      });
    } catch (error) {
      const identityErrorCode = error?.details?.identityErrorCode;
      if (typeof identityErrorCode === 'string') {
        throw mapEmailOtpVerificationError(identityErrorCode);
      }
      functions.logger.error('[EMAIL_OTP_VERIFY_FAILED]', {
        email,
        identityErrorCode,
        message: error?.message,
      });
      throw new HttpsError('failed-precondition', 'Verifikasi OTP email gagal.');
  }

  if (!response || response.email !== email) {
    throw new HttpsError('failed-precondition', 'Verifikasi OTP email gagal.');
  }

  functions.logger.info('[EMAIL_OTP_VERIFIED]', { email });
  return { success: true };
});