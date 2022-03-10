package confidentialstorage.client.service.remote

import confidentialstorage.client.clientmodels.SessionManager

data class SessionExport(val session: SessionManager.Session, val keyString: String)
