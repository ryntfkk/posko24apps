'use strict';

const assert = require('assert');
const admin = require('firebase-admin');
const midtransClient = require('midtrans-client');
const { calculateRefundBreakdown } = require('../refund');

function deepClone(value) {
  return value === undefined ? undefined : JSON.parse(JSON.stringify(value));
}

function createFakeFirestore() {
  const stores = {
    users: new Map(),
    orders: new Map(),
    transactions: new Map(),
    chats: new Map(),
  };
  let autoId = 0;

  function getStore(name) {
    if (!stores[name]) {
      stores[name] = new Map();
    }
    return stores[name];
  }

  function makeDocRef(collectionName, docId) {
    const store = getStore(collectionName);
    const id = docId || `doc-${++autoId}`;

    return {
      id,
      __collectionName: collectionName,
      async get() {
        const data = store.get(id);
        return {
          exists: data !== undefined,
          data: () => deepClone(data),
        };
      },
      async set(value, options = {}) {
        if (options && options.merge) {
          const existing = store.get(id) || {};
          store.set(id, { ...existing, ...deepClone(value) });
        } else {
          store.set(id, deepClone(value));
        }
      },
      async update(value) {
        const existing = store.get(id) || {};
        store.set(id, { ...existing, ...deepClone(value) });
      },
    };
  }

  return {
    collection(name) {
      return {
        doc(docId) {
          return makeDocRef(name, docId);
        },
      };
    },
    async runTransaction(callback) {
      const tx = {
        async get(docRef) {
          return docRef.get();
        },
        update(docRef, value) {
          docRef.update(value);
          return tx;
        },
        set(docRef, value) {
          docRef.set(value);
          return tx;
        },
      };
      return callback(tx);
    },
    __seedUser(id, data) {
      getStore('users').set(id, deepClone(data));
    },
    __seedOrder(id, data) {
      getStore('orders').set(id, deepClone(data));
    },
    __updateOrder(id, data) {
      const store = getStore('orders');
      const existing = store.get(id) || {};
      store.set(id, { ...existing, ...deepClone(data) });
    },
    __getUser(id) {
      const data = getStore('users').get(id);
      return data ? deepClone(data) : undefined;
    },
    __getOrder(id) {
      const data = getStore('orders').get(id);
      return data ? deepClone(data) : undefined;
    },
    __getTransactions() {
      return Array.from(getStore('transactions').values()).map((item) => deepClone(item));
    },
  };
}

function createMockResponse() {
  return {
    statusCode: null,
    body: null,
    status(code) {
      this.statusCode = code;
      return this;
    },
    send(payload) {
      this.body = payload;
      return this;
    },
  };
}

function makeSnapshot(data) {
  return {
    data: () => deepClone(data),
  };
}

async function invokeHttpsFunction(handler, req, res) {
  if (typeof handler === 'function') {
    return handler(req, res);
  }
  if (handler && typeof handler.run === 'function') {
    return handler.run(req, res);
  }
  throw new Error('Unsupported HTTP function type');
}
function testSingleItemTotalAmount() {
  const order = {
    totalAmount: 12500,
    adminFee: 2500,
    discountAmount: 0,
    serviceSnapshot: {
      items: [{ serviceName: 'Cleaning', lineTotal: 10000 }],
    },
  };

  const breakdown = calculateRefundBreakdown(order);
  assert.strictEqual(breakdown.source, 'totalAmount');
  assert.strictEqual(breakdown.adjustments.subtractAdminFee, 2500);
  assert.strictEqual(breakdown.amount, 10000);
}

function testMultiItemWithoutTotal() {
  const order = {
    adminFee: 2500,
    discountAmount: 2000,
    serviceSnapshot: {
      items: [
        { serviceName: 'Cleaning', lineTotal: 5000 },
        { serviceName: 'Laundry', lineTotal: 7000 },
      ],
    },
  };

  const breakdown = calculateRefundBreakdown(order);
  assert.strictEqual(breakdown.source, 'items');
  assert.strictEqual(breakdown.adjustments.subtractDiscount, 2000);
  assert.strictEqual(breakdown.adjustments.subtractAdminFee, 0);
  assert.strictEqual(breakdown.amount, 10000);
}

function testMultiItemWithAdminRefund() {
  const order = {
    adminFee: 2500,
    discountAmount: 2000,
    serviceSnapshot: {
      items: [
        { serviceName: 'Cleaning', lineTotal: 5000 },
        { serviceName: 'Laundry', lineTotal: 7000 },
      ],
      refundPolicy: {
        includeAdminFee: true,
      },
    },
  };

  const breakdown = calculateRefundBreakdown(order);
  assert.strictEqual(breakdown.source, 'items');
  assert.strictEqual(breakdown.adjustments.addAdminFee, 2500);
  assert.strictEqual(breakdown.amount, 12500);
}

