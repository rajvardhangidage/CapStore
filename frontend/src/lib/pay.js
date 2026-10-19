import api from '../api'
import { inr } from './format'

function loadRazorpay() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve()
    const s = document.createElement('script')
    s.src = 'https://checkout.razorpay.com/v1/checkout.js'
    s.onload = resolve
    s.onerror = () => reject(new Error('Could not load the payment window. Check your connection.'))
    document.body.appendChild(s)
  })
}

/**
 * Opens the payment window for an order that is awaiting payment.
 * Resolves with the confirmed order, rejects with Error('cancelled') if the buyer closes the window.
 */
export async function startPayment(order, user, storeName = 'CapStore', confirmDialog) {
  const { data: p } = await api.post('/payments/create-order', { orderId: order.id })

  // Backend running in dev mode (no Razorpay keys): simulate the gateway
  if (p.keyId === 'dev') {
    const ok = await confirmDialog(`Test mode: simulate a successful payment of ${inr(p.amountPaise / 100)}?`)
    if (!ok) throw new Error('cancelled')
    const { data } = await api.post('/payments/verify', {
      orderId: order.id,
      razorpay_order_id: p.gatewayOrderId,
      razorpay_payment_id: 'pay_DEV_' + Date.now(),
      razorpay_signature: 'dev',
    })
    return data
  }

  await loadRazorpay()
  return new Promise((resolve, reject) => {
    const rz = new window.Razorpay({
      key: p.keyId,
      amount: p.amountPaise,
      currency: p.currency,
      order_id: p.gatewayOrderId,
      name: storeName,
      description: `Order ${p.orderNo}`,
      prefill: { name: user?.name, email: user?.email, contact: user?.phone },
      theme: { color: '#1b2a49' },
      handler: async (resp) => {
        try {
          const { data } = await api.post('/payments/verify', { orderId: order.id, ...resp })
          resolve(data)
        } catch (e) { reject(e) }
      },
      modal: { ondismiss: () => reject(new Error('cancelled')) },
    })
    rz.on('payment.failed', async (r) => {
      try {
        await api.post(`/orders/${order.publicId}/cancel`)
        const error = new Error(r?.error?.description || 'Payment failed. The order was cancelled.')
        error.code = 'PAYMENT_FAILED_CANCELLED'
        reject(error)
      } catch (error) {
        reject(error)
      }
    })
    rz.open()
  })
}
