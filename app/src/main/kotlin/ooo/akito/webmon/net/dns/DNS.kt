package ooo.akito.webmon.net.dns

import ooo.akito.webmon.net.Utils.normaliseToString
import org.minidns.hla.ResolverApi
import org.minidns.hla.ResolverResult
import org.minidns.record.A
import org.minidns.record.AAAA
import org.minidns.record.InternetAddressRR
import java.net.InetAddress

class DNS {

  fun retrieveAllIPsFromDnsRecords(url: String): List<ByteArray> {
    /** A Records */
    val resultA: ResolverResult<A> = ResolverApi.INSTANCE.resolve(url, A::class.java)
    /** AAAA Records */
    val resultAAAA: ResolverResult<AAAA> = ResolverApi.INSTANCE.resolve(url, AAAA::class.java)
    /** All Records' Answers */
    val answersA = try {
      (resultA.answers as Set<*>).filterIsInstance<A>()
    } catch (e: Exception) {
      emptyList()
    }
    val answersAAAA = try {
      (resultAAAA.answers as Set<*>).filterIsInstance<AAAA>()
    } catch (e: Exception) {
      emptyList()
    }

    val answersAll: List<InternetAddressRR<out InetAddress>> = answersA + answersAAAA
    return answersAll.mapNotNull { it.ip }
  }

  fun retrieveAllIPsFromDnsRecordsAsStrings(url: String): List<String> = retrieveAllIPsFromDnsRecords(url).normaliseToString()

}