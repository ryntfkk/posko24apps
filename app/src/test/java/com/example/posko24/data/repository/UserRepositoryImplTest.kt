package com.example.posko24.data.repository

import com.example.posko24.data.model.User
import com.example.posko24.data.model.UserAddress
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRepositoryImplTest {

    @Test
    fun `getUserProfile emits user with default address`() = runTest {
        val firestore: FirebaseFirestore = mockk()
        val functions: FirebaseFunctions = mockk(relaxed = true)
        val collectionReference: CollectionReference = mockk()
        val documentReference: DocumentReference = mockk()
        val documentSnapshot: DocumentSnapshot = mockk()

        val address = UserAddress(
            id = "addr-123",
            province = "Jawa Barat",
            city = "Bandung",
            district = "Coblong",
            detail = "Jl. Sangkuriang No. 10"
        )
        val user = User(
            uid = "user-1",
            fullName = "Jane Doe",
            email = "jane@example.com",
            phoneNumber = "08123456789",
            defaultAddressId = address.id,
            defaultAddress = address
        )

        every { firestore.collection("users") } returns collectionReference
        every { collectionReference.document("user-1") } returns documentReference
        every { documentReference.get() } returns Tasks.forResult(documentSnapshot)
        every { documentSnapshot.toObject(User::class.java) } returns user

        val repository = UserRepositoryImpl(firestore, functions)

        val result = repository.getUserProfile("user-1").first()

        assertTrue(result.isSuccess)
        val loadedUser = result.getOrNull()
        assertNotNull(loadedUser)
        assertEquals(address.id, loadedUser.defaultAddressId)
        assertEquals(address, loadedUser.defaultAddress)
    }
}