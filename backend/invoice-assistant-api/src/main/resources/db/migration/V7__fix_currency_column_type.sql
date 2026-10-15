-- V7: Fix currency column type from CHAR(3) to VARCHAR(3) to match entity mapping.
ALTER TABLE invoice_document_fields ALTER COLUMN currency TYPE VARCHAR(3);