'use strict';

const assert = require('assert');
const admin = require('firebase-admin');
const firebaseFunctionsTest = require('firebase-functions-test');

function deepClone(value) {
  return value === undefined ? undefined : JSON.parse(JSON.stringify(value));
}

function createFakeFirestore() {
  const stores = {
    orders: new Map(),
  };
  let autoId = 0;

  function getStore(name) {
    if (!stores[name]) {
      stores[name] = new Map();
    }
    return stores[name];
  }

  function makeDocSnapshot(collectionName, id, data) {
    return {
      id,
      ref: makeDocRef(collectionName, id),
      exists: data !== undefined,
      data: () => deepClone(data),
    };
  }

  function matchesFilters(data, filters) {
    return filters.every(({ field, op, value }) => {
      const actual = data ? data[field] : undefined;
      switch (op) {
        case '==':
          if (value === null) {
            return actual === null || actual === undefined;
          }
          return actual === value;
        case 'in':
          if (!Array.isArray(value)) {
            throw new Error('`in` operator expects an array value');
          }
          return value.some((candidate) => {
            if (candidate === null) {
              return actual === null || actual === undefined;
            }
            return actual === candidate;
          });
        default:
          throw new Error(`Unsupported operator: ${op}`);
      }
    });
  }

  function buildQuery(collectionName, filters) {
    return {
      __isQuery: true,
      __collectionName: collectionName,
      __filters: filters,
      where(field, op, value) {
        return buildQuery(collectionName, filters.concat([{ field, op, value }]));
      },
      async get() {
        const store = getStore(collectionName);
        const docs = [];
        for (const [id, value] of store.entries()) {
          if (matchesFilters(value, filters)) {
            docs.push(makeDocSnapshot(collectionName, id, value));
          }
        }
        return {
          empty: docs.length === 0,
          size: docs.length,
          docs,
        };
      },
    };
  }

  function makeDocRef(collectionName, docId) {
    const store = getStore(collectionName);
    const id = docId || `doc-${++autoId}`;

    return {
      id,
      __collectionName: collectionName,
      async get() {
        const data = store.get(id);
        return makeDocSnapshot(collectionName, id, data);
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
        where(field, op, value) {
          return buildQuery(name, [{ field, op, value }]);
        },
      };
    },
    async runTransaction(callback) {
      const tx = {
        async get(target) {
          if (target && target.__isQuery) {
            return target.get();
          }
          return target.get();
        },
        update(docRef, value) {
          docRef.update(value);
          return tx;
        },
        set(docRef, value, options = {}) {
          docRef.set(value, options);
          return tx;
        },
      };

      return callback(tx);
    },
    __seedOrder(id, data) {
      getStore('orders').set(id, deepClone(data));
    },
    __getOrder(id) {
      const data = getStore('orders').get(id);
      return data ? deepClone(data) : undefined;
    },
    __reset() {
      Object.values(stores).forEach((store) => store.clear());
    },
  };
}

async function invokeCallable(handler, data, providerId) {
  if (!handler || typeof handler.run !== 'function') {
    throw new Error('Unsupported callable handler type');
  }
  const request = {
    data,
    auth: providerId ? { uid: providerId } : undefined,
  };
  return handler.run(request);
}

async function claimOrderSucceedsWithoutConflict(handler, fakeDb) {
  fakeDb.__reset();
  const providerId = 'provider-1';
  const orderId = 'order-no-conflict';

  fakeDb.__seedOrder(orderId, {
    status: 'searching_provider',
    providerId: null,
    scheduledDate: '2024-04-01',
  });

  const result = await invokeCallable(handler, { orderId }, providerId);
  assert.ok(result && result.success, 'Callable should return success');

  const updatedOrder = fakeDb.__getOrder(orderId);
  assert.strictEqual(updatedOrder.providerId, providerId);
  assert.strictEqual(updatedOrder.status, 'pending');
}

