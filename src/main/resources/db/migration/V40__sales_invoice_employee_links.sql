-- V40__sales_invoice_employee_links.sql
-- Minimum additive change requested for the Performance Engine's Sales Credit Rule:
-- sold_by_employee_id     = who actually took/entered this order (operational history only)
-- credited_to_employee_id = the customer's owner as resolved from Customer Ownership at the
--                           moment this invoice was created (frozen here, same reasoning as
--                           Payroll's settings snapshot — later ownership changes must never
--                           rewrite which employee historical sales credit belongs to).
-- Both nullable: existing invoices and non-sales-flow invoices are unaffected.

ALTER TABLE sales_invoices
    ADD COLUMN sold_by_employee_id BIGINT NULL,
    ADD COLUMN credited_to_employee_id BIGINT NULL,
    ADD CONSTRAINT fk_sales_invoice_sold_by FOREIGN KEY (sold_by_employee_id) REFERENCES employees (id),
    ADD CONSTRAINT fk_sales_invoice_credited_to FOREIGN KEY (credited_to_employee_id) REFERENCES employees (id),
    ADD KEY idx_sales_invoice_credited_to (credited_to_employee_id);