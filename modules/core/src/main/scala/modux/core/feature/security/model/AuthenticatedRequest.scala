package modux.core.feature.security.model

import modux.core.feature.security.context.ModuxWebContext
import org.pac4j.core.profile.UserProfile

final case class AuthenticatedRequest(context: ModuxWebContext, profiles: Seq[UserProfile])
