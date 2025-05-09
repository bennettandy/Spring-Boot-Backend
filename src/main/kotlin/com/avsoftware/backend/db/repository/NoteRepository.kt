package com.avsoftware.backend.db.repository

import com.avsoftware.backend.db.model.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface NoteRepository: MongoRepository<Note, ObjectId> {
    fun findByOwner(owner: ObjectId): List<Note>
}