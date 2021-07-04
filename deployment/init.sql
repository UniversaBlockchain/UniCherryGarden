CREATE ROLE ucp_user;
COMMENT ON ROLE ucp_user IS
    'The general users who can access the UniCherrypicker database. Has access only to the most public data, and to creation of tracked addresses.';


CREATE ROLE ucp_engine;
COMMENT ON ROLE ucp_engine IS
    'The engine of UniCherrypicker itself, having all the internal access it needs.';


---
--- Permissions
---
GRANT unicashier_public_user TO unicashier_payment_receiver;
GRANT unicashier_public_user TO unicashier_payment_sender;
GRANT unicashier_public_user TO unicashier_token_administrator;

-- ucp_engine
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES, TRIGGER ON ALL TABLES IN SCHEMA "public"
    TO ucp_engine;
GRANT ALL ON ALL SEQUENCES IN SCHEMA "public"
    TO ucp_engine;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA "public"
    TO ucp_engine;





--
-- What roles are present for UniCashier?
--
CREATE ROLE unicashier_public_user;
COMMENT ON ROLE unicashier_public_user IS
    'The general users who can access the UniCashier database. Has access only to the most public data.';

-- GRANT unicashier_public_user TO <someuser>;
CREATE ROLE unicashier_payment_receiver;
COMMENT ON ROLE unicashier_payment_receiver IS
    'The users who are allowed to create invoices to receive payments.';
-- GRANT unicashier_payment_receiver TO <someuser>;
CREATE ROLE unicashier_payment_sender;
COMMENT ON ROLE unicashier_payment_sender IS
    'The users who are allowed to send outgoing payments.';
-- GRANT unicashier_payment_sender TO <someuser>;
CREATE ROLE unicashier_token_administrator;
COMMENT ON ROLE unicashier_token_administrator IS
    'The users who are allowed to add new tokens.';
-- GRANT unicashier_token_administrator TO <someuser>;
CREATE ROLE unicashier_payment_requester;
COMMENT ON ROLE unicashier_payment_requester IS
    'The users who are allowed to process (send and receive) payments.';
GRANT unicashier_payment_receiver TO unicashier_payment_requester;
GRANT unicashier_payment_sender TO unicashier_payment_requester;
CREATE ROLE unicashier_engine;
COMMENT ON ROLE unicashier_engine IS
    'The engine of UniCashier itself, having all the internal access it needs.';
-- GRANT unicashier_public_user TO <someuser>;
---
--- Permissions
---
GRANT unicashier_public_user TO unicashier_payment_receiver;
GRANT unicashier_public_user TO unicashier_payment_sender;
GRANT unicashier_public_user TO unicashier_token_administrator;
-- unicashier_engine
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES, TRIGGER ON ALL TABLES IN SCHEMA "public"
    TO unicashier_engine;
GRANT ALL ON ALL SEQUENCES IN SCHEMA "public"
    TO unicashier_engine;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA "public"
    TO unicashier_engine;