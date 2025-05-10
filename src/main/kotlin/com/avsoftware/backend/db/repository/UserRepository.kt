package com.avsoftware.backend.db.repository

import com.avsoftware.backend.db.model.Note
import com.avsoftware.backend.db.model.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: MongoRepository<User, ObjectId> {
    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?
}