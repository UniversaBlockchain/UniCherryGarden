CREATE ROLE ucg_user;
COMMENT ON ROLE ucg_user IS
    'The general users who can access the UniCherryGarden database. Has access only to the most public data, and to creation of tracked addresses.';


CREATE ROLE ucg_engine;
COMMENT ON ROLE ucg_engine IS
    'The engine of UniCherryGarden itself, having all the internal access it needs.';
