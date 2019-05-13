package link.continuum.desktop.events

import com.squareup.moshi.Types
import koma.matrix.json.MoshiInstance
import koma.matrix.room.InvitedRoom
import koma.matrix.room.naming.RoomId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InvitationDeserialization {
    @Test
    fun test1() {
        val typeA = Types.newParameterizedType(Map::class.java, RoomId::class.java, InvitedRoom::class.java)
        val adapter = MoshiInstance.moshi.adapter<Map<RoomId, InvitedRoom>>(typeA)
        val data = adapter.fromJson(invite)
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