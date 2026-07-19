-- V21__create_indexes.sql

CREATE INDEX idx_attendance_employee_date ON attendance_records (employee_id, attendance_date);

CREATE INDEX idx_sales_customer ON sales_invoices (customer_id);
CREATE INDEX idx_sales_date ON sales_invoices (invoice_date);

CREATE INDEX idx_purchases_supplier ON purchases (supplier_id);
CREATE INDEX idx_purchases_date ON purchases (purchase_date);

CREATE INDEX idx_ot_employee_status ON overtime_requests (employee_id, status);
