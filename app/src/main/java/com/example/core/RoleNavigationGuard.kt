package com.example.core

import com.example.data.model.UserProfile
import com.example.data.model.UserRole

enum class AppRoute(
    val routeId: String,
    val allowedRoles: Set<UserRole>,
    val isRoleDashboard: Boolean = false
) {
    ADMIN_DASHBOARD(
        routeId = "admin_dashboard",
        allowedRoles = setOf(UserRole.ADMIN),
        isRoleDashboard = true
    ),
    TEACHER_DASHBOARD(
        routeId = "teacher_dashboard",
        allowedRoles = setOf(UserRole.TEACHER),
        isRoleDashboard = true
    ),
    STUDENT_DASHBOARD(
        routeId = "student_dashboard",
        allowedRoles = setOf(UserRole.STUDENT),
        isRoleDashboard = true
    ),
    PARENT_DASHBOARD(
        routeId = "parent_dashboard",
        allowedRoles = setOf(UserRole.PARENT),
        isRoleDashboard = true
    ),
    ACADEMICS(
        routeId = "academics",
        allowedRoles = UserRole.entries.toSet()
    ),
    AI_TUTOR(
        routeId = "ai_tutor",
        allowedRoles = UserRole.entries.toSet()
    ),
    COMMS(
        routeId = "comms",
        allowedRoles = UserRole.entries.toSet()
    ),
    ADMIN_MANAGEMENT(
        routeId = "admin",
        allowedRoles = setOf(UserRole.ADMIN)
    ),
    DEVELOPER(
        routeId = "developer",
        allowedRoles = UserRole.entries.toSet()
    ),
    USER_MANUAL(
        routeId = "user_manual",
        allowedRoles = UserRole.entries.toSet()
    ),
    PERFORMANCE(
        routeId = "performance",
        allowedRoles = UserRole.entries.toSet()
    ),
    NOTIFICATIONS(
        routeId = "notifications",
        allowedRoles = UserRole.entries.toSet()
    );

    companion object {
        fun fromRouteId(routeId: String?): AppRoute? =
            entries.firstOrNull { it.routeId == routeId }
    }
}

enum class RedirectReason {
    LOGIN_ROLE_REDIRECT,
    UNAUTHORIZED_ROLE_ACCESS,
    ROLE_UPDATED_IN_FIRESTORE
}

data class GuardResolution(
    val targetRoute: AppRoute,
    val wasRedirected: Boolean,
    val reason: RedirectReason? = null
)

sealed interface RoleGuardState {
    data object CheckingFirestoreRole : RoleGuardState
    data class NeedsOnboarding(val suggestedRole: UserRole) : RoleGuardState
    data class Authorized(
        val profile: UserProfile,
        val targetDashboard: AppRoute
    ) : RoleGuardState
    data class Error(val message: String) : RoleGuardState
}

/**
 * Firestore-backed role navigation guard that resolves a signed-in user's landing dashboard
 * and enforces per-route role permissions across Admin, Teacher, Student, and Parent accounts.
 */
object RoleNavigationGuard {

    fun homeRouteFor(role: UserRole): AppRoute = when (role) {
        UserRole.ADMIN -> AppRoute.ADMIN_DASHBOARD
        UserRole.TEACHER -> AppRoute.TEACHER_DASHBOARD
        UserRole.STUDENT -> AppRoute.STUDENT_DASHBOARD
        UserRole.PARENT -> AppRoute.PARENT_DASHBOARD
    }

    fun isRouteAllowed(routeId: String?, role: UserRole): Boolean {
        val route = AppRoute.fromRouteId(routeId) ?: return false
        return role in route.allowedRoles
    }

    fun resolveRoute(
        requestedRouteId: String?,
        role: UserRole,
        previousRole: UserRole? = null
    ): GuardResolution {
        val home = homeRouteFor(role)

        if (previousRole != null && previousRole != role) {
            return GuardResolution(
                targetRoute = home,
                wasRedirected = true,
                reason = RedirectReason.ROLE_UPDATED_IN_FIRESTORE
            )
        }

        if (requestedRouteId.isNullOrBlank() || requestedRouteId == "dashboard") {
            return GuardResolution(
                targetRoute = home,
                wasRedirected = true,
                reason = RedirectReason.LOGIN_ROLE_REDIRECT
            )
        }

        val requested = AppRoute.fromRouteId(requestedRouteId)
            ?: return GuardResolution(
                targetRoute = home,
                wasRedirected = true,
                reason = RedirectReason.UNAUTHORIZED_ROLE_ACCESS
            )

        return if (role in requested.allowedRoles) {
            GuardResolution(
                targetRoute = requested,
                wasRedirected = false,
                reason = null
            )
        } else {
            GuardResolution(
                targetRoute = home,
                wasRedirected = true,
                reason = RedirectReason.UNAUTHORIZED_ROLE_ACCESS
            )
        }
    }
}
