'use strict';

function toNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function getItems(order) {
  if (!order || typeof order !== 'object') return [];
  const snapshot = order.serviceSnapshot;
  if (!snapshot || typeof snapshot !== 'object') return [];
  const items = snapshot.items;
  return Array.isArray(items) ? items : [];
}

function resolveRefundPolicy(order) {
  const fallback = { includeAdminFee: false, includeDiscount: false };
  if (!order || typeof order !== 'object') {
    return fallback;
  }

  const candidates = [];
  if (order.refundPolicy && typeof order.refundPolicy === 'object') {
    candidates.push(order.refundPolicy);
  }
  const snapshot = order.serviceSnapshot;
  if (snapshot && typeof snapshot === 'object') {
    if (snapshot.refundPolicy && typeof snapshot.refundPolicy === 'object') {
      candidates.push(snapshot.refundPolicy);
    }
  }

  if (candidates.length === 0) return fallback;

  // The latest policy wins
  const policy = candidates.reduce((acc, candidate) => Object.assign(acc, candidate), {});

  const includeAdminFee =
    policy.includeAdminFee === true || policy.refundAdminFee === true || policy.adminFee === 'refundable';
  const includeDiscount =
    policy.includeDiscount === true || policy.refundDiscount === true || policy.discount === 'refundable';

  return {
    includeAdminFee,
    includeDiscount,
  };
}

function calculateRefundBreakdown(order) {
  const items = getItems(order);
  const totalAmount = toNumber(order?.totalAmount);
  const adminFee = toNumber(order?.adminFee);
  const discountAmount = toNumber(order?.discountAmount);
  const quantity = toNumber(order?.quantity) || 1;
  const basePrice = toNumber(order?.serviceSnapshot?.basePrice);

  let baseAmount = 0;
  let source = 'unknown';

  if (totalAmount > 0) {
    baseAmount = totalAmount;
    source = 'totalAmount';
  } else if (items.length > 0) {
    baseAmount = items.reduce((sum, item) => sum + toNumber(item?.lineTotal), 0);
    source = 'items';
  } else if (basePrice > 0) {
    baseAmount = basePrice * (quantity > 0 ? quantity : 1);
    source = 'basePrice';
  }

  if (!(baseAmount > 0)) {
    return {
      amount: 0,
      source,
      adjustments: {
        subtractAdminFee: 0,
        addAdminFee: 0,
        subtractDiscount: 0,
      },
      policy: resolveRefundPolicy(order),
      totals: {
        totalAmount,
        items: baseAmount,
      },
    };
  }

  const policy = resolveRefundPolicy(order);
  const adjustments = {
    subtractAdminFee: 0,
    addAdminFee: 0,
    subtractDiscount: 0,
  };

  if (source === 'totalAmount' && !policy.includeAdminFee && adminFee > 0) {
    const deduction = Math.min(adminFee, baseAmount);
    baseAmount -= deduction;
    adjustments.subtractAdminFee = deduction;
  }

  if (source !== 'totalAmount' && policy.includeAdminFee && adminFee > 0) {
    baseAmount += adminFee;
    adjustments.addAdminFee = adminFee;
  }

  if (source !== 'totalAmount' && !policy.includeDiscount && discountAmount > 0) {
    const deduction = Math.min(discountAmount, baseAmount);
    baseAmount -= deduction;
    adjustments.subtractDiscount = deduction;
  }

  const amount = Math.max(0, baseAmount);

  return {
    amount,
    source,
    adjustments,
    policy,
    totals: {
      totalAmount,
      items: items.reduce((sum, item) => sum + toNumber(item?.lineTotal), 0),
    },
  };
}

function calculateRefundAmount(order) {
  return calculateRefundBreakdown(order).amount;
}

module.exports = {
  calculateRefundAmount,
  calculateRefundBreakdown,
  resolveRefundPolicy,
  toNumber,
};