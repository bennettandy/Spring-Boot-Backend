package com.avsoftware.backend.controllers

import com.avsoftware.backend.db.model.Note
import com.avsoftware.backend.db.repository.NoteRepository
import org.springframework.web.bind.annotation.RestController
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Clock
import java.time.Instant

// GET localhost:8080/notes
// POST localhost:8080/notes?ownerId
// DELETE localhost:8080/notes/123

@RestController
@RequestMapping("/notes")
class NoteController(
    private val noteRepository: NoteRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

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
    fun save(@RequestBody body: NoteRequest): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val note: Note = noteRepository.save(
            Note(
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(clock),
                owner = ObjectId(ownerId),
                id = body.id?.let { ObjectId(body.id) } ?: ObjectId.get()
            )
        )

        return note.toNoteResponse()
    }

    @GetMapping
    fun findByOwnerId(): List<NoteResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        // hex string is 24 characters
        return noteRepository.findByOwner(ObjectId(ownerId)).map {
            it.toNoteResponse()
        }
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String) {
        logger.debug("DELETE id: $id")

        // make sure we are the owner of this note
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }

        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        if (note.id.toHexString() == ownerId){
            noteRepository.deleteById(ObjectId(id))
            logger.debug("DELETED")
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