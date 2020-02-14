CREATE SEQUENCE account_id START WITH 1;
CREATE SEQUENCE transaction_id START WITH 1;
CREATE SEQUENCE posting_id START WITH 1;
CREATE TYPE transaction_type as ENUM ('deposit', 'withdrawal', 'transfer');
CREATE TYPE posting_type as ENUM ('debit', 'credit');

CREATE TABLE account
(
    id   BIGINT PRIMARY KEY DEFAULT nextval('account_id'),
    name VARCHAR(255)
);

CREATE TABLE transaction
(
    id              BIGINT PRIMARY KEY DEFAULT nextval('transaction_id'),
    type            transaction_type NOT NULL,
    amount          DECIMAL(12, 2)   NOT NULL CHECK ( amount > 0 ),
    from_account_id BIGINT           NOT NULL REFERENCES account (id),
    to_account_id   BIGINT           NOT NULL REFERENCES account (id),
    idempotency_key VARCHAR(255)     NOT NULL UNIQUE,

    CHECK ( from_account_id <> to_account_id )
);

CREATE TABLE posting
(
    id             BIGINT PRIMARY KEY DEFAULT nextval('posting_id'),
    transaction_id BIGINT         NOT NULL REFERENCES transaction (id),
    account_id     BIGINT         NOT NULL REFERENCES account (id),
    type           posting_type   NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL CHECK ( amount > 0 )
);

CREATE INDEX posting_account_idx ON posting (account_id);

INSERT INTO account(id, name)
VALUES (0, 'cash book');
