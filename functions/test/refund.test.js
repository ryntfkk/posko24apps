'use strict';

const assert = require('assert');
const { calculateRefundBreakdown } = require('../refund');

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

function run() {
  testSingleItemTotalAmount();
  testMultiItemWithoutTotal();
  testMultiItemWithAdminRefund();
  testTotalAmountWithAdminRefund();
  testDiscountNotNegative();
  console.log('All refund calculation tests passed.');
}

run();