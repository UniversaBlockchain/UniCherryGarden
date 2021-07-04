//package com.myodov.unicherrygarden.ethereum
//
//import scala.math.BigDecimal
//
///** Various helpers and utils to deal with Ethereum data. */
////object EthUtils {
////  /** Helpers to deal with Wei/Gwei/Ether conversion. */
////  object Wei {
////    /** “Decimals” value for analyzing the raw BigInt Ethereum values.  */
////    private val DECIMALS = 18
////    val WEI_IN_ETHER: BigInt = BigInt(10).pow(DECIMALS)
////    val GWEI_IN_ETHER: BigInt = WEI_IN_ETHER / 1_000_000_000l
////
////    /**
////     * Convert the amount (defined in Wei) to regular {@link BigDecimal} value of Ethers.
////     */
////    def valueFromWeis(weis: BigInt): BigDecimal = BigDecimal(weis) / BigDecimal(WEI_IN_ETHER)
////
////    /**
////     * Convert the amount of Ethers to Wei.
////     */
////    def valueToWeis(ethers: BigDecimal): BigInt = (ethers * BigDecimal(WEI_IN_ETHER)).toBigInt
////
////    /**
////     * Convert the amount (defined in Gwei) to regular {@link BigDecimal} value of Ethers.
////     */
////    def valueFromGweis(gweis: BigDecimal): BigDecimal = gweis / BigDecimal(GWEI_IN_ETHER)
////
////    /**
////     * Convert the amount of Ethers to Gwei.
////     */
////    def valueToGweis(ethers: BigDecimal): BigDecimal = ethers * BigDecimal(GWEI_IN_ETHER)
////  }
////}
