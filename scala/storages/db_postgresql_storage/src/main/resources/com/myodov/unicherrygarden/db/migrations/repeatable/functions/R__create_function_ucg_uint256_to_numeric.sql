CREATE OR REPLACE FUNCTION ucg_uint256_to_numeric(_uint256 BYTEA)
    RETURNS NUMERIC
    LANGUAGE PLPGSQL
    IMMUTABLE STRICT
AS
$$
BEGIN
    IF length(_uint256) != 32
    THEN
        RAISE EXCEPTION 'length(%) is % instead of 32!', _uint256, length(_uint256);
    END IF;

    -- The expression below is autogenerated with the following Python expression:
    -- print(' +\n'.join(f'get_byte(_uint256, {31 - i:02}) * {0x01 << (8 * i)}::numeric' for i in range(32)))

    RETURN (
        SELECT
                get_byte(_uint256, 31) * 1::numeric +
                get_byte(_uint256, 30) * 256::numeric +
                get_byte(_uint256, 29) * 65536::numeric +
                get_byte(_uint256, 28) * 16777216::numeric +
                get_byte(_uint256, 27) * 4294967296::numeric +
                get_byte(_uint256, 26) * 1099511627776::numeric +
                get_byte(_uint256, 25) * 281474976710656::numeric +
                get_byte(_uint256, 24) * 72057594037927936::numeric +
                get_byte(_uint256, 23) * 18446744073709551616::numeric +
                get_byte(_uint256, 22) * 4722366482869645213696::numeric +
                get_byte(_uint256, 21) * 1208925819614629174706176::numeric +
                get_byte(_uint256, 20) * 309485009821345068724781056::numeric +
                get_byte(_uint256, 19) * 79228162514264337593543950336::numeric +
                get_byte(_uint256, 18) * 20282409603651670423947251286016::numeric +
                get_byte(_uint256, 17) * 5192296858534827628530496329220096::numeric +
                get_byte(_uint256, 16) * 1329227995784915872903807060280344576::numeric +
                get_byte(_uint256, 15) * 340282366920938463463374607431768211456::numeric +
                get_byte(_uint256, 14) * 87112285931760246646623899502532662132736::numeric +
                get_byte(_uint256, 13) * 22300745198530623141535718272648361505980416::numeric +
                get_byte(_uint256, 12) * 5708990770823839524233143877797980545530986496::numeric +
                get_byte(_uint256, 11) * 1461501637330902918203684832716283019655932542976::numeric +
                get_byte(_uint256, 10) * 374144419156711147060143317175368453031918731001856::numeric +
                get_byte(_uint256, 09) * 95780971304118053647396689196894323976171195136475136::numeric +
                get_byte(_uint256, 08) * 24519928653854221733733552434404946937899825954937634816::numeric +
                get_byte(_uint256, 07) * 6277101735386680763835789423207666416102355444464034512896::numeric +
                get_byte(_uint256, 06) * 1606938044258990275541962092341162602522202993782792835301376::numeric +
                get_byte(_uint256, 05) * 411376139330301510538742295639337626245683966408394965837152256::numeric +
                get_byte(_uint256, 04) * 105312291668557186697918027683670432318895095400549111254310977536::numeric +
                get_byte(_uint256, 03) * 26959946667150639794667015087019630673637144422540572481103610249216::numeric +
                get_byte(_uint256, 02) * 6901746346790563787434755862277025452451108972170386555162524223799296::numeric +
                get_byte(_uint256, 01) * 1766847064778384329583297500742918515827483896875618958121606201292619776::numeric +
                get_byte(_uint256, 00) * 452312848583266388373324160190187140051835877600158453279131187530910662656::numeric
    );
END;
$$;

COMMENT ON FUNCTION ucg_uint256_to_numeric(_uint256 BYTEA) IS
    'Convert a uint256 byte string to the integer numeric value.';
