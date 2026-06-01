ALTER TABLE stone_shape
    ADD COLUMN IF NOT EXISTS stone_shape_default BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE stone_type
    ADD COLUMN IF NOT EXISTS stone_type_default BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE stone_shape
   SET stone_shape_default = TRUE
 WHERE stone_shape_id = (SELECT MIN(stone_shape_id) FROM stone_shape)
   AND NOT EXISTS (SELECT 1 FROM stone_shape WHERE stone_shape_default = TRUE);

UPDATE stone_type
   SET stone_type_default = TRUE
 WHERE stone_type_id = (SELECT MIN(stone_type_id) FROM stone_type)
   AND NOT EXISTS (SELECT 1 FROM stone_type WHERE stone_type_default = TRUE);
