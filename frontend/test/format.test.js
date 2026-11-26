import test from 'node:test'
import assert from 'node:assert/strict'

import { estimate, statusLabel } from '../src/lib/format.js'

test('estimate applies GST and shipping to a discounted subtotal', () => {
  assert.deepEqual(estimate(1000, 100, {
    gstRatePercent: 5,
    freeShippingAbove: 999,
    shippingFlatRate: 70,
  }), { tax: 45, shipping: 70, total: 1015 })
})

test('estimate does not produce a negative taxable total', () => {
  assert.deepEqual(estimate(100, 200, {}), { tax: 0, shipping: 70, total: 70 })
})

test('statusLabel formats underscore-separated statuses for display', () => {
  assert.equal(statusLabel('OUT_FOR_DELIVERY'), 'Out for delivery')
})
