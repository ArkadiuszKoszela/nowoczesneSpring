-- Dodanie kolumn dla marży i rabatu usług
ALTER TABLE projects 
ADD COLUMN services_margin DOUBLE DEFAULT 0.0,
ADD COLUMN services_discount DOUBLE DEFAULT 0.0;

