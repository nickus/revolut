CREATE OR REPLACE FUNCTION check_debit_eq_credit() RETURNS TRIGGER AS
$$
DECLARE
    sum DECIMAL(21, 2);
BEGIN
    --
    -- Checks whether debit is equal credit
    --
    sum := (SELECT COALESCE(SUM(CASE WHEN type = 'debit' THEN -amount ELSE amount END), 0) FROM posting);
    IF sum <> 0 THEN RAISE EXCEPTION 'debit is not equal credit' USING ERRCODE = '11122'; END IF;
    RETURN new;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER debit_eq_credit
    AFTER INSERT
    ON posting DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE PROCEDURE check_debit_eq_credit();