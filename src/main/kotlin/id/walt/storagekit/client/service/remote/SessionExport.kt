package id.walt.storagekit.client.service.remote

import id.walt.storagekit.client.clientmodels.SessionManager

data class SessionExport(val session: id.walt.storagekit.client.clientmodels.SessionManager.Session, val keyString: String)
