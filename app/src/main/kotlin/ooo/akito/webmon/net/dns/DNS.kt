package ooo.akito.webmon.net.dns

import ooo.akito.webmon.net.Utils.normalise
import ooo.akito.webmon.utils.ArrayIP
import org.minidns.hla.ResolverApi
import org.minidns.hla.ResolverResult
import org.minidns.record.A
import org.minidns.record.AAAA

class DNS {
  
  fun retrieveAllIPsFromDnsRecords(url: String): List<ArrayIP> {
    /** A Records */
    val resultA: ResolverResult<A> = ResolverApi.INSTANCE.resolve(url, A::class.java)
    /** AAAA Records */
    val resultAAAA: ResolverResult<AAAA> = ResolverApi.INSTANCE.resolve(url, AAAA::class.java)
    /** All Records' Answers */
    val answersAll = resultA.answers + resultAAAA.answers
    return answersAll.mapNotNull { it.ip }
  }

  fun retrieveAllIPsFromDnsRecordsAsStrings(url: String): List<String> = retrieveAllIPsFromDnsRecords(url).normalise()

}