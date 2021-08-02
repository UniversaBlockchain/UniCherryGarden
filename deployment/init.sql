CREATE ROLE ucp_user;
COMMENT ON ROLE ucp_user IS
    'The general users who can access the UniCherryGarden database. Has access only to the most public data, and to creation of tracked addresses.';


CREATE ROLE ucp_engine;
COMMENT ON ROLE ucp_engine IS
    'The engine of UniCherryGarden itself, having all the internal access it needs.';
