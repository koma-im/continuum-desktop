package link.continuum.desktop.events

import koma.matrix.room.InvitedRoom
import koma.matrix.room.naming.RoomId
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.internal.MapLikeSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.map
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InvitationDeserialization {
    @UnstableDefault
    @Test
    fun test1() {
        val j =Json(JsonConfiguration.Default.copy(strictMode = false))
        val serializer = (RoomId.serializer() to InvitedRoom.serializer()).map
        val data =j.parse(serializer, invite)
        val (room, invite) = data!!.toList().first()
        val invitation = InviteData(invite, room)
        assertEquals("#welc2:example.com", invitation.roomDisplayName)
        assertEquals(null, invitation.roomAvatar)
        assertEquals(null, invitation.inviterAvatar)
        assertEquals("newu1", invitation.inviterName)
    }
}

val invite = """{
      "!yfThBgaOntzNidoJHt:example.com": {
        "invite_state": {
          "events": [
            {
              "content": {
                "membership": "join",
                "avatar_url": null,
                "displayname": "newu1"
              },
              "type": "m.room.member",
              "sender": "@newu1:example.com",
              "state_key": "@newu1:example.com"
            },
            {
              "content": {
                "join_rule": "public"
              },
              "type": "m.room.join_rules",
              "sender": "@newu1:example.com",
              "state_key": ""
            },
            {
              "content": {
                "alias": "#welc2:example.com"
              },
              "type": "m.room.canonical_alias",
              "sender": "@newu1:example.com",
              "state_key": ""
            },
            {
              "origin_server_ts": 1557401186828,
              "sender": "@newu1:example.com",
              "event_id": "${'$'}1557401186141WAVPX:example.com",
              "unsigned": {
                "age": 4210641
              },
              "state_key": "@grethds:example.com",
              "content": {
                "membership": "invite",
                "avatar_url": null,
                "displayname": "grethds"
              },
              "membership": "invite",
              "type": "m.room.member"
            }
          ]
        }
      }
}""".trimIndent()