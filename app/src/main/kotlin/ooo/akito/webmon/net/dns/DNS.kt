package ooo.akito.webmon.net.dns

import ooo.akito.webmon.utils.ExceptionCompanion.msgDnsOnlyNXDOMAIN
import org.minidns.dnsmessage.DnsMessage
import org.minidns.hla.ResolverApi
import org.minidns.hla.ResolverResult
import org.minidns.record.A
import org.minidns.record.AAAA
import org.minidns.record.Data


class DNS {

  class ResolvesToNowhereException(message:String): Exception(message)

  private fun List<ResolverResult<out Data>>.areNXDOMAIN(): Boolean {
    return this.all { it.responseCode.value == DnsMessage.RESPONSE_CODE.NX_DOMAIN.value }
  }

  fun retrieveAllIPsFromDnsRecords(url: String): List<String> {
    /** A Records */
    val resultA: ResolverResult<A> = ResolverApi.INSTANCE.resolve(url, A::class.java)

    /** AAAA Records */
    val resultAAAA: ResolverResult<AAAA> = ResolverApi.INSTANCE.resolve(url, AAAA::class.java)

    /** A Records' Answers */
    val answersA = try {
      (resultA.answers as Set<*>).filterIsInstance<A>()
    } catch (e: Exception) {
      emptyList()
    }

    /** AAAA Records' Answers */
    val answersAAAA = try {
      (resultAAAA.answers as Set<*>).filterIsInstance<AAAA>()
    } catch (e: Exception) {
      emptyList()
    }

    val resultData: List<ResolverResult<out Data>> = listOf(resultA, resultAAAA)
    if (resultData.areNXDOMAIN()) {
      throw ResolvesToNowhereException(msgDnsOnlyNXDOMAIN)
    }

    return answersA.mapNotNull { it.inetAddress.hostAddress } + answersAAAA.mapNotNull { aaaa -> aaaa.inetAddress.hostAddress?.let { "[${it}]" } }
  }

}