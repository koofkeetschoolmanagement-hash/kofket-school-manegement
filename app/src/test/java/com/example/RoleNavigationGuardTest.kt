package com.example

import com.example.core.AppRoute
import com.example.core.RedirectReason
import com.example.core.RoleNavigationGuard
import com.example.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleNavigationGuardTest {

    @Test
    fun `homeRouteFor maps every role to its dedicated dashboard`() {
        assertEquals(AppRoute.ADMIN_DASHBOARD, RoleNavigationGuard.homeRouteFor(UserRole.ADMIN))
        assertEquals(AppRoute.TEACHER_DASHBOARD, RoleNavigationGuard.homeRouteFor(UserRole.TEACHER))
        assertEquals(AppRoute.STUDENT_DASHBOARD, RoleNavigationGuard.homeRouteFor(UserRole.STUDENT))
        assertEquals(AppRoute.PARENT_DASHBOARD, RoleNavigationGuard.homeRouteFor(UserRole.PARENT))
    }

    @Test
    fun `login redirect routes each role from generic dashboard to role dashboard`() {
        val adminRes = RoleNavigationGuard.resolveRoute("dashboard", UserRole.ADMIN)
        assertEquals(AppRoute.ADMIN_DASHBOARD, adminRes.targetRoute)
        assertTrue(adminRes.wasRedirected)
        assertEquals(RedirectReason.LOGIN_ROLE_REDIRECT, adminRes.reason)

        val teacherRes = RoleNavigationGuard.resolveRoute(null, UserRole.TEACHER)
        assertEquals(AppRoute.TEACHER_DASHBOARD, teacherRes.targetRoute)
        assertTrue(teacherRes.wasRedirected)
        assertEquals(RedirectReason.LOGIN_ROLE_REDIRECT, teacherRes.reason)

        val studentRes = RoleNavigationGuard.resolveRoute("dashboard", UserRole.STUDENT)
        assertEquals(AppRoute.STUDENT_DASHBOARD, studentRes.targetRoute)
        assertTrue(studentRes.wasRedirected)
        assertEquals(RedirectReason.LOGIN_ROLE_REDIRECT, studentRes.reason)
    }

    @Test
    fun `student and teacher are blocked from admin routes and redirected`() {
        val studentToAdmin = RoleNavigationGuard.resolveRoute(AppRoute.ADMIN_MANAGEMENT.routeId, UserRole.STUDENT)
        assertEquals(AppRoute.STUDENT_DASHBOARD, studentToAdmin.targetRoute)
        assertTrue(studentToAdmin.wasRedirected)
        assertEquals(RedirectReason.UNAUTHORIZED_ROLE_ACCESS, studentToToReason(studentToAdmin))

        val teacherToAdminDash = RoleNavigationGuard.resolveRoute(AppRoute.ADMIN_DASHBOARD.routeId, UserRole.TEACHER)
        assertEquals(AppRoute.TEACHER_DASHBOARD, teacherToAdminDash.targetRoute)
        assertTrue(teacherToAdminDash.wasRedirected)
        assertEquals(RedirectReason.UNAUTHORIZED_ROLE_ACCESS, teacherToAdminDash.reason)

        val studentToTeacherDash = RoleNavigationGuard.resolveRoute(AppRoute.TEACHER_DASHBOARD.routeId, UserRole.STUDENT)
        assertEquals(AppRoute.STUDENT_DASHBOARD, studentToTeacherDash.targetRoute)
        assertTrue(studentToTeacherDash.wasRedirected)
        assertEquals(RedirectReason.UNAUTHORIZED_ROLE_ACCESS, studentToTeacherDash.reason)
    }

    @Test
    fun `admin is allowed to access admin dashboard and admin management`() {
        val adminDash = RoleNavigationGuard.resolveRoute(AppRoute.ADMIN_DASHBOARD.routeId, UserRole.ADMIN)
        assertEquals(AppRoute.ADMIN_DASHBOARD, adminDash.targetRoute)
        assertFalse(adminDash.wasRedirected)
        assertNull(adminDash.reason)

        val adminMgmt = RoleNavigationGuard.resolveRoute(AppRoute.ADMIN_MANAGEMENT.routeId, UserRole.ADMIN)
        assertEquals(AppRoute.ADMIN_MANAGEMENT, adminMgmt.targetRoute)
        assertFalse(adminMgmt.wasRedirected)
        assertNull(adminMgmt.reason)
    }

    @Test
    fun `live role update in Firestore redirects user immediately to new dashboard`() {
        val promoted = RoleNavigationGuard.resolveRoute(
            requestedRouteId = AppRoute.STUDENT_DASHBOARD.routeId,
            role = UserRole.TEACHER,
            previousRole = UserRole.STUDENT
        )
        assertEquals(AppRoute.TEACHER_DASHBOARD, promoted.targetRoute)
        assertTrue(promoted.wasRedirected)
        assertEquals(RedirectReason.ROLE_UPDATED_IN_FIRESTORE, promoted.reason)
    }

    private fun studentToToReason(res: com.example.core.GuardResolution): RedirectReason? = res.reason
}