async function claimOrderFailsWhenSameDateConflict(handler, fakeDb) {
  fakeDb.__reset();
  const providerId = 'provider-1';
  const conflictingDate = '2024-05-01';

  fakeDb.__seedOrder('existing-order', {
    status: 'pending',
    providerId,
    scheduledDate: conflictingDate,
  });

  fakeDb.__seedOrder('order-to-claim', {
    status: 'searching_provider',
    providerId: null,
    scheduledDate: conflictingDate,
  });

  try {
    await invokeCallable(handler, { orderId: 'order-to-claim' }, providerId);
    assert.fail('Expected claimOrder to throw for conflicting schedule.');
  } catch (error) {
    assert.strictEqual(error.code, 'failed-precondition');
    assert.match(error.message, /pesanan aktif pada tanggal tersebut/i);
  }

  const untouched = fakeDb.__getOrder('order-to-claim');
  assert.strictEqual(untouched.providerId, null);
  assert.strictEqual(untouched.status, 'searching_provider');
}

async function claimOrderFailsWhenExistingOrderHasNoSchedule(handler, fakeDb) {
  fakeDb.__reset();
  const providerId = 'provider-1';

  fakeDb.__seedOrder('active-without-schedule', {
    status: 'ongoing',
    providerId,
    scheduledDate: null,
  });

  fakeDb.__seedOrder('order-future', {
    status: 'searching_provider',
    providerId: null,
    scheduledDate: '2024-06-10',
  });

  try {
    await invokeCallable(handler, { orderId: 'order-future' }, providerId);
    assert.fail('Expected claimOrder to throw when provider has unscheduled active order.');
  } catch (error) {
    assert.strictEqual(error.code, 'failed-precondition');
    assert.match(error.message, /pesanan aktif/i);
  }

  const untouched = fakeDb.__getOrder('order-future');
  assert.strictEqual(untouched.providerId, null);
  assert.strictEqual(untouched.status, 'searching_provider');
}

async function claimOrderSucceedsWithDifferentDate(handler, fakeDb) {
  fakeDb.__reset();
  const providerId = 'provider-1';

  fakeDb.__seedOrder('existing-order', {
    status: 'accepted',
    providerId,
    scheduledDate: '2024-05-01',
  });

  fakeDb.__seedOrder('order-new-date', {
    status: 'searching_provider',
    providerId: null,
    scheduledDate: '2024-05-03',
  });

  const result = await invokeCallable(handler, { orderId: 'order-new-date' }, providerId);
  assert.ok(result && result.success, 'Callable should succeed when schedules do not overlap.');

  const updated = fakeDb.__getOrder('order-new-date');
  assert.strictEqual(updated.providerId, providerId);
  assert.strictEqual(updated.status, 'pending');
}

async function run() {
  process.env.FUNCTIONS_EMULATOR = 'true';
  const testEnv = firebaseFunctionsTest({ projectId: 'posko24-test' });
  const indexPath = require.resolve('../index.js');

  const originalInitializeApp = admin.initializeApp;
  const originalFirestoreDescriptor = Object.getOwnPropertyDescriptor(admin, 'firestore');

  let fakeDb;
  let myFunctions;

  try {
    admin.initializeApp = () => {};
    fakeDb = createFakeFirestore();
    Object.defineProperty(admin, 'firestore', {
      configurable: true,
      enumerable: true,
      writable: true,
      value: () => fakeDb,
    });

    delete require.cache[indexPath];
    myFunctions = require('../index.js');

    const claimOrderHandler = myFunctions.claimOrder;

    await claimOrderSucceedsWithoutConflict(claimOrderHandler, fakeDb);
    await claimOrderFailsWhenSameDateConflict(claimOrderHandler, fakeDb);
    await claimOrderFailsWhenExistingOrderHasNoSchedule(claimOrderHandler, fakeDb);
    await claimOrderSucceedsWithDifferentDate(claimOrderHandler, fakeDb);

    console.log('All claimOrder tests passed.');
  } finally {
    delete require.cache[indexPath];
    admin.initializeApp = originalInitializeApp;
    if (originalFirestoreDescriptor) {
      Object.defineProperty(admin, 'firestore', originalFirestoreDescriptor);
    }
    await testEnv.cleanup();
  }
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});