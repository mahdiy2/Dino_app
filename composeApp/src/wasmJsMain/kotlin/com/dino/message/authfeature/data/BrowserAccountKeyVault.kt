package com.dino.message.authfeature.data

import com.dino.message.authfeature.domain.model.AccountKeyRecord
import com.dino.message.authfeature.domain.model.AccountKeyVault
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json

internal class BrowserAccountKeyVault(
    private val databaseName: String = "secmessage-key-vault",
) : AccountKeyVault {
    override suspend fun exists(account: String): Boolean =
        perform("exists", account) == "true"

    override suspend fun read(account: String, passphrase: String): AccountKeyRecord? =
        perform("read", account, passphrase).takeIf(String::isNotEmpty)
            ?.let { Json.decodeFromString<AccountKeyRecord>(it) }

    override suspend fun remove(account: String) {
        perform("remove", account)
    }

    override suspend fun write(account: String, passphrase: String, record: AccountKeyRecord) {
        require(passphrase.length >= 12) { "Use a passphrase of at least 12 characters." }
        perform("write", account, passphrase, Json.encodeToString(record))
    }

    private suspend fun perform(
        operation: String,
        account: String,
        passphrase: String = "",
        value: String = "",
    ): String = suspendCancellableCoroutine { continuation ->
        vaultOperation(databaseName, operation, account, passphrase, value,
            { continuation.resume(it) },
            { continuation.resumeWithException(IllegalStateException("Could not unlock or save the key vault.")) },
        )
    }
}

@JsFun("""
async (name, operation, account, passphrase, payload, resolve, reject) => {
    let db;
    try {
        const encoder = new TextEncoder();
        const aad = encoder.encode(JSON.stringify(["secmessage-key-vault", 1, location.origin, account]));
        const deriveKey = async salt => {
            const secret = await crypto.subtle.importKey("raw", encoder.encode(passphrase), "PBKDF2", false, ["deriveKey"]);
            return crypto.subtle.deriveKey(
                { name: "PBKDF2", salt, iterations: 600000, hash: "SHA-256" },
                secret, { name: "AES-GCM", length: 256 }, false, ["encrypt", "decrypt"]
            );
        };
        db = await new Promise((ok, fail) => {
            const open = indexedDB.open(name, 1);
            open.onupgradeneeded = () => open.result.createObjectStore("accounts");
            open.onsuccess = () => ok(open.result);
            open.onerror = () => fail(open.error);
            open.onblocked = () => fail(new Error("Key vault is blocked"));
        });
        if (operation === "remove") {
            await new Promise((ok, fail) => {
                const tx = db.transaction("accounts", "readwrite");
                tx.objectStore("accounts").delete(account);
                tx.oncomplete = ok;
                tx.onabort = () => fail(tx.error);
            });
            resolve("");
            return;
        }
        if (operation === "write") {
            const salt = crypto.getRandomValues(new Uint8Array(16));
            const iv = crypto.getRandomValues(new Uint8Array(12));
            const ciphertext = await crypto.subtle.encrypt(
                { name: "AES-GCM", iv, additionalData: aad }, await deriveKey(salt), encoder.encode(payload)
            );
            await new Promise((ok, fail) => {
                const tx = db.transaction("accounts", "readwrite");
                tx.objectStore("accounts").put({ version: 1, salt, iv, ciphertext }, account);
                tx.oncomplete = ok;
                tx.onabort = () => fail(tx.error);
            });
            resolve("");
            return;
        }
        const record = await new Promise((ok, fail) => {
            const read = db.transaction("accounts", "readonly").objectStore("accounts").get(account);
            read.onsuccess = () => ok(read.result);
            read.onerror = () => fail(read.error);
        });
        if (operation === "exists") {
            resolve(String(!!record));
        } else if (!record) {
            resolve("");
        } else {
            if (record.version !== 1 || record.salt.length !== 16 || record.iv.length !== 12) {
                throw new Error("Invalid vault record");
            }
            const plaintext = await crypto.subtle.decrypt(
                { name: "AES-GCM", iv: record.iv, additionalData: aad },
                await deriveKey(record.salt), record.ciphertext
            );
            resolve(new TextDecoder().decode(plaintext));
        }
    } catch (_) {
        reject();
    } finally {
        db?.close();
    }
}
""")
private external fun vaultOperation(
    name: String, operation: String, account: String, passphrase: String, payload: String,
    resolve: (String) -> Unit, reject: () -> Unit,
)
