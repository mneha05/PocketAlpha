#!/usr/bin/env ruby
# frozen_string_literal: true

require "csv"
require "fileutils"
require_relative "../lib/ledger_bridge"

out = ARGV[0] || "payments/out"
FileUtils.mkdir_p(out)

service = LedgerBridge::PaymentService.new

payments = [
  [10_000, 0],
  [7_500, 2_500],
  [4_200, 0]
].each_with_index.map do |(amount, refund), index|
  payment = service.create_payment(
    amount_cents: amount,
    idempotency_key: "fixture-create-#{index}"
  )
  service.capture(
    payment_id: payment.id,
    idempotency_key: "fixture-capture-#{index}"
  )
  if refund.positive?
    service.refund(
      payment_id: payment.id,
      amount_cents: refund,
      idempotency_key: "fixture-refund-#{index}"
    )
  end
  payment
end

CSV.open(File.join(out, "internal_payments.csv"), "w") do |csv|
  csv << %w[payment_id captured_cents refunded_cents]
  payments.each do |payment|
    csv << [payment.id, payment.captured_cents, payment.refunded_cents]
  end
end

CSV.open(File.join(out, "settlements.csv"), "w") do |csv|
  csv << %w[payment_id gross_cents fee_cents net_cents status]
  payments.each do |payment|
    gross = payment.captured_cents - payment.refunded_cents
    fee = (gross * 0.029).round + 30
    csv << [payment.id, gross, fee, gross - fee, "settled"]
  end
end

abort "ledger invariant failed" unless service.ledger.balanced?

puts "wrote #{payments.length} reconciliable payments to #{out}"
