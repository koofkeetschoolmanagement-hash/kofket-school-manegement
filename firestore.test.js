const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_fayera";
const TEACHER_UID = "teacher_tolasa";
const PARENT_UID = "parent_abebe";
const STUDENT_A_UID = "student_chaltu";
const STUDENT_B_UID = "student_bikila";
const SCHOOL_ID = "koofkeet_main";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedRoles() {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("school_meta").doc("initialized").set({
      schoolId: SCHOOL_ID,
      adminUid: ADMIN_UID,
      adminEmail: "koofkeetschoolmanagement@gmail.com",
      schoolName: "Mana Barumsaa Koofkeet",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: "koofkeetschoolmanagement@gmail.com",
      createdAt: past,
    });
    await db.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      displayName: "Admin Fayyeeraa",
      email: "koofkeetschoolmanagement@gmail.com",
      role: "ADMIN",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(TEACHER_UID).set({
      userId: TEACHER_UID,
      displayName: "Barsiisaa Tolasaa",
      email: "teacher@koofkeet.org",
      role: "TEACHER",
      subject: "Herrega",
      gradeLevel: "Kutaa 10",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(PARENT_UID).set({
      userId: PARENT_UID,
      displayName: "Maatii Ababaa",
      email: "parent@koofkeet.org",
      role: "PARENT",
      linkedStudentId: STUDENT_A_UID,
      linkedStudentName: "Caaltuu",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      displayName: "Caaltuu",
      email: "chaltu@koofkeet.org",
      role: "STUDENT",
      gradeLevel: "Kutaa 10",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(STUDENT_B_UID).set({
      userId: STUDENT_B_UID,
      displayName: "Biqilaa",
      email: "bikila@koofkeet.org",
      role: "STUDENT",
      gradeLevel: "Kutaa 10",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedRoles();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("announcements").where("schoolId", "==", SCHOOL_ID).get());
  await assertFails(unauthDb.collection("exam_results").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(unauthDb.collection("users").doc("hacker").set({
    userId: "hacker",
    displayName: "Hacker",
    email: "h@x.com",
    role: "ADMIN",
    adminId: ADMIN_UID,
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  }));
});

test("Students cannot self-escalate role to ADMIN or TEACHER on creation or update", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 2000);
  const attackerDb = testEnv.authenticatedContext("new_user_1", {
    email: "new_student@gmail.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    attackerDb.collection("users").doc("new_user_1").set({
      userId: "new_user_1",
      displayName: "Fake Admin",
      email: "new_student@gmail.com",
      role: "ADMIN",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    attackerDb.collection("users").doc("new_user_1").set({
      userId: "new_user_1",
      displayName: "Barataa Haaraa",
      email: "new_student@gmail.com",
      role: "STUDENT",
      gradeLevel: "Kutaa 9",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cannot escalate own role via update
  await assertFails(
    attackerDb.collection("users").doc("new_user_1").update({
      role: "TEACHER",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Student A cannot read Student B exam results; Student A & Parent can read Student A results", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();

  await assertSucceeds(
    teacherDb.collection("exam_results").doc("res_1").set({
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      parentId: PARENT_UID,
      teacherId: TEACHER_UID,
      teacherName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      subject: "Herrega",
      gradeLevel: "Kutaa 10",
      examName: "Qormaata 1ffaa",
      monthLabel: "Sadaasa 2026",
      score: 88.5,
      maxScore: 100.0,
      remarks: "Gaariidha",
      createdAt: past,
      updatedAt: past,
    })
  );

  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();

  // Student A can read own results via scoped query
  await assertSucceeds(studentADb.collection("exam_results").where("studentId", "==", STUDENT_A_UID).get());
  // Parent can read child's results via scoped query
  await assertSucceeds(parentDb.collection("exam_results").where("parentId", "==", PARENT_UID).get());
  // Student B is strictly denied from reading Student A's results or unfiltered list
  await assertFails(studentBDb.collection("exam_results").doc("res_1").get());
  await assertFails(studentBDb.collection("exam_results").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(studentBDb.collection("exam_results").get());
});

test("Parent-Teacher and Parent-Admin messages are strictly hidden from Students", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();

  await assertSucceeds(
    parentDb.collection("parent_messages").doc("msg_1").set({
      parentId: PARENT_UID,
      parentName: "Maatii Ababaa",
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      recipientRole: "TEACHER",
      recipientId: TEACHER_UID,
      participantUids: [PARENT_UID, TEACHER_UID],
      senderId: PARENT_UID,
      senderName: "Maatii Ababaa",
      senderRole: "PARENT",
      subject: "Haala Barnootaa",
      body: "Barnoonni Caaltuu akkam jira?",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Parent and Teacher can read via participantUids query
  await assertSucceeds(parentDb.collection("parent_messages").where("participantUids", "array-contains", PARENT_UID).get());
  await assertSucceeds(teacherDb.collection("parent_messages").where("participantUids", "array-contains", TEACHER_UID).get());

  // Student A CANNOT read or list or create parent_messages even about themselves!
  await assertFails(studentADb.collection("parent_messages").doc("msg_1").get());
  await assertFails(studentADb.collection("parent_messages").where("participantUids", "array-contains", PARENT_UID).get());
  await assertFails(studentADb.collection("parent_messages").where("parentId", "==", PARENT_UID).get());
  await assertFails(studentADb.collection("parent_messages").get());
  const ownQuerySnap = await assertSucceeds(
    studentADb.collection("parent_messages").where("participantUids", "array-contains", STUDENT_A_UID).get()
  );
  if (ownQuerySnap.size !== 0) {
    throw new Error("Expected 0 parent messages visible to student");
  }
  await assertFails(
    studentADb.collection("parent_messages").doc("msg_2").set({
      parentId: PARENT_UID,
      parentName: "Maatii Ababaa",
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      recipientRole: "TEACHER",
      recipientId: TEACHER_UID,
      participantUids: [PARENT_UID, TEACHER_UID, STUDENT_A_UID],
      senderId: STUDENT_A_UID,
      senderName: "Caaltuu",
      senderRole: "PARENT",
      subject: "Sneaky",
      body: "Trying to inject myself",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Early warnings can be created/resolved by Teacher/Admin and are hidden from Students", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "koofkeetschoolmanagement@gmail.com",
    email_verified: true,
  }).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();

  await assertSucceeds(
    teacherDb.collection("early_warnings").doc("warn_1").set({
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      gradeLevel: "Kutaa 10",
      subject: "Herrega",
      previousAverage: 85.0,
      currentScore: 48.0,
      dropAmount: 37.0,
      reason: "Qabxiin 85.0% irraa gara 48.0% gadi bu'eera.",
      teacherId: TEACHER_UID,
      adminId: ADMIN_UID,
      status: "OPEN",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(teacherDb.collection("early_warnings").where("teacherId", "==", TEACHER_UID).get());
  await assertSucceeds(adminDb.collection("early_warnings").where("adminId", "==", ADMIN_UID).get());
  await assertSucceeds(adminDb.collection("early_warnings").doc("warn_1").update({
    status: "RESOLVED",
    updatedAt: new Date(Date.now() - 1000),
  }));

  // Student A cannot read or list early warnings
  await assertFails(studentADb.collection("early_warnings").doc("warn_1").get());
  await assertFails(studentADb.collection("early_warnings").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(studentADb.collection("early_warnings").get());
});

test("Assignments, submissions, quizzes, materials, announcements, and shadow-update/timestamp guards", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const future = new Date(Date.now() + 60000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();

  // Teacher creates assignment
  await assertSucceeds(
    teacherDb.collection("assignments").doc("hw_1").set({
      title: "Hojii Manaa Aljebraa",
      description: "Gaaffilee 1-10 hojjedhaa",
      subject: "Herrega",
      gradeLevel: "Kutaa 10",
      dueDateLabel: "2026-11-30",
      teacherId: TEACHER_UID,
      teacherName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      schoolId: SCHOOL_ID,
      status: "OPEN",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student A submits assignment (no @DocumentId `id` field, valid past timestamp)
  await assertSucceeds(
    studentADb.collection("assignment_submissions").doc("sub_1").set({
      assignmentId: "hw_1",
      assignmentTitle: "Hojii Manaa Aljebraa",
      subject: "Herrega",
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      parentId: PARENT_UID,
      teacherId: TEACHER_UID,
      adminId: ADMIN_UID,
      answerText: "x = 5, y = 12",
      score: 0.0,
      status: "SUBMITTED",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student B cannot read Student A submission
  await assertFails(studentBDb.collection("assignment_submissions").doc("sub_1").get());

  // Teacher grades submission with partial update
  await assertSucceeds(
    teacherDb.collection("assignment_submissions").doc("sub_1").update({
      score: 95.0,
      feedback: "Baay'ee gaarii!",
      status: "GRADED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update & extra `id` field & future timestamp rejected
  await assertFails(
    teacherDb.collection("announcements").doc("ann_bad").set({
      id: "ann_bad",
      title: "Extra ID",
      body: "Should fail",
      category: "GENERAL",
      authorId: TEACHER_UID,
      authorName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      schoolId: SCHOOL_ID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    teacherDb.collection("announcements").doc("ann_future").set({
      title: "Future Time",
      body: "Should fail",
      category: "GENERAL",
      authorId: TEACHER_UID,
      authorName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      schoolId: SCHOOL_ID,
      createdAt: future,
      updatedAt: future,
    })
  );
});

test("User Role Management: Admin manages Admin/Teacher/Student roles and grants; non-admins blocked", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 4000);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "koofkeetschoolmanagement@gmail.com",
    email_verified: true,
  }).firestore();
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID, {
    email: "teacher@koofkeet.org",
    email_verified: true,
  }).firestore();
  const studentDb = testEnv.authenticatedContext(STUDENT_A_UID, {
    email: "chaltu@koofkeet.org",
    email_verified: true,
  }).firestore();

  // Admin can list users and issue a TEACHER role_grant
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("role_grants").doc("newteacher@koofkeet.org").set({
      email: "newteacher@koofkeet.org",
      role: "TEACHER",
      fullName: "Barsiisaa Haaraa",
      subject: "Fiiziksii",
      gradeLevel: "Kutaa 11",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(adminDb.collection("role_grants").get());

  // Teacher and Student CANNOT list all users or issue/delete role_grants
  await assertFails(teacherDb.collection("users").get());
  await assertFails(studentDb.collection("users").get());
  await assertFails(
    teacherDb.collection("role_grants").doc("fake@koofkeet.org").set({
      email: "fake@koofkeet.org",
      role: "ADMIN",
      fullName: "Fake",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Verified granted user can onboard with TEACHER role, but NOT ADMIN
  const newTeacherDb = testEnv.authenticatedContext("new_teacher_uid", {
    email: "newteacher@koofkeet.org",
    email_verified: true,
  }).firestore();
  await assertFails(
    newTeacherDb.collection("users").doc("new_teacher_uid").set({
      userId: "new_teacher_uid",
      displayName: "Barsiisaa Haaraa",
      email: "newteacher@koofkeet.org",
      role: "ADMIN",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    newTeacherDb.collection("users").doc("new_teacher_uid").set({
      userId: "new_teacher_uid",
      displayName: "Barsiisaa Haaraa",
      email: "newteacher@koofkeet.org",
      role: "TEACHER",
      subject: "Fiiziksii",
      gradeLevel: "Kutaa 11",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin can promote Student B to ADMIN (users + admins) and demote back to STUDENT
  await assertSucceeds(
    adminDb.collection("users").doc(STUDENT_B_UID).update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("admins").doc(STUDENT_B_UID).set({
      userId: STUDENT_B_UID,
      email: "bikila@koofkeet.org",
      createdAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(STUDENT_B_UID).update({
      role: "STUDENT",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(adminDb.collection("admins").doc(STUDENT_B_UID).delete());

  // Bootstrap admin doc cannot be deleted
  await assertFails(adminDb.collection("admins").doc(ADMIN_UID).delete());

  // Admin can revoke role_grant
  await assertSucceeds(adminDb.collection("role_grants").doc("newteacher@koofkeet.org").delete());
});

test("Profile Setup preferredLanguage supports om/en/am on create & owner update and rejects invalid codes", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 3000);
  const setupUid = "profile_setup_user";
  const setupDb = testEnv.authenticatedContext(setupUid, {
    email: "setup_user@koofkeet.org",
    email_verified: true,
  }).firestore();

  // Reject invalid preferredLanguage on create
  await assertFails(
    setupDb.collection("users").doc(setupUid).set({
      userId: setupUid,
      displayName: "Dabalaa",
      email: "setup_user@koofkeet.org",
      role: "STUDENT",
      gradeLevel: "Kutaa 10",
      preferredLanguage: "fr",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Accept valid preferredLanguage ("am") on initial profile setup
  await assertSucceeds(
    setupDb.collection("users").doc(setupUid).set({
      userId: setupUid,
      displayName: "Dabalaa",
      email: "setup_user@koofkeet.org",
      role: "STUDENT",
      gradeLevel: "Kutaa 10",
      preferredLanguage: "am",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Owner can switch preferredLanguage to "en" and "om"
  await assertSucceeds(
    setupDb.collection("users").doc(setupUid).update({
      preferredLanguage: "en",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    setupDb.collection("users").doc(setupUid).update({
      preferredLanguage: "om",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner cannot update preferredLanguage to an unsupported code
  await assertFails(
    setupDb.collection("users").doc(setupUid).update({
      preferredLanguage: "es",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("AI Tutor interactions under users/{uid}/ai_chats are strictly private to the owning user", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 2000);
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "koofkeetschoolmanagement@gmail.com",
    email_verified: true,
  }).firestore();

  const chatRef = studentADb.collection("users").doc(STUDENT_A_UID).collection("ai_chats").doc("chat_1");
  await assertSucceeds(
    chatRef.set({
      userId: STUDENT_A_UID,
      prompt: "Explain photosynthesis",
      response: "Photosynthesis converts light energy into glucose.",
      subject: "Baayoolojii",
      createdAt: past,
    })
  );

  // Owning student can read and list own private AI chats
  await assertSucceeds(studentADb.collection("users").doc(STUDENT_A_UID).collection("ai_chats").doc("chat_1").get());
  await assertSucceeds(studentADb.collection("users").doc(STUDENT_A_UID).collection("ai_chats").get());

  // Other students, teachers, parents, and even admins CANNOT read, list, write, or delete Student A's AI chats
  for (const otherDb of [studentBDb, teacherDb, parentDb, adminDb]) {
    await assertFails(otherDb.collection("users").doc(STUDENT_A_UID).collection("ai_chats").doc("chat_1").get());
    await assertFails(otherDb.collection("users").doc(STUDENT_A_UID).collection("ai_chats").get());
    await assertFails(otherDb.collection("users").doc(STUDENT_A_UID).collection("ai_chats").doc("chat_1").delete());
    await assertFails(
      otherDb.collection("users").doc(STUDENT_A_UID).collection("ai_chats").doc("chat_2").set({
        userId: STUDENT_A_UID,
        prompt: "Injected",
        response: "Nope",
        subject: "Herrega",
        createdAt: past,
      })
    );
  }

  // Owning student can delete their own private chat
  await assertSucceeds(chatRef.delete());
});

test("Daily attendance tracking: teachers mark Present/Absent/Late, update same-day history, and students/parents read scoped history", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 4000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "koofkeetschoolmanagement@gmail.com",
    email_verified: true,
  }).firestore();

  // Admin registers Student A as ACTIVE
  await assertSucceeds(
    adminDb.collection("student_registrations").doc("reg_chaltu").set({
      studentUid: STUDENT_A_UID,
      studentName: "Caaltuu",
      studentCode: "KFK-101",
      gradeLevel: "Kutaa 10",
      section: "A",
      parentUid: PARENT_UID,
      parentName: "Maatii Ababaa",
      adminId: ADMIN_UID,
      teacherUids: [ADMIN_UID],
      status: "ACTIVE",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Teacher can list active student registrations & STUDENT user profiles for daily roll call
  await assertSucceeds(teacherDb.collection("student_registrations").where("status", "==", "ACTIVE").get());
  await assertSucceeds(teacherDb.collection("users").where("role", "==", "STUDENT").get());
  // Teacher cannot list non-student profiles (e.g. ADMIN)
  await assertFails(teacherDb.collection("users").where("role", "==", "ADMIN").get());

  const attDocId = `2026-11-25_${STUDENT_A_UID}_${TEACHER_UID}`;
  const attRef = teacherDb.collection("attendance").doc(attDocId);

  // Teacher marks Student A ABSENT on 2026-11-25
  await assertSucceeds(
    attRef.set({
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      parentId: PARENT_UID,
      teacherId: TEACHER_UID,
      adminId: ADMIN_UID,
      gradeLevel: "Kutaa 10",
      dateLabel: "2026-11-25",
      status: "ABSENT",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Teacher updates same-day record to PRESENT with a note (preserving createdAt & studentId)
  await assertSucceeds(
    attRef.update({
      status: "PRESENT",
      note: "Arrived after assembly",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt or studentId mutation
  await assertFails(
    attRef.update({
      studentId: STUDENT_B_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    attRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Teacher, Student A, and Parent can query scoped attendance history
  await assertSucceeds(teacherDb.collection("attendance").where("teacherId", "==", TEACHER_UID).get());
  await assertSucceeds(studentADb.collection("attendance").where("studentId", "==", STUDENT_A_UID).get());
  await assertSucceeds(parentDb.collection("attendance").where("parentId", "==", PARENT_UID).get());

  // Student B & Parent cannot read Student A's record as Student B or write/delete attendance
  await assertFails(studentBDb.collection("attendance").doc(attDocId).get());
  await assertFails(studentBDb.collection("attendance").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(studentADb.collection("attendance").doc(attDocId).delete());
  await assertFails(
    parentDb.collection("attendance").doc("2026-11-25_fake").set({
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      teacherId: PARENT_UID,
      adminId: ADMIN_UID,
      gradeLevel: "Kutaa 10",
      dateLabel: "2026-11-25",
      status: "PRESENT",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Owning teacher can delete mistaken attendance record
  await assertSucceeds(attRef.delete());
});

test("Teacher records, updates, lists, and deletes Grade 1-6 and Grade 7-8 subject grades in exam_results", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 4000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();

  const gradeRef = teacherDb.collection("exam_results").doc("grade_g7_ict_1");
  await assertSucceeds(
    gradeRef.set({
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      parentId: PARENT_UID,
      teacherId: TEACHER_UID,
      teacherName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      subject: "ICT",
      gradeLevel: "Grade 7",
      examName: "Term 1 Evaluation",
      monthLabel: "2026-11",
      score: 91.5,
      maxScore: 100.0,
      remarks: "Very good lab work",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Teacher can record Grade 1-6 subject ("Gadaafi Safuu")
  await assertSucceeds(
    teacherDb.collection("exam_results").doc("grade_g4_gadaa_1").set({
      studentId: STUDENT_B_UID,
      studentName: "Biqilaa",
      teacherId: TEACHER_UID,
      teacherName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      subject: "Gadaafi Safuu",
      gradeLevel: "Grade 4",
      examName: "Class Assessment",
      monthLabel: "2026-11",
      score: 86.0,
      maxScore: 100.0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Teacher can update score while preserving createdAt and studentId
  await assertSucceeds(
    gradeRef.update({
      score: 94.0,
      remarks: "Updated practical grade",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Teacher & Student A can read scoped grades; Student B cannot read Student A's grade or delete it
  await assertSucceeds(teacherDb.collection("exam_results").where("teacherId", "==", TEACHER_UID).get());
  await assertSucceeds(studentADb.collection("exam_results").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(studentBDb.collection("exam_results").doc("grade_g7_ict_1").get());
  await assertFails(studentADb.collection("exam_results").doc("grade_g7_ict_1").delete());

  // Owning teacher can delete their own erroneous grade entry
  await assertSucceeds(gradeRef.delete());
});

test("Calendar sync: Teachers and Admins manage school_events while Students and Parents read school calendar", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 4000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "koofkeetschoolmanagement@gmail.com",
    email_verified: true,
  }).firestore();

  const evRef = teacherDb.collection("school_events").doc("assign_sync_1");
  await assertSucceeds(
    evRef.set({
      title: "Herrega Midterm Review",
      description: "Grade 7 Mathematics midterm prep",
      eventDateLabel: "2026-11-28",
      startTimeLabel: "09:00",
      endTimeLabel: "10:00",
      category: "EXAM",
      gradeLevel: "Grade 7",
      subject: "Herrega",
      sourceType: "MANUAL",
      createdByUid: TEACHER_UID,
      createdByName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      schoolId: "koofkeet_main",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Students and Parents can list & read school calendar events
  await assertSucceeds(studentADb.collection("school_events").where("schoolId", "==", "koofkeet_main").get());
  await assertSucceeds(parentDb.collection("school_events").doc("assign_sync_1").get());

  // Students and Parents cannot create, update, or delete calendar events
  await assertFails(
    studentADb.collection("school_events").doc("student_fake").set({
      title: "No School",
      eventDateLabel: "2026-11-29",
      startTimeLabel: "08:00",
      endTimeLabel: "09:00",
      category: "HOLIDAY",
      gradeLevel: "Hunda",
      sourceType: "MANUAL",
      createdByUid: STUDENT_A_UID,
      createdByName: "Caaltuu",
      adminId: ADMIN_UID,
      schoolId: "koofkeet_main",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(studentADb.collection("school_events").doc("assign_sync_1").delete());

  // Owning teacher can update & delete; admin can update
  await assertSucceeds(
    evRef.update({
      title: "Herrega Midterm & ICT Quiz",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("school_events").doc("assign_sync_1").update({
      category: "GENERAL",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(evRef.delete());
});






