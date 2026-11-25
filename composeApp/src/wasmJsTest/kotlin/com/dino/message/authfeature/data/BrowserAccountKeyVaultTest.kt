package com.dino.message.authfeature.data

import com.dino.message.authfeature.domain.model.AccountKeyRecord
import com.dino.message.corefeature.data.remote.util.CommonUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BrowserAccountKeyVaultTest {
    @Test
    fun unlocksAfterReopenAndIsolatesAccounts() = runTest {
        val database = "vault-test-${CommonUuid.randomUuidString()}"
        val passphrase = "disposable vault phrase"
        val record = AccountKeyRecord("test private material", "test public material", "registration")
        val vault = BrowserAccountKeyVault(database)
        vault.write("alice", passphrase, record)

        val reopened = BrowserAccountKeyVault(database)
        assertEquals(record, reopened.read("alice", passphrase))
        assertNull(reopened.read("bob", passphrase))
        assertFailsWith<IllegalStateException> { reopened.read("alice", "incorrect vault phrase") }

        val saved = suspendCancellableCoroutine<String> { continuation ->
            inspectAndTamper(database, { continuation.resume(it) }, {
                continuation.resumeWithException(IllegalStateException("Could not inspect test record"))
            })
        }
        assertFalse(saved.contains(record.privateKey))
        assertFalse(saved.contains(passphrase))
        assertFailsWith<IllegalStateException> { reopened.read("alice", passphrase) }

        reopened.remove("alice")
        assertFalse(vault.exists("alice"))
    }
}


@JsFun("""
(name, resolve, reject) => {
    const open = indexedDB.open(name, 1);
    open.onerror = reject;
    open.onsuccess = () => {
        const db = open.result;
        const tx = db.transaction("accounts", "readwrite");
        const store = tx.objectStore("accounts");
        const request = store.get("alice");
        let saved;
        request.onsuccess = () => {
            saved = JSON.stringify(request.result);
            const bytes = new Uint8Array(request.result.ciphertext);
            bytes[0] ^= 1;
            store.put(request.result, "alice");
        };
        tx.oncomplete = () => { db.close(); resolve(saved); };
        tx.onabort = () => { db.close(); reject(); };
    };
}
""")
private external fun inspectAndTamper(name: String, resolve: (String) -> Unit, reject: () -> Unit)
