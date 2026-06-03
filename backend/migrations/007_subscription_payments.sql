-- Riwayat pembayaran langganan (sumber revenue dashboard)
CREATE TABLE IF NOT EXISTS subscription_payments (
  id              SERIAL PRIMARY KEY,
  user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  subscription_id INT REFERENCES subscriptions(id) ON DELETE SET NULL,
  plan            VARCHAR(50) NOT NULL,
  amount          INT NOT NULL DEFAULT 0,
  payment_type    VARCHAR(30) NOT NULL DEFAULT 'new',
  method          VARCHAR(50) NOT NULL DEFAULT 'manual',
  notes           TEXT,
  paid_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_paid_at
  ON subscription_payments(paid_at DESC);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_user
  ON subscription_payments(user_id, paid_at DESC);

-- Backfill dari langganan existing (sekali, jika tabel masih kosong)
INSERT INTO subscription_payments (user_id, subscription_id, plan, amount, payment_type, method, paid_at)
SELECT s.user_id,
       s.id,
       s.plan,
       COALESCE(p.price, 0),
       'new',
       'manual',
       s.started_at
FROM subscriptions s
LEFT JOIN packages p ON p.slug = LOWER(s.plan)
WHERE COALESCE(p.price, 0) > 0
  AND NOT EXISTS (SELECT 1 FROM subscription_payments LIMIT 1);
