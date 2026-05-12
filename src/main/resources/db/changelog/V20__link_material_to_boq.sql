-- Liquibase migration: V20__link_material_to_boq.sql
ALTER TABLE material_site_transactions ADD COLUMN boq_item_id BIGINT;
ALTER TABLE material_site_transactions 
    ADD CONSTRAINT fk_material_site_tx_boq_item 
    FOREIGN KEY (boq_item_id) REFERENCES boq_items(id);
