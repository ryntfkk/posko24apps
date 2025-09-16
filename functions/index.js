'use strict';

const functions = require('firebase-functions/v2');
const { onDocumentUpdated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
const midtransClient = require('midtrans-client');
const { ADMIN_FEE } = require('./config');


admin.initializeApp();
const db = admin.firestore();

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
        const resp = await fetch(`${base}/snap/v1/transactions`, {
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

  const { orderId } = request.data || {};
  if (!orderId) {
    throw new functions.https.HttpsError('invalid-argument', 'OrderId wajib diisi.');
  }

  const uid = request.auth.uid;
  const orderRef = db.collection('orders').doc(orderId);
  const snap = await orderRef.get();
  if (!snap.exists) {
    throw new functions.https.HttpsError('not-found', 'Pesanan tidak ditemukan.');
  }

  const data = snap.data();
  if (data.providerId) {
    throw new functions.https.HttpsError('failed-precondition', 'Pesanan sudah memiliki provider.');
  }
  if (data.status !== 'searching_provider') {
    throw new functions.https.HttpsError('failed-precondition', 'Pesanan tidak dalam status pencarian provider.');
  }

  await orderRef.update({ providerId: uid, status: 'pending' });
  functions.logger.info('[CLAIM_ORDER]', { orderId, providerId: uid });
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
 * 7) REFUND ON CANCELLATION (Firestore Trigger v2)
 * ============================================================
 */
exports.onOrderCancelled = onDocumentUpdated('orders/{orderId}', async (event) => {
  const after = event.data.after.data();
  const before = event.data.before.data();

   if (
      after.status === 'cancelled' &&
      before.paymentStatus !== 'paid' &&
      after.paymentStatus === 'paid'
    ) {
        const { customerId, serviceSnapshot, quantity } = after;
        const basePrice = serviceSnapshot?.basePrice;
        const amount = basePrice * (quantity || 1);
    if (!customerId || !amount || amount <= 0) return null;

    const customerRef = db.collection('users').doc(customerId);
    await db.runTransaction(async (tx) => {
      const customerDoc = await tx.get(customerRef);
      if (!customerDoc.exists) throw new Error('User tidak ditemukan.');
      const newBalance = (customerDoc.data().balance || 0) + amount;
      tx.update(customerRef, { balance: newBalance });
      const refundRef = db.collection('transactions').doc();
      tx.set(refundRef, {
        userId: customerId,
        orderId: event.params.orderId,
        type: 'REFUND_IN',
        amount,
        description: 'Pengembalian dana dari pesanan yang dibatalkan',
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });
  }
  return null;
});
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
