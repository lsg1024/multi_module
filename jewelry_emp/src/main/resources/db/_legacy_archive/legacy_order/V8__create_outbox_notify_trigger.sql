CREATE OR REPLACE FUNCTION notify_outbox_event() RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('outbox_channel', current_schema() || ':' || NEW.ID::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER outbox_event_notify
    AFTER INSERT ON OUT_BOX_EVENT
    FOR EACH ROW
    EXECUTE FUNCTION notify_outbox_event();
