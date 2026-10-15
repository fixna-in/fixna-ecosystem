-- V8: Fix currency column type from CHAR(3) to VARCHAR(3) in invoice_records to match entity mapping.
ALTER TABLE invoice_records ALTER COLUMN currency TYPE VARCHAR(3);