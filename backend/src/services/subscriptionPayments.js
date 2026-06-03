/**
 * Catat pembayaran langganan — dipakai dashboard revenue bulan ini.
 */
export async function recordSubscriptionPayment(
  db,
  {
    userId,
    subscriptionId = null,
    plan,
    amount,
    paymentType = "new",
    method = "manual",
    notes = null,
    paidAt = new Date(),
  },
) {
  if (!userId || !plan || amount == null || amount <= 0) return null;

  const result = await db.query(
    `INSERT INTO subscription_payments
       (user_id, subscription_id, plan, amount, payment_type, method, notes, paid_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     RETURNING id, amount, payment_type, paid_at`,
    [
      userId,
      subscriptionId,
      String(plan).toLowerCase(),
      amount,
      paymentType,
      method,
      notes,
      paidAt instanceof Date ? paidAt.toISOString() : paidAt,
    ],
  );
  return result.rows[0];
}
