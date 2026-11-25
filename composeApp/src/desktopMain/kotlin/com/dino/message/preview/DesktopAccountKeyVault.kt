package com.dino.message.preview

import com.dino.message.authfeature.domain.model.AccountKeyRecord
import com.dino.message.authfeature.domain.model.AccountKeyVault
import com.dino.message.corefeature.data.spref.SecureKeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class DesktopAccountKeyVault(private val store: SecureKeyValueStore) : AccountKeyVault {
    override suspend fun exists(account: String) = withContext(Dispatchers.IO) {
        store.getString("account_key:$account") != null
    }

    override suspend fun read(account: String, passphrase: String) = withContext(Dispatchers.IO) {
        store.getString("account_key:$account")?.let { Json.decodeFromString<AccountKeyRecord>(it) }
    }

    override suspend fun remove(account: String) {
        withContext(Dispatchers.IO) { store.remove("account_key:$account").apply() }
    }

    override suspend fun write(account: String, passphrase: String, record: AccountKeyRecord) {
        withContext(Dispatchers.IO) {
            store.putString("account_key:$account", Json.encodeToString(record))
        }
    }
}
