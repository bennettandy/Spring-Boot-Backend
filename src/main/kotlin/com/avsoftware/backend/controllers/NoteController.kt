package com.avsoftware.backend.controllers

import com.avsoftware.backend.db.model.Note
import com.avsoftware.backend.db.repository.NoteRepository
import org.springframework.web.bind.annotation.RestController
import org.bson.types.ObjectId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Clock
import java.time.Instant

@RestController
@RequestMapping("/notes")
class NoteController(
    private val noteRepository: NoteRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    data class NoteRequest(
        val id: String?,
        val title: String,
        val content: String,
        val color: Long,
        val ownerId: String // FIXME: Implement Authentication to identify user
    )

    data class NoteResponse(
        val id: String?,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant,
    )

    @PostMapping
    fun save( @RequestBody body: NoteRequest): NoteResponse {
        val note: Note = noteRepository.save(
            Note(
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(clock),
                owner = ObjectId(body.ownerId),
                id = body.id?.let { ObjectId(body.id) } ?: ObjectId.get()
            )
        )

        return note.toNoteResponse()
    }

    @GetMapping
    fun findByOwnerId(
        @RequestParam(required = true) ownerId: String
    ): List<NoteResponse> {
        // hex string is 24 characters
        return if(ownerId.isNotEmpty() && ownerId.length == 24) {
            noteRepository.findByOwner(ObjectId(ownerId)).map {
                it.toNoteResponse()
            }
        }
        else {
            emptyList()
        }
    }

    private fun Note.toNoteResponse() = NoteResponse(
        id = id.toHexString(),
        title = title,
        content = content,
        color = color,
        createdAt = createdAt
    )
}

/*
    val title: String,
    val content: String,
    val color: Long,
    val createdAt: Instant,
    val owner: ObjectId,
 */