function testTotalAmountWithAdminRefund() {
  const order = {
    totalAmount: 12500,
    adminFee: 2500,
    discountAmount: 2000,
    serviceSnapshot: {
      items: [
        { serviceName: 'Cleaning', lineTotal: 10000 },
      ],
      refundPolicy: {
        includeAdminFee: true,
      },
    },
  };

  const breakdown = calculateRefundBreakdown(order);
  assert.strictEqual(breakdown.source, 'totalAmount');
  assert.strictEqual(breakdown.adjustments.subtractAdminFee, 0);
  assert.strictEqual(breakdown.amount, 12500);
}

function testDiscountNotNegative() {
  const order = {
    adminFee: 0,
    discountAmount: 5000,
    serviceSnapshot: {
      items: [{ serviceName: 'Cleaning', lineTotal: 3000 }],
    },
  };

  const breakdown = calculateRefundBreakdown(order);
  assert.strictEqual(breakdown.amount, 0);
}

async function testWebhookThenCancellationTriggersRefund() {
  const originalInitializeApp = admin.initializeApp;
  const originalFirestore = admin.firestore;
  const originalFieldValue = originalFirestore && originalFirestore.FieldValue;
  const originalFirestoreDescriptor = Object.getOwnPropertyDescriptor(admin, 'firestore');
  const originalCoreApi = midtransClient.CoreApi;
  const indexPath = require.resolve('../index.js');

  let fakeDb;
  let myFunctions;
  let nextNotificationResponse = null;

  try {
    admin.initializeApp = () => {};
    fakeDb = createFakeFirestore();
    Object.defineProperty(admin, 'firestore', {
      configurable: true,
      enumerable: true,
      writable: true,
      value: () => fakeDb,
    });
    admin.firestore.FieldValue = {
      serverTimestamp: () => ({ mockTimestamp: true }),
    };

    midtransClient.CoreApi = function () {
      return {
        transaction: {
          notification: async () => {
            if (!nextNotificationResponse) {
              throw new Error('Stub notification response not configured');
            }
            return nextNotificationResponse;
          },
        },
      };
    };

    delete require.cache[indexPath];
    myFunctions = require('../index');

    const orderId = 'order-cancel-test';
    const customerId = 'customer-123';
    const initialBalance = 5000;

    const baseOrderData = {
      status: 'awaiting_payment',
      paymentStatus: 'pending',
      orderType: 'basic',
      customerId,
      adminFee: 1000,
      totalAmount: 15000,
      discountAmount: 0,
      serviceSnapshot: {
        items: [{ serviceName: 'Cleaning', lineTotal: 15000 }],
      },
    };

    fakeDb.__seedUser(customerId, { balance: initialBalance });
    fakeDb.__seedOrder(orderId, baseOrderData);

    nextNotificationResponse = {
      order_id: orderId,
      transaction_status: 'settlement',
      fraud_status: 'accept',
    };

    const req = { body: { order_id: orderId } };
    const res = createMockResponse();

    await invokeHttpsFunction(myFunctions.midtransWebhookHandler, req, res);

    assert.strictEqual(res.statusCode, 200, 'Webhook should respond with 200 OK');

    const orderAfterWebhook = fakeDb.__getOrder(orderId);
    assert.strictEqual(orderAfterWebhook.paymentStatus, 'paid');
    assert.strictEqual(orderAfterWebhook.status, 'searching_provider');

    const beforeData = orderAfterWebhook;
    fakeDb.__updateOrder(orderId, { status: 'cancelled' });
    const afterData = fakeDb.__getOrder(orderId);

    const event = {
      params: { orderId },
      data: {
        before: makeSnapshot(beforeData),
        after: makeSnapshot(afterData),
      },
    };

    await myFunctions.__testHandleOrderCancelled(event);

    const updatedUser = fakeDb.__getUser(customerId);
    const balanceIncrease = updatedUser.balance - initialBalance;
    const breakdown = calculateRefundBreakdown(afterData);

    assert.strictEqual(balanceIncrease, breakdown.amount);

    const transactions = fakeDb.__getTransactions();
    assert.strictEqual(transactions.length, 1);
    assert.strictEqual(transactions[0].orderId, orderId);
    assert.strictEqual(transactions[0].userId, customerId);
    assert.strictEqual(transactions[0].type, 'REFUND_IN');
    assert.strictEqual(transactions[0].amount, breakdown.amount);
  } finally {
    delete require.cache[indexPath];
    admin.initializeApp = originalInitializeApp;
    if (originalFirestoreDescriptor) {
      Object.defineProperty(admin, 'firestore', originalFirestoreDescriptor);
    } else {
      Object.defineProperty(admin, 'firestore', {
        configurable: true,
        enumerable: true,
        writable: true,
        value: originalFirestore,
      });
    }
    if (originalFieldValue !== undefined) {
      admin.firestore.FieldValue = originalFieldValue;
    } else {
      delete admin.firestore.FieldValue;
    }
    midtransClient.CoreApi = originalCoreApi;
  }
}

async function run() {
  testSingleItemTotalAmount();
  testMultiItemWithoutTotal();
  testMultiItemWithAdminRefund();
  testTotalAmountWithAdminRefund();
  testDiscountNotNegative();
  await testWebhookThenCancellationTriggersRefund();
  console.log('All refund tests passed.');
  }

run().catch((error) => {
  console.error(error);
  process.exit(1);
});