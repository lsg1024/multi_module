-- ----------------------------------------------------------------------------
-- (1) address 의 deleted 정리
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'address' AND column_name = 'deleted'
    ) THEN
        DELETE FROM address WHERE deleted = TRUE;
    END IF;
END $$;
ALTER TABLE address DROP COLUMN IF EXISTS deleted;


-- ----------------------------------------------------------------------------
-- (2) additional_option 의 deleted 정리
-- ----------------------------------------------------------------------------
-- AdditionalOption 은 store/factory 가 1:1 로 보유하는 dependent entity
-- 이므로 자체 soft delete 가 불필요. 부모 삭제 시 CASCADE 로 자동 정리.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'additional_option' AND column_name = 'deleted'
    ) THEN
        DELETE FROM additional_option WHERE deleted = TRUE;
    END IF;
END $$;
ALTER TABLE additional_option DROP COLUMN IF EXISTS deleted;
