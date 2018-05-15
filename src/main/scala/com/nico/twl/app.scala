/**
  * Created by nperez on 8/31/16.
  */


package com.nico.twl

import com.twilio.sdk.TwilioRestClient
import com.twitter.finagle.Http
import com.twitter.util.{FuturePool, Await}
import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import org.apache.http.NameValuePair
import org.apache.http.message.BasicNameValuePair

import scala.collection.JavaConverters._


case class Message(to: String, body: String)

case class MessageList(items: List[Message])

object app {

  def main(args: Array[String]) {

    val port = Option(System.getProperty("http.port")) getOrElse "9090"

    val accountid = System.getenv("ACCOUNT_ID")

    val authToken = System.getenv("AUTH_TOKEN")

    val sendOne: Endpoint[String] = post("sms" :: "one" :: body.as[Message]) {
      (msg: Message) =>
        val p: List[NameValuePair] = List(
          new BasicNameValuePair("From", "+13053631921"),
          new BasicNameValuePair("To", msg.to),
          new BasicNameValuePair("Body", msg.body)
        )

        val client = new TwilioRestClient(accountid, authToken)
        val messageFactory = client.getAccount().getMessageFactory
        val message = messageFactory.create(p.asJava)

        println(message.getSid)
        println(message.getStatus)

        Ok("OK")
    }

    val sendMany: Endpoint[String] = post("sms" :: "many" :: body.as[MessageList]
      :: headerExists("token").should("be valid") { _ == "lolotoken"}) {
      (messages: MessageList, t: String) =>
        println(t)
        FuturePool.unboundedPool {
          messages.items.foreach { msg =>

            val p: List[NameValuePair] = List(
              new BasicNameValuePair("From", "+13053631921"),
              new BasicNameValuePair("To", msg.to),
              new BasicNameValuePair("Body", msg.body)
            )
            val client = new TwilioRestClient(accountid, authToken)
            val messageFactory = client.getAccount().getMessageFactory
            val message = messageFactory.create(p.asJava)

            println(message.getSid)
            println(message.getStatus)
          }

          Ok("OK")
        }
    }

    val api = sendMany :+: sendOne

    Await.ready(Http.server.serve(s":$port", api.toService))
  }
}
