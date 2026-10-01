#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require_relative "../lib/ledger_bridge"

service = LedgerBridge::PaymentService.new

payment = service.create_payment(
  amount_cents: 25_00,
  currency: "usd",
  idempotency_key: "demo-create-001"
)
service.capture(payment_id: payment.id, idempotency_key: "demo-capture-001")
service.refund(payment_id: payment.id, amount_cents: 5_00, idempotency_key: "demo-refund-001")

puts JSON.pretty_generate(
  payment: {
    id: payment.id,
    status: payment.status,
    amount_cents: payment.amount_cents,
    captured_cents: payment.captured_cents,
    refunded_cents: payment.refunded_cents
  },
  ledger_balanced: service.ledger.balanced?,
  ledger_entries: service.ledger.entries.map(&:to_h)
)